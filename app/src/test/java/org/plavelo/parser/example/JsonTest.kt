package org.plavelo.parser.example

import junit.framework.Assert
import org.junit.Test
import org.plavelo.parser.Parser
import org.plavelo.parser.Value

class JsonTest {
    @Test
    @Throws(Exception::class)
    fun json() {
        // json subset parser test
        class Language {
            val whitespace = Parser.regex("\\s*")
            val token: (parser: Parser) -> Parser = { it.skip(whitespace) }
            val word: (string: String) -> Parser = { Parser.string(it).thru(token) }

            val elements: Parser = Parser.lazy(lazy {
                Parser.alt(
                        obj,
                        array,
                        stringValue,
                        numberValue,
                        trueValue,
                        falseValue
                ).thru { whitespace.then(it) }
            })
            val leftBrace = word("{")
            val rightBrace = word("}")
            val leftBracket = word("[")
            val rightBracket = word("]")
            val comma = word(",")
            val colon = word(":")

            val trueValue = word("true").result(true)
            val falseValue = word("false").result(false)
            val stringValue = token(Parser.regex("\"((?:\\\\.|.)*?)\"", group = 1))
            val numberValue = token(Parser.regex("-?(0|[1-9][0-9]*)").map { Value.Single((it.content() as String).toInt()) })

            val array = leftBracket.then(elements.sepBy(comma)).skip(rightBracket)
            val pair = Parser.seq(stringValue.skip(colon), elements)
            val obj = leftBrace.then(pair.sepBy(comma)).skip(rightBrace).map {
                val result = mutableMapOf<String, Any?>()
                if (it.content() !is Value.Empty) {
                    (it.content() as List<*>).map {
                        it as List<*>
                    }.forEach {
                        result[it[0] as String] = it[1]
                    }
                }
                Value.Single(result)
            }
        }

        val source = """{
            |  "string": "foobar",
            |  "number": -12345,
            |  "list": [
            |    true,
            |    false,
            |    "",
            |    " ",
            |    {
            |      "empty": {}
            |    }
            |  ]
            |}""".trimMargin()

        val result = Language().obj.parse(source).right().content() as Map<*, *>

        Assert.assertEquals("foobar", result["string"])
        Assert.assertEquals(-12345, result["number"])

        val list = result["list"] as List<*>
        Assert.assertEquals(5, list.size)
        Assert.assertEquals(true, list[0])
        Assert.assertEquals(false, list[1])
        Assert.assertEquals("", list[2])
        Assert.assertEquals(" ", list[3])

        val map = list[4] as Map<*, *>
        Assert.assertEquals(1, map.size)

        val innerMap = map["empty"] as Map<*, *>
        Assert.assertEquals(0, innerMap.size)
    }
}
