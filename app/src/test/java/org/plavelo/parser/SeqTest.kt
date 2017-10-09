package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class SeqTest {
    @Test
    @Throws(Exception::class)
    fun seq() {
        val parser = Parser.seq(
                Parser.string("("),
                Parser.regex("[^)]").many().map { xs ->
                    when (xs) {
                        is Value.Single -> xs
                        is Value.Multiple -> Value.Single(xs.value().joinToString(""))
                        is Value.Empty -> xs
                    }
                },
                Parser.string(")")
        )
        val result1 = parser.parse("(string between parens)").reply().value() as List<*>
        Assert.assertEquals(3, result1.size)
        Assert.assertEquals("(", result1[0])
        Assert.assertEquals("string between parens", result1[1])
        Assert.assertEquals(")", result1[2])
    }
}