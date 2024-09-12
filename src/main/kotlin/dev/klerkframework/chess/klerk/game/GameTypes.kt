package dev.klerkframework.chess.klerk.game

import dev.klerkframework.chess.klerk.*
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.klerk.ModelID

data class Game(
    val whitePlayer: ModelID<User>,
    val blackPlayer: ModelID<User>,
    val moves: List<CoordinateNotationMove>,
    val whitePlayerTime: PlayTime,
    val blackPlayerTime: PlayTime,
)

data class CreateGameParams(
    val whitePlayer: ModelID<User>,
    val blackPlayer: ModelID<User>,
)

data class MakeMoveParams(val from: Position, val to: Position)

data class PromotePawnParams(val piece: PieceString)    // enumcontainer?
