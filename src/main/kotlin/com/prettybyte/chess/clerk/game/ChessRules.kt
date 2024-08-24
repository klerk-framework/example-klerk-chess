package com.prettybyte.chess.klerk.game

import com.prettybyte.chess.klerk.CoordinateNotationMove
import com.prettybyte.chess.klerk.Position
import com.prettybyte.chess.klerk.Collections
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.chess.klerk.game.Color.Black
import com.prettybyte.chess.klerk.game.Color.White
import com.prettybyte.chess.klerk.game.Piece.*
import com.prettybyte.klerk.ArgForInstanceNonEvent
import com.prettybyte.klerk.Model

class Board(val pieces: List<String>) {

    fun getPieceAt(x: Int, y: Int): Pair<Piece, Color>? {
        if (x !in 0..7 || y !in 0..7) {
            return null
        }
        val piece = pieces[y * 8 + x]
        if (piece.isEmpty()) {
            return null
        }
        val pieceType = piece.substring(1, 2)
        val pieceColor = piece.substring(0, 1)
        return Pair(getPieceFromAbbreviation(pieceType), if (pieceColor == "w") White else Black)
    }

    fun positionOfKing(color: Color): Position {
        return when (color) {
            White -> Position.fromIndex(pieces.indexOf("wk"))
            Black -> Position.fromIndex(pieces.indexOf("bk"))
        }
    }

    fun afterMove(move: CoordinateNotationMove): Board {
        val piecesAfter = pieces.toMutableList()
        piecesAfter[squareToIndex(move.to)] = pieces[squareToIndex(move.from)]
        piecesAfter[squareToIndex(move.from)] = ""
        if (move.from == Position.from("e".first(), 1) && move.to == Position.from("g".first(), 1)) {   // castling
            piecesAfter[squareToIndex(Position.from("f".first(), 1))] = pieces[squareToIndex(Position.from("h".first(), 1))]
            piecesAfter[squareToIndex(Position.from("h".first(), 1))] = ""
        }
        if (move.from == Position.from("e".first(), 1) && move.to == Position.from("c".first(), 1)) {   // castling
            piecesAfter[squareToIndex(Position.from("d".first(), 1))] = pieces[squareToIndex(Position.from("a".first(), 1))]
            piecesAfter[squareToIndex(Position.from("a".first(), 1))] = ""
        }
        move.promotedTo()?.let {
            val colorLetter = piecesAfter[squareToIndex(move.to)].first()
            piecesAfter[squareToIndex(move.to)] = "${colorLetter}${it.string}"
        }
        return Board(piecesAfter)
    }

    fun canPromotePawn(color: Color): Boolean =
        when (color) {
            White -> pieces.subList(56, 64).contains("wp")
            Black -> pieces.subList(0, 8).contains("bp")
        }

    companion object {
        fun getSquareName(x: Int, y: Int): String {
            return "abcdefgh"[x] + (y + 1).toString()
        }

        fun getSquareName(sq: SquareCoordinates): String = getSquareName(sq.first, sq.second)

        fun isSquareWhite(squareIndex: Int): Boolean {
            val row = squareIndex / 8
            val column = squareIndex % 8
            return if (row % 2 == 0) column % 2 != 0 else column % 2 == 0
        }

        fun squareToIndex(pos: Position): Int {
            return (pos.row - 1) * 8 + "abcdefgh".indexOfFirst { it == pos.column }
        }

        private fun getPieceFromAbbreviation(pieceType: String): Piece {
            return when (pieceType) {
                "p" -> Pawn
                "r" -> Rook
                "b" -> Bishop
                "q" -> Queen
                "k" -> King
                "n" -> Knight
                else -> throw RuntimeException()
            }
        }

        fun fromMoves(moves: List<CoordinateNotationMove>): Board {
            var board = Board(setupPieces())
            var state = GameState.WhiteTurn
            moves.forEach {
                board = board.afterMove(it)
                state = when (state) {
                    GameState.WhiteTurn -> GameState.BlackTurn
                    GameState.BlackTurn -> GameState.WhiteTurn
                    else -> error("illegal state")
                }
            }
            return board
        }

    }

}

enum class Piece(p: String) {
    Pawn("p"), Rook("r"), Knight("k"), Bishop("b"), Queen("q"), King("k")
}

