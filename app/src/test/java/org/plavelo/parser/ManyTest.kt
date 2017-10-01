package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class ManyTest {
    @Test
    @Throws(Exception::class)
    fun many() {
        val parser = Parser.regex("[a-z]", RegexOption.IGNORE_CASE).many()
        val result1 = parser.parse("x").result().value() as List<*>
        Assert.assertEquals(1, result1.size)
        Assert.assertEquals("x", result1[0])
        val result2 = parser.parse("xyz").result().value() as List<*>
        Assert.assertEquals(3, result2.size)
        Assert.assertEquals("x", result2[0])
        Assert.assertEquals("y", result2[1])
        Assert.assertEquals("z", result2[2])
    }
}
