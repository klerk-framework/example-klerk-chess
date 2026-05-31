package dev.klerkframework.chess

import dev.klerkframework.chess.klerk.Collections
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.*
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import dev.klerkframework.klerk.job.Job
import dev.klerkframework.klerk.job.JobMetadata
import dev.klerkframework.klerk.job.JobResult
import dev.klerkframework.klerk.job.RunnableJob
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.random.Random

class CalculateAiAction(val gameId: ModelID<Game>, val klerk: Klerk<Ctx, Collections>) : RunnableJob<Ctx, Collections>() {
    override val maxRetries: Int = 0
    override val parameters: String = gameId.toString()

    companion object {
        private val log = KotlinLogging.logger {}
        private val random = Random(seed = 1)

        suspend fun run(metadata: JobMetadata, klerk: Klerk<Ctx, Collections>): JobResult {
            delay(4000)

            val (game, blackPlayer) = klerk.read(Ctx.system()) {
                val gameId = ModelID<Game>(metadata.parameters.toInt())
                val game = get(gameId)
                val blackPlayer = get(game.props.blackPlayer)
                Pair(game, blackPlayer)
            }

            val command = when (game.state) {
                GameState.WaitingForInvitedPlayer.name -> Command(AcceptInvite, game.id, null)

                GameState.BlackTurn.name -> {
                    val move =
                        calculateAllValidMoves(
                            Board.fromMoves(game.props.moves),
                            GameState.valueOf(game.state)
                        ).random()
                    Command(MakeMove, game.id, MakeMoveParams(move.from, move.to))
                }

                GameState.WhiteHasProposedDraw.name -> {
                    val event = if (random.nextBoolean()) AcceptDraw else DeclineDraw
                    Command(event, game.id, null)
                }

                else -> {
                    log.info("Cannot handle state ${game.state}")
                    return JobResult.Fail()
                }
            }

            val result = klerk.handle(
                command,
                Ctx.fromUser(blackPlayer),
                ProcessingOptions(CommandToken.simple()),
            )
            log.info(result.toString())

            return JobResult.Success()
        }
    }

    override fun getRunFunction() = CalculateAiAction::run

}