enum class Color(c: String) {
    White("w"), Black("b")
}

typealias SquareCoordinates = Pair<Int, Int>

fun setupPieces(): List<String> =
    listOf(
        "wr",
        "wn",
        "wb",
        "wq",
        "wk",
        "wb",
        "wn",
        "wr",
        "wp",
        "wp",
        "wp",
        "wp",
        "wp",
        "wp",
        "wp",
        "wp",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "bp",
        "bp",
        "bp",
        "bp",
        "bp",
        "bp",
        "bp",
        "bp",
        "br",
        "bn",
        "bb",
        "bq",
        "bk",
        "bb",
        "bn",
        "br"
    )

fun calculateAllValidMoves(
    board: Board,
    state: GameState,
    removeIfKingIsExposed: Boolean = true,
    excludeCastling: Boolean = false
): Set<CoordinateNotationMove> {
    val validMoves = mutableSetOf<CoordinateNotationMove>()
    for (x in 0..7) {
        for (y in 0..7) {
            val piece = board.getPieceAt(x, y) ?: continue
            if ((piece.second == White && state == GameState.WhiteTurn) || (piece.second == Black && state == GameState.BlackTurn)) {
                validMoves.addAll(validMovesForPiece(board, x, y, excludeCastling))
            }
        }
    }
    return if (removeIfKingIsExposed) {
        validMoves.filter { !willMoveLeaveKingChecked(board, state, it) }.toSet()
    } else {
        validMoves
    }
}

fun isWhiteCheck(board: Board): Boolean {
    val kingPosition = board.positionOfKing(White)
    return calculateAllValidMoves(
        board,
        GameState.BlackTurn,
        removeIfKingIsExposed = false,
        excludeCastling = true
    ).any { it.to == kingPosition }
}

fun isBlackCheck(board: Board): Boolean {
    val kingPosition = board.positionOfKing(Black)
    val validMoves = calculateAllValidMoves(
        board,
        GameState.WhiteTurn,
        removeIfKingIsExposed = false,
        excludeCastling = true
    )

    return validMoves.any { it.to == kingPosition }
}

fun canClaimDraw(model: Model<Game>): Boolean = isThreefoldRepetition() || isFiftyMoves()

