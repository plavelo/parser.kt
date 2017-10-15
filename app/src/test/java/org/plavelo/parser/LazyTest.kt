package org.plavelo.parser

import org.junit.Assert
import org.junit.Test

class LazyTest {
    @Test
    @Throws(Exception::class)
    fun lazy() {
        val parser = Parser.lazy(lazy {
            Parser.alt(
                    Parser.string("ab"),
                    Parser.string("a")
            )
        })
        val result1 = parser.parse("ab")
        Assert.assertTrue(result1.isRight())
        Assert.assertEquals("ab", result1.right().content())
    }
}
