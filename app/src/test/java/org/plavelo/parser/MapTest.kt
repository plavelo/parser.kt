package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class MapTest {
    @Test
    @Throws(Exception::class)
    fun map() {
        val parser = Parser.string("x").map {
            Assert.assertEquals("x", it.content())
            Value.Single("y")
        }
        val result1 = parser.parse("x")
        Assert.assertTrue(result1.isRight())
        Assert.assertEquals("y", result1.right().content())
    }
}
