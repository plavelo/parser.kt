package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class SepByTest {
    @Test
    @Throws(Exception::class)
    fun sepBy() {
        val characters = Parser.regex("[a-zA-Z]+")
        val comma = Parser.string(",")
        val parser = Parser.sepBy(characters, comma)
        val result1 = parser.parse("one,two,three,four,five").right().content() as List<*>
        Assert.assertEquals(5, result1.size)
        Assert.assertEquals("one", result1[0])
        Assert.assertEquals("two", result1[1])
        Assert.assertEquals("three", result1[2])
        Assert.assertEquals("four", result1[3])
        Assert.assertEquals("five", result1[4])
    }
}