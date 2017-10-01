package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class ThenTest {
    @Test
    @Throws(Exception::class)
    fun then() {
        val parser = Parser.string("x").then(Parser.string("y"))
        val result1 = parser.parse("xy")
        Assert.assertTrue(result1.isRight())
        Assert.assertEquals("y", result1.result().value())
    }
}