package com.prettybyte.chess.klerk.user

import com.prettybyte.chess.klerk.Score
import com.prettybyte.chess.klerk.UserName

data class User(val name: UserName, val score: Score) {

    override fun toString(): String {
        return name.toString()
    }

}

class CreateUserParams(val name: UserName)

class UpdateScoreParams(val delta: Score)
