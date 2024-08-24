package com.prettybyte.chess

import com.prettybyte.chess.klerk.Collections
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.chess.klerk.game.*
import com.prettybyte.klerk.ModelID
import com.prettybyte.klerk.actions.Job
import com.prettybyte.klerk.actions.JobContext
import com.prettybyte.klerk.actions.JobResult
import com.prettybyte.klerk.command.Command
import com.prettybyte.klerk.command.ProcessingOptions
import com.prettybyte.klerk.command.CommandToken
import kotlinx.coroutines.delay
import kotlin.random.Random

class CalculateAiAction(val gameId: ModelID<Game>) : Job<Ctx, Collections> {
    private val random = Random(seed = 1)
    override val id = random.nextLong()
    override val maxRetries: Int = 0

    override suspend fun run(jobContext: JobContext<Ctx, Collections>): JobResult {
        delay(4000)

        val (game, blackPlayer) = klerk.read(Ctx.system()) {
            val game = get(gameId)
            val blackPlayer = get(game.props.blackPlayer)
            Pair(game, blackPlayer)
        }

        val command = when (game.state) {
            GameState.WaitingForInvitedPlayer.name -> Command(AcceptInvite, gameId, null)

            GameState.BlackTurn.name -> {
                val move =
                    calculateAllValidMoves(Board.fromMoves(game.props.moves), GameState.valueOf(game.state)).random()
                Command(MakeMove, gameId, MakeMoveParams(move.from, move.to))
            }

            GameState.WhiteHasProposedDraw.name -> {
                val event = if (random.nextBoolean()) AcceptDraw else DeclineDraw
                Command(event, gameId, null)
            }

            else -> {
                jobContext.log("Cannot handle state ${game.state}")
                return JobResult.Fail
            }
        }

        val result = klerk.handle(
            command,
            Ctx.fromUser(blackPlayer),
            ProcessingOptions(CommandToken.simple()),
        )
        jobContext.log(result.toString())

        return JobResult.Success
    }

}