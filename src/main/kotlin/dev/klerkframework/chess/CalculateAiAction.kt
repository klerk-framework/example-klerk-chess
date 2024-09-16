package dev.klerkframework.chess

import dev.klerkframework.chess.klerk.Collections
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.*
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.actions.Job
import dev.klerkframework.klerk.actions.JobContext
import dev.klerkframework.klerk.actions.JobResult
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import kotlinx.coroutines.delay
import kotlin.random.Random

class CalculateAiAction(val gameId: ModelID<Game>, val klerk: Klerk<Ctx, Collections>) : Job<Ctx, Collections> {
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
