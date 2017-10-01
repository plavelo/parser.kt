package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class RegexTest {
    @Test
    @Throws(Exception::class)
    fun regex() {
        val parser = Parser.regex("[0-9]")
        Assert.assertEquals("1", parser.parse("1").result().value())
        Assert.assertTrue(parser.parse("x0").isLeft())
    }
}
