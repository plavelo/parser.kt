package org.plavelo.parser

sealed class Either<out L, out R> {
    abstract fun left(): L
    abstract fun right(): R
    open fun isLeft(): Boolean = false
    open fun isRight(): Boolean = false

    class Left<out L, out R>(private val l: L) : Either<L, R>() {
        override fun isLeft() = true
        override fun left(): L = l
        override fun right(): Nothing = throw RuntimeException()
    }

    class Right<out L, out R>(private val r: R) : Either<L, R>() {
        override fun isRight() = true
        override fun left(): Nothing = throw RuntimeException()
        override fun right(): R = r
    }
}

sealed class Value {
    abstract fun value(): Any
    abstract fun wrapped(): Collection<Any>
    class None : Value() {
        override fun value() = throw RuntimeException()
        override fun wrapped(): Collection<Any> = emptyList()
    }

    class Single(val value: Any) : Value() {
        override fun value() = value
        override fun wrapped() = listOf(value)
    }

    class Multiple(val value: Collection<Any>) : Value() {
        override fun value() = value
        override fun wrapped() = value
    }
}

abstract class Result {
    abstract fun index(): Int
    abstract fun raw(): Value
    abstract fun value(): Any
    abstract fun farthest(): Int
    abstract fun expected(): Collection<String>
}

class Success(val index: Int, val value: Value) : Result() {
    override fun index(): Int = index
    override fun raw(): Value = value
    override fun value(): Any = when (value) {
        is Value.Single -> value.value()
        is Value.Multiple -> value.value()
        is Value.None -> value
    }

    override fun farthest(): Int = -1
    override fun expected(): Collection<String> = emptyList()
}

class Failure(private val index: Int, private val expected: Collection<String>) : Result() {
    override fun index(): Int = index
    override fun raw(): Value = throw RuntimeException()
    override fun value(): Any = throw RuntimeException()
    override fun farthest(): Int = index
    override fun expected(): Collection<String> = expected
}

fun Either<Failure, Success>.result(): Result = if (this.isRight()) this.right() else this.left()

private fun mergeReplies(result: Either<Failure, Success>, last: Either<Failure, Success>?): Either<Failure, Success> {
    if (last == null) {
        return result
    }
    val resVal = result.result()
    val lasVal = last.result()
    if (resVal.farthest() > lasVal.farthest()) {
        return result
    }
    if (result.isRight()) {
        return Either.Right(Success(resVal.index(), resVal.raw()))
    }
    val expected = if (resVal.farthest() == lasVal.farthest()) {
        resVal.expected().union(lasVal.expected())
    } else {
        lasVal.expected()
    }
    return Either.Left(Failure(resVal.index(), expected))
}

class Parser(private val action: (source: String, index: Int) -> Either<Failure, Success>) {
    fun then(next: Parser): Parser = seq(this, next).map { results -> if (results is Value.Multiple) Value.Single(results.value.toList()[1]) else throw RuntimeException() }

    fun thru(wrapper: (Parser) -> Parser): Parser = wrapper(this)

    fun skip(next: Parser): Parser = seq(this, next).map { results -> if (results is Value.Multiple) Value.Single(results.value.toList()[0]) else throw RuntimeException() }

    fun or(alternative: Parser): Parser = alt(this, alternative)

    fun sepBy(separator: Parser): Parser = sepBy(this, separator)

    fun sepBy1(separator: Parser): Parser = sepBy1(this, separator)

    fun many(): Parser {
        val parent = this
        return Parser(fun(source, index): Either<Failure, Success> {
            var result: Either<Failure, Success>? = null
            val accumulator = mutableListOf<Any>()
            var i = index
            while (true) {
                result = mergeReplies(parent.invoke(source, i), result)
                if (result.isRight()) {
                    accumulator.add(result.result().value())
                    i = result.result().index()
                } else {
                    return mergeReplies(Either.Right(Success(i, Value.Multiple(accumulator))), result)
                }
            }
        })
    }

