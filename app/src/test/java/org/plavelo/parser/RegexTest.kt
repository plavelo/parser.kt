package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class RegexTest {
    @Test
    @Throws(Exception::class)
    fun regex() {
        val parser1 = Parser.regex("[0-9]")
        Assert.assertEquals("1", parser1.parse("1").reply().value())
        Assert.assertTrue(parser1.parse("x0").isLeft())
        Assert.assertEquals("1", Parser.regex("[0-9]")("1", 0).reply().value())
        Assert.assertTrue("1", Parser.regex("[0-9]")("x0", 0).isLeft())

        val whitespace = Parser.regex("\\s*")
        val parser2 = whitespace.then(Parser.regex("\"(.*)\"", group = 1)).skip(whitespace)
        val result = parser2.parse("\"abc\"")
        Assert.assertEquals("abc", result.reply().value())
    }
}
