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
    abstract fun content(): Any
    abstract fun wrapped(): Collection<Any>
    class Empty : Value() {
        override fun content() = emptyList<Any>()
        override fun wrapped(): Collection<Any> = emptyList()
    }

    class Single(private val value: Any) : Value() {
        override fun content() = value
        override fun wrapped() = listOf(value)
    }

    class Multiple(private val value: Collection<Any>) : Value() {
        override fun content() = value
        override fun wrapped() = value
    }
}

interface Reply {
    fun position(): Int
    fun errorPosition(): Int
}

class Success(private val position: Int, private val value: Value) : Reply {
    override fun position(): Int = position
    override fun errorPosition(): Int = -1
    fun value(): Value = value
    fun content(): Any = value.content()
}

class Failure(private val position: Int, private val expected: Collection<String>) : Reply {
    override fun position(): Int = position
    override fun errorPosition(): Int = position
    fun expected(): Collection<String> = expected
}

fun Either<Failure, Success>.reply(): Reply = if (this.isRight()) this.right() else this.left()

private fun mergeReplies(either: Either<Failure, Success>, lastEither: Either<Failure, Success>?): Either<Failure, Success> {
    if (lastEither == null) {
        return either
    }
    if (either.isRight()) {
        return Either.Right(Success(either.right().position(), either.right().value()))
    }
    if (either.reply().errorPosition() == lastEither.reply().errorPosition()) {
        return Either.Left(Failure(either.left().errorPosition(), either.left().expected().union(lastEither.left().expected())))
    }
    if (either.reply().errorPosition() > lastEither.reply().errorPosition()) {
        return either
    }
    return lastEither
}

class Parser(private var action: (source: String, position: Int) -> Either<Failure, Success>) {

    operator fun invoke(source: String, position: Int): Either<Failure, Success> = action(source, position)

    fun parse(source: String): Either<Failure, Success> = skip(eof)(source, 0)

    fun skip(next: Parser): Parser = seq(this, next).map {
        if (it is Value.Multiple) Value.Single(it.content().toList()[0]) else throw RuntimeException()
    }

    fun then(next: Parser): Parser = seq(this, next).map {
        if (it is Value.Multiple) Value.Single(it.content().toList()[1]) else throw RuntimeException()
    }

    fun thru(wrapper: (Parser) -> Parser): Parser = wrapper(this)

    fun or(alternative: Parser): Parser = alt(this, alternative)

    fun sepBy(separator: Parser): Parser = sepBy(this, separator)

    fun many(): Parser {
        return Parser(fun(source, position): Either<Failure, Success> {
            val accumulator = mutableListOf<Any>()
            var pos = position
            var reply: Either<Failure, Success> = this(source, pos)
            while (true) {
                if (reply.isRight()) {
                    accumulator.add(reply.right().content())
                    pos = reply.right().position()
                    reply = mergeReplies(this(source, pos), reply)
                } else {
                    return mergeReplies(Either.Right(Success(pos, Value.Multiple(accumulator))), reply)
                }
            }
        })
    }

    fun chain(function: (Value) -> Parser): Parser {
        return Parser(fun(source, position): Either<Failure, Success> {
            val reply = this(source, position)
            if (reply.isLeft()) {
                return reply
            }
            val nextParser = function(reply.right().value())
            return mergeReplies(nextParser(source, reply.right().position()), reply)
        })
    }

    fun result(result: Any): Parser = map {
        Value.Single(result)
    }

    fun map(function: (Value) -> Value): Parser {
        return Parser { source, position ->
            val reply = this(source, position)
            if (reply.isLeft()) {
                reply
            } else {
                mergeReplies(Either.Right(Success(reply.right().position(), function(reply.right().value()))), reply)
            }
        }
    }

    companion object {
        val eof = Parser { source, position ->
            if (position < source.length) {
                Either.Left(Failure(position, listOf("eof")))
            } else {
                Either.Right(Success(position, Value.Empty()))
            }
        }

        val all: Parser = Parser { source, position ->
            Either.Right(Success(source.length, Value.Multiple(listOf(source.slice(position until source.length)))))
        }

        fun regex(pattern: String, option: RegexOption? = null, group: Int = 0): Parser {
            val anchoredPattern = "^$pattern"
            val regex = if (option == null) Regex(anchoredPattern) else Regex(anchoredPattern, option)
            return Parser { source, position ->
                val match = regex.find(source.slice(position until source.length))
                if (match != null && group in 0..match.groupValues.size) {
                    Either.Right(Success(position + match.value.length, Value.Single(match.groupValues[group])))
                } else {
                    Either.Left(Failure(position, listOf(pattern)))
                }
            }
        }

        fun string(string: String): Parser {
            return Parser(fun(source: String, position: Int): Either<Failure, Success> {
                val to = position + string.length
                if (to > source.length) {
                    return Either.Left(Failure(position, listOf(string)))
                }
                val head = source.slice(position until to)
                return if (head == string) {
                    Either.Right(Success(to, Value.Single(head)))
                } else {
                    Either.Left(Failure(position, listOf(string)))
                }
            })
        }

        fun lazy(lazyParser: Lazy<Parser>): Parser {
            var parser = Parser { _, position ->
                Either.Left(Failure(position, listOf("dummy parser")))
            }
            parser = Parser { source, position ->
                parser.action = lazyParser.value.action
                parser(source, position)
            }
            return parser
        }

        fun seq(vararg parsers: Parser): Parser {
            return Parser(fun(source, position): Either<Failure, Success> {
                var reply: Either<Failure, Success>? = null
                var pos: Int = position
                val accumulator = mutableListOf<Any>()
                for (parser in parsers) {
                    reply = mergeReplies(parser(source, pos), reply)
                    if (reply.isLeft()) {
                        return reply
                    }
                    pos = reply.right().position()
                    accumulator.add(reply.right().content())
                }
                return mergeReplies(Either.Right(Success(pos, Value.Multiple(accumulator))), reply)
            })
        }

        fun alt(vararg parsers: Parser): Parser {
            if (parsers.isEmpty()) {
                return fail(listOf("zero alternates"))
            }
            return Parser(fun(source, position): Either<Failure, Success> {
                var reply: Either<Failure, Success>? = null
                for (parser in parsers) {
                    reply = mergeReplies(parser(source, position), reply)
                    if (reply.isRight()) {
                        return reply
                    }
                }
                return mergeReplies(Either.Left(Failure(position, emptyList())), reply)
            })
        }

        fun sepBy(parser: Parser, separator: Parser): Parser = sepBy1(parser, separator).or(succeed(Value.Empty()))

        fun sepBy1(parser: Parser, separator: Parser): Parser {
            val pairs = separator.then(parser).many()
            return parser.chain(fun(r): Parser {
                return pairs.map(fun(rs): Value {
                    val value = ((r as? Value.Single)?.wrapped() ?: listOf(r.wrapped())) + rs.wrapped()
                    return Value.Multiple(value)
                })
            })
        }

        fun fail(expected: Collection<String>): Parser = Parser { _, position ->
            Either.Left(Failure(position, expected))
        }

        fun succeed(value: Value): Parser = Parser { _, position ->
            Either.Right(Success(position, value))
        }
    }
}
