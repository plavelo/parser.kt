package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class OrTest {
    @Test
    @Throws(Exception::class)
    fun or() {
        val parser = Parser.string("x").or(Parser.string("y"))
        Assert.assertEquals("x", parser.parse("x").reply().value())
        Assert.assertEquals("y", parser.parse("y").reply().value())
        Assert.assertTrue(parser.parse("z").isLeft())
    }
}