    fun chain(function: (Value) -> Parser): Parser {
        val parent = this
        return Parser(fun(source, index): Either<Failure, Success> {
            val result = parent.invoke(source, index)
            if (result.isLeft()) {
                return result
            }
            val nextParser = function(result.result().raw())
            return mergeReplies(nextParser.invoke(source, index), result)
        })
    }

    fun parse(source: String): Either<Failure, Success> {
        return skip(EOF).invoke(source, 0)
    }

    fun invoke(source: String, index: Int): Either<Failure, Success> = action(source, index)

    fun result(result: Any): Parser = map { _ -> Value.Single(result) }

    fun map(function: (Value) -> Value): Parser {
        val parent = this
        return Parser(fun(source, index): Either<Failure, Success> {
            val result = parent.invoke(source, index)
            if (result.isLeft()) {
                return result
            }
            val resVal = result.result()
            return mergeReplies(Either.Right(Success(resVal.index(), function(resVal.raw()))), result)
        })
    }

    companion object {
        val EOF by lazy {
            Parser { s, i ->
                if (i < s.length) {
                    Either.Left(Failure(i, listOf("EOF")))
                } else {
                    Either.Right(Success(i, Value.None()))
                }
            }
        }

        val all: Parser = Parser { source, index ->
            Either.Right(Success(source.length, Value.Multiple(listOf(source.slice(index until source.length)))))
        }

        fun regex(pattern: String, option: RegexOption? = null, group: Int = 0): Parser {
            val anchoredPattern = "^(?:$pattern)"
            val anchored = if (option == null) Regex(anchoredPattern) else Regex(anchoredPattern, option)
            return Parser(fun(source: String, index: Int): Either<Failure, Success> {
                val match = anchored.find(source.slice(index until source.length))
                if (match != null && group in 0..match.groupValues.size) {
                    val fullMatch = match.value
                    val groupMatch = match.groupValues[group]
                    return Either.Right(Success(index + fullMatch.length, Value.Single(groupMatch)))
                }
                return Either.Left(Failure(index, listOf(pattern)))
            })
        }

        fun string(string: String): Parser {
            return Parser(fun(source: String, index: Int): Either<Failure, Success> {
                val to = index + string.length
                val head = source.slice(index until to)
                return if (head == string) {
                    Either.Right(Success(to, Value.Single(head)))
                } else {
                    Either.Left(Failure(index, listOf(string)))
                }
            })
        }

        fun seq(vararg parsers: Parser): Parser {
            return Parser(fun(source, index): Either<Failure, Success> {
                var result: Either<Failure, Success>? = null
                var i: Int = index
                val accumulator = mutableListOf<Any>()
                for (parser in parsers) {
                    result = mergeReplies(parser.invoke(source, i), result)
                    if (result.isLeft()) {
                        return result
                    }
                    accumulator.add(result.result().value())
                    i = result.result().index()
                }
                return mergeReplies(Either.Right(Success(i, Value.Multiple(accumulator))), result)
            })
        }

        fun alt(vararg parsers: Parser): Parser {
            if (parsers.isEmpty()) {
                return fail(listOf("zero alternates"))
            }
            return Parser(fun(source, index): Either<Failure, Success> {
                var result: Either<Failure, Success>? = null
                for (parser in parsers) {
                    result = mergeReplies(parser.invoke(source, index), result)
                    if (result.isRight()) {
                        return result
                    }
                }
                return mergeReplies(Either.Left(Failure(index, emptyList())), result)
            })
        }

        fun sepBy(parser: Parser, separator: Parser): Parser = sepBy1(parser, separator).or(succeed(Value.None()))

        fun sepBy1(parser: Parser, separator: Parser): Parser {
            val pairs = separator.then(parser).many()
            return parser.chain(fun(r): Parser {
                return pairs.map(fun(rs): Value {
                    return Value.Multiple(r.wrapped() + rs.wrapped())
                })
            })
        }

        fun fail(expected: Collection<String>): Parser = Parser { _, index -> Either.Left(Failure(index, expected)) }

        fun succeed(value: Value): Parser = Parser { _, index -> Either.Right(Success(index, value)) }
    }
}
