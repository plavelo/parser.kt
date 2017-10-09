package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class ChainTest {
    @Test
    @Throws(Exception::class)
    fun chain() {
        val parser = Parser.string("x").chain{
            Assert.assertEquals("x", it.value())
            Parser.string("y")
        }
        val result1 = parser.parse("xy").reply().value()
        Assert.assertEquals("y", result1)
        Assert.assertTrue(parser.parse("x").isLeft())
    }
}