fun isStalemate(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean {
    val board = Board.fromMoves(args.model.props.moves)
    return when (GameState.valueOf(args.model.state)) {
        GameState.WhiteTurn -> !isWhiteCheck(board) && calculateAllValidMoves(board, GameState.WhiteTurn,  true).isEmpty()
        GameState.BlackTurn -> !isBlackCheck(board) && calculateAllValidMoves(board, GameState.BlackTurn,  true).isEmpty()
        else -> throw Exception()
    }
}

fun isDeadPosition(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean = isInsufficientMaterial(args.model)   // We are not trying to figure out other dead positions

fun isFivefoldRepetition(): Boolean {
    return false // TODO
}

private fun willMoveLeaveKingChecked(board: Board, state: GameState, move: CoordinateNotationMove): Boolean {
    // figure out if the opponent can take the king next move
    val boardAfterMove = board.afterMove(move)
    return if (state == GameState.WhiteTurn) isWhiteCheck(boardAfterMove) else isBlackCheck(boardAfterMove)
}

private fun isInsufficientMaterial(model: Model<Game>): Boolean {
    // will only check for insufficient material
    val board = Board.fromMoves(model.props.moves)
    val remainingPieces = board.pieces.filter { it.isNotEmpty() }
    if (remainingPieces.size == 2) {   // king vs king
        return true
    }
    if (remainingPieces.size == 3) {
        return (remainingPieces.contains("wb") || remainingPieces.contains("bb")) ||    // king and bishop vs king
                (remainingPieces.contains("wn") || remainingPieces.contains("bn"))      // king and knight vs king
    }
    if (remainingPieces.filter { it.isNotEmpty() }.size == 4) {
        return remainingPieces.contains("wb") && remainingPieces.contains("bb") &&  // King and bishop vs king and bishop of the same color as the opponent's bishop
                Board.isSquareWhite(remainingPieces.indexOf("wb")) == Board.isSquareWhite(remainingPieces.indexOf("bb"))
    }
    return false
}

private fun isThreefoldRepetition(): Boolean {
    return false // TODO
}

private fun isFiftyMoves(): Boolean {
    return false // TODO
}

private fun validMovesForPiece(board: Board, x: Int, y: Int, excludeCastling: Boolean): Set<CoordinateNotationMove> {
    val piece = board.getPieceAt(x, y) ?: throw Exception()
    return when (piece.first) {
        Pawn -> validMovesForPawn(board, x, y, piece.second)
        King -> validMovesForKing(board, x, y, piece.second, excludeCastling)
        Rook -> validMovesForRook(board, x, y, piece.second)
        Bishop -> validMovesForBishop(board, x, y, piece.second)
        Queen -> validMovesForQueen(board, x, y, piece.second)
        Knight -> validMovesForKnight(board, x, y, piece.second)
    }
}

private fun validMovesForPawn(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    val deltaY = if (color == White) 1 else -1
    val validMoves = mutableSetOf<CoordinateNotationMove>()
    val from = Position.fromString(Board.getSquareName(x, y))
    if (y == 7) {
        return emptySet()
    }
    if (board.getPieceAt(x, y + deltaY) == null) {
        validMoves.add(CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(x, y + deltaY))))
    }
    if ((color == White && y == 1) || (color == Black && y == 6) &&
        board.getPieceAt(x, y + deltaY) == null &&
        board.getPieceAt(x, y + deltaY + deltaY) == null
    ) {
        validMoves.add(CoordinateNotationMove.move(from, Position.fromString(
            Board.getSquareName(
                x,
                y + deltaY + deltaY
            )
        ))) // two steps forward
    }
    val pieceForwardRight = board.getPieceAt(x + 1, y + deltaY)
    if (x < 7 && pieceForwardRight != null && pieceForwardRight.second != color) {
        validMoves.add(CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(x + 1, y + deltaY))))
    }
    val pieceForwardLeft = board.getPieceAt(x - 1, y + deltaY)
    if (x > 0 && pieceForwardLeft != null && pieceForwardLeft.second != color) {
        validMoves.add(CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(x - 1, y + deltaY))))
    }
    // TODO: en passant
    return validMoves
}

private fun validMovesForKing(board: Board, x: Int, y: Int, color: Color, excludeCastling: Boolean): Set<CoordinateNotationMove> {
    val validMoves = mutableSetOf<CoordinateNotationMove>()
    val from = Position.fromString(Board.getSquareName(x, y))
    val toSquares = setOf(
        Pair(x, y + 1), Pair(x + 1, y + 1), Pair(x + 1, y), Pair(x + 1, y - 1), Pair(x, y - 1), Pair(x - 1, y - 1),
        Pair(x - 1, y), Pair(x - 1, y + 1)
    )
    val squaresOnBoard = toSquares.filter { it.first in 0..7 && it.second in 0..7 }
    squaresOnBoard.forEach {
        val pieceAtSquare = board.getPieceAt(it.first, it.second)
        if (pieceAtSquare == null || pieceAtSquare.second != color) {
            validMoves.add(CoordinateNotationMove.move(from, Position.fromString(
                Board.getSquareName(
                    it.first,
                    it.second
                )
            )))
        }
    }
    if (!excludeCastling) {
        validMoves.addAll(validCastlingMoves(board, x, y, color))
    }
    return validMoves
}

private fun validCastlingMoves(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    val validMoves = mutableSetOf<CoordinateNotationMove>()
    val from = Position.fromString(Board.getSquareName(x, y))
    when (color) {
        White -> {
            if (isWhiteCheck(board)) {
                return emptySet()
            }
            if (board.getPieceAt(5, 0) == null &&
                board.getPieceAt(6, 0) == null &&
                !blackCanMoveTo(5, 0, board, excludeCastling = true) &&
                !blackCanMoveTo(6, 0, board, true) &&
                !pieceHasMovedFrom(4, 0) &&
                !pieceHasMovedFrom(7, 0)
            ) {
                validMoves.add(CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(6, 0))))
            }
            if (board.getPieceAt(1, 0) == null &&
                board.getPieceAt(2, 0) == null &&
                board.getPieceAt(3, 0) == null &&
                !blackCanMoveTo(1, 0, board, true) &&
                !blackCanMoveTo(2, 0, board, true) &&
                !blackCanMoveTo(3, 0, board, true) &&
                !pieceHasMovedFrom(4, 0) &&
                !pieceHasMovedFrom(0, 0)
            ) {
                validMoves.add(CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(2, 0))))
            }
        }
        Black -> {
            // TODO
        }
    }
    return validMoves
}

