package dev.klerkframework.chess.klerk.user

import dev.klerkframework.chess.klerk.Score
import dev.klerkframework.chess.klerk.UserName

data class User(val name: UserName, val score: Score) {

    override fun toString(): String {
        return name.toString()
    }

}

class CreateUserParams(val name: UserName)

class UpdateScoreParams(val delta: Score)
