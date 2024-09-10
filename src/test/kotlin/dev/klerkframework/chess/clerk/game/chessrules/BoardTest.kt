package dev.klerkframework.chess.klerk.game.chessrules

import dev.klerkframework.chess.klerk.CoordinateNotationMove
import dev.klerkframework.chess.klerk.Position
import dev.klerkframework.chess.klerk.game.Board
import dev.klerkframework.chess.klerk.game.isBlackCheck
import kotlin.test.Test
import kotlin.test.assertTrue

class BoardTest {

    @Test
    fun moves() {
        val movesCheck = listOf(
            CoordinateNotationMove.move(from = Position("e2"), to = Position("e4")),
            CoordinateNotationMove.move(from = Position("e7"), to = Position("e5")),
            CoordinateNotationMove.move(from = Position("d1"), to = Position("h5")),
            CoordinateNotationMove.move(from = Position("g7"), to = Position("g6")),
            CoordinateNotationMove.move(from = Position("h5"), to = Position("g6")),
            CoordinateNotationMove.move(from = Position("g8"), to = Position("f6")),
            CoordinateNotationMove.move(from = Position("g6"), to = Position("f6")),
            CoordinateNotationMove.move(from = Position("f8"), to = Position("a3")),
            CoordinateNotationMove.move(from = Position("f1"), to = Position("c4")),
            CoordinateNotationMove.move(from = Position("h7"), to = Position("h6")),
            CoordinateNotationMove.move(from = Position("f6"), to = Position("f7")),
        )

        val board = Board.fromMoves(movesCheck)
        assertTrue(isBlackCheck(board))
    }
}
