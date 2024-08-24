package com.prettybyte.chess.klerk.game

import com.prettybyte.chess.klerk.Collections
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.chess.klerk.game.ShowNotificationDecisions.*
import com.prettybyte.klerk.ArgForInstanceNonEvent
import com.prettybyte.klerk.misc.AlgorithmBuilder
import com.prettybyte.klerk.misc.Decision
import com.prettybyte.klerk.misc.FlowChartAlgorithm

object IsAutomaticDraw : FlowChartAlgorithm<ArgForInstanceNonEvent<Game, Ctx, Collections>, Boolean>("Is it a draw?") {

    override fun configure(): AlgorithmBuilder<ArgForInstanceNonEvent<Game, Ctx, Collections>, Boolean>.() -> Unit = {

        start(IsStalemate)

        booleanNode(IsStalemate) {
            on(true, terminateWith = true)
            on(false, next = IsDeadPosition)
        }

        booleanNode(IsDeadPosition) {
            on(true, terminateWith = true)
            on(false, next = IsFivefoldRepetition)
        }

        booleanNode(IsFivefoldRepetition) {
            on(true, terminateWith = true)
            on(false, terminateWith = false)
        }

    }

}

sealed class ShowNotificationDecisions<T>(
    override val name: String,
    override val function: (ArgForInstanceNonEvent<Game, Ctx, Collections>) -> T
) : Decision<T, ArgForInstanceNonEvent<Game, Ctx, Collections>> {

    data object IsStalemate : ShowNotificationDecisions<Boolean>("Is it a stalemate?", ::isStalemate)

    data object IsDeadPosition : ShowNotificationDecisions<Boolean>("Is it a dead position?", ::isDeadPosition)

    data object IsFivefoldRepetition : ShowNotificationDecisions<Boolean>("Has there been five repeated moves?", ::isStalemate)
}
