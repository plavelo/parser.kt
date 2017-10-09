package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class AltTest {
    @Test
    @Throws(Exception::class)
    fun alt() {
        val parser = Parser.alt(
                Parser.string("ab"),
                Parser.string("a")
        )
        val result1 = parser.parse("ab")
        Assert.assertTrue(result1.isRight())
        Assert.assertEquals("ab", result1.reply().value())
    }
}