private fun blackCanMoveTo(x: Int, y: Int, board: Board, excludeCastling: Boolean): Boolean {
    return calculateAllValidMoves(board, GameState.BlackTurn, excludeCastling = excludeCastling).any {
        it.to == Position.fromString(
            Board.getSquareName(
                x,
                y
            )
        )
    }
}

private fun pieceHasMovedFrom(x: Int, y: Int): Boolean {
    return false     // TODO
    /*    val squareName = Board.getSquareName(x, y)
        return simpleBackend.getEventsForModelId(gameId).any { it is MakeMove && it.getParams().from == squareName } */
}

private fun validMovesForRook(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    val from = Position.fromString(Board.getSquareName(x, y))
    val validSquares = mutableSetOf<SquareCoordinates>()
    validSquares.addAll(squaresInLine(board, x, y, color, 0, 1))
    validSquares.addAll(squaresInLine(board, x, y, color, 0, -1))
    validSquares.addAll(squaresInLine(board, x, y, color, 1, 0))
    validSquares.addAll(squaresInLine(board, x, y, color, -1, 0))
    return validSquares.map { CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(it))) }.toSet()
}

private fun validMovesForBishop(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    val from = Position.fromString(Board.getSquareName(x, y))
    val validSquares = mutableSetOf<SquareCoordinates>()
    validSquares.addAll(squaresInLine(board, x, y, color, 1, 1))
    validSquares.addAll(squaresInLine(board, x, y, color, 1, -1))
    validSquares.addAll(squaresInLine(board, x, y, color, -1, 1))
    validSquares.addAll(squaresInLine(board, x, y, color, -1, -1))
    return validSquares.map { CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(it))) }.toSet()
}

private fun validMovesForQueen(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    return setOf(*validMovesForBishop(board, x, y, color).toTypedArray(), *validMovesForRook(board, x, y, color).toTypedArray())
}

private fun validMovesForKnight(board: Board, x: Int, y: Int, color: Color): Set<CoordinateNotationMove> {
    val from = Position.fromString(Board.getSquareName(x, y))
    val validSquares = mutableSetOf<SquareCoordinates>()
    val destinations = arrayOf(
        SquareCoordinates(x + 1, y + 2),
        SquareCoordinates(x + 2, y + 1),
        SquareCoordinates(x + 2, y - 1),
        SquareCoordinates(x + 1, y - 2),
        SquareCoordinates(x - 1, y - 2),
        SquareCoordinates(x - 2, y - 1),
        SquareCoordinates(x - 2, y + 1),
        SquareCoordinates(x - 1, y + 2),
    )
    destinations.forEach {
        if (it.first in 0..7 && it.second in 0..7 && board.getPieceAt(it.first, it.second)?.second != color) {
            validSquares.add(it)
        }
    }
    return validSquares.map { CoordinateNotationMove.move(from, Position.fromString(Board.getSquareName(it))) }.toSet()
}

private fun squaresInLine(board: Board, x: Int, y: Int, color: Color, deltaX: Int, deltaY: Int): Set<SquareCoordinates> {
    val squares = mutableSetOf<SquareCoordinates>()
    var squareWasAdded: Boolean
    var yToCheck = y + deltaY
    var xToCheck = x + deltaX
    var capturedPiece = false
    do {
        val piece = board.getPieceAt(xToCheck, yToCheck)
        if (yToCheck in 0..7 && xToCheck in 0..7 && (piece == null || piece.second != color)) {
            squares.add(Pair(xToCheck, yToCheck))
            squareWasAdded = true
            yToCheck = yToCheck + deltaY
            xToCheck = xToCheck + deltaX
            if (piece != null && piece.second != color) {
                capturedPiece = true
            }
        } else {
            squareWasAdded = false
        }
    } while (squareWasAdded && !capturedPiece)
    return squares
}
