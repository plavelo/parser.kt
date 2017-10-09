package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class EofTest {
    @Test
    @Throws(Exception::class)
    fun eof() {
        val parser = Parser.regex("\\s*")
                .skip(Parser.eof)
                .or(Parser.all.result("default"))
        Assert.assertEquals("  ", parser.parse("  ").reply().value())
        Assert.assertEquals("default", parser.parse("x").reply().value())
    }
}
