package dev.klerkframework.chess.klerk

import dev.klerkframework.klerk.Translation
import dev.klerkframework.klerk.validation.Valid
import dev.klerkframework.klerk.datatypes.DurationContainer
import dev.klerkframework.klerk.datatypes.IntContainer
import dev.klerkframework.klerk.datatypes.StringContainer
import dev.klerkframework.klerk.validation.PropertyValidity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class UserName(value: String) : StringContainer(value) {
    override val minLength = 3
    override val maxLength = 50
    override val maxLines = 1
}

class PlayTime(value: Duration) : DurationContainer(value) {
    override fun toString() = value.toString()
}

class PieceString(value: String): StringContainer(value) {
    override val minLength = 1
    override val maxLength = 1
    override val maxLines = 1
}

class CoordinateNotationMove(value: String) : StringContainer(value) {
    override val minLength = 1
    override val maxLength = 10
    override val maxLines = 1

    val from: Position
        get() = Position.from(value[0], value[1].toString().toInt())

    val to: Position
        get() = Position.from(value[3], value[4].toString().toInt())

    fun withPromotedPawn(piece: PieceString): CoordinateNotationMove {
        return CoordinateNotationMove(value.plus("(${piece.value})"))
    }

    fun promotedTo(): PieceString? {
        if (value.endsWith(")")) {
            return PieceString(value[value.length -2].toString())
        }
        return null
    }

    companion object {
        fun move(from: Position, to: Position) = CoordinateNotationMove("${from.column}${from.row}-${to.column}${to.row}")
    }
}

class Position(value: String) : StringContainer(value) {
    override val minLength = 2
    override val maxLength = 2
    override val maxLines = 1

    val column: Char
        get() = value.first()

    val row: Int
        get() = value[1].toString().toInt()

    override fun toString(): String {
        return "$column$row"
    }

    override val validators: Set<(String, Translation) -> PropertyValidity> = setOf(::isSquareOnBoard)

    private fun isSquareOnBoard(square: String, translation: Translation): PropertyValidity {
        if (columns.contains(square[0]) && square[1].digitToIntOrNull() in 1..8) {
            return Valid
        }
        return PropertyValidity.Invalid("Illegal position")
    }

    companion object {
        private val columns = "abcdefgh";

        fun fromIndex(index: Int): Position {
            var c = 1
            var r = 1;
            var currentIndex = 0
            while (currentIndex < index) {
                currentIndex++
                c++
                if (c == 9) {
                    c = 1
                    r++
                }
            }
            return from(columns[c - 1], r);
        }

        fun fromString(value: String): Position {
            require(value.length == 2)
            return from(value.first(), value.last().toString().toInt())
        }

        fun from(column: Char, row: Int) = Position("${column}${row}")

    }

    fun toCartesian(): Int {
        val columnInt = columns.indexOf(column);
        return (row - 1) * 8 + columnInt;
    }
}

class Score(value: Int) : IntContainer(value) {
    override val min = 0
    override val max = Int.MAX_VALUE
}
