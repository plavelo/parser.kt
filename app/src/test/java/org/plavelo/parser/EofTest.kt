package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class EofTest {
    @Test
    @Throws(Exception::class)
    fun eof() {
        val parser = Parser.regex("\\s*")
                .skip(Parser.EOF)
                .or(Parser.all.result("default"))
        Assert.assertEquals("  ", parser.parse("  ").result().value())
        Assert.assertEquals("default", parser.parse("x").result().value())
    }
}
