package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class OrTest {
    @Test
    @Throws(Exception::class)
    fun or() {
        val parser = Parser.string("x").or(Parser.string("y"))
        Assert.assertEquals("x", parser.parse("x").right().content())
        Assert.assertEquals("y", parser.parse("y").right().content())
        Assert.assertTrue(parser.parse("z").isLeft())
    }
}
