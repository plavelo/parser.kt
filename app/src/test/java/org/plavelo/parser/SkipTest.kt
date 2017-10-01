package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class SkipTest {
    @Test
    @Throws(Exception::class)
    fun skip() {
        val parser = Parser.string("x").skip(Parser.string("y"))
        val result1 = parser.parse("xy")
        Assert.assertTrue(result1.isRight())
        Assert.assertEquals("x", result1.result().value())
    }
}
