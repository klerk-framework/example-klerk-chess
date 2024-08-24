package com.prettybyte.chess.klerk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class PositionTest {

    @Test
    fun illegalPosition() {
        assertNotNull(Position.from("a".first(), 0).validate("test"))
    }

    @Test
    fun fromIndex() {
        assertEquals(Position.from("a".first(), 1), Position.fromIndex(0))
        assertEquals(Position.from("h".first(), 1), Position.fromIndex(7))
        assertEquals(Position.from("a".first(), 8), Position.fromIndex(56))
        assertEquals(Position.from("h".first(), 8), Position.fromIndex(63))
    }
}