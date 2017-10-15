package org.plavelo.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringTest {
    @Test
    @Throws(Exception::class)
    fun string() {
        val parser = Parser.string("a")
        val result1 = parser.parse("a")
        assertTrue(result1.isRight())
        assertEquals("a", result1.right().content())
    }
}
