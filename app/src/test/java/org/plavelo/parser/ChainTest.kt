package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class ChainTest {
    @Test
    @Throws(Exception::class)
    fun chain() {
        val parser = Parser.string("x").chain{
            Assert.assertEquals("x", it.content())
            Parser.string("y")
        }
        val result1 = parser.parse("xy").right().content()
        Assert.assertEquals("y", result1)
        Assert.assertTrue(parser.parse("x").isLeft())
    }
}