package dev.klerkframework.chess

import dev.klerkframework.chess.klerk.Views
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.*
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.job.*
import dev.klerkframework.klerk.view.asSequence
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/** How long the AI pretends to think before it acts. */
val AI_THINKING_TIME = 4.seconds

@Serializable
data class AiCursor(val gameId: ModelID<Game>)

object CalculateAiAction : JobType.Local<AiCursor, Ctx, Views>() {

    private val log = KotlinLogging.logger {}
    private val random = Random(seed = 1)

    override val name = JobName("calculate-ai-action")
    // The commands are validated with e.g. onlyByBlackPlayer, so they must be applied as the user that scheduled
    // the job (Mr. Robot), not as the system.
    override val agent = JobAgent.Scheduler
    override val maxRetries = 0

    // Note: the thinking pause is the job's scheduleAt, not a delay() inside the step. The context a step runs under
    // (and thus the time the emitted command is applied at) is built before the step starts, so sleeping here would
    // hide the AI's thinking time from the players' clocks.
    override suspend fun step(args: JobStepArgs.Local<AiCursor, Ctx, Views>): JobResult<AiCursor, Ctx, Views> {
        delay(AI_THINKING_TIME)     // simulate thinking
        val game = with(args.reader) { get(args.cursor.gameId) }

        val command = when (game.stateAs<GameState>()) {
            GameState.WaitingForInvitedPlayer -> Command(AcceptInvite, game.id)

            GameState.BlackTurn -> {
                val move = calculateAllValidMoves(
                    Board.fromMoves(game.props.moves),
                    game.stateAs<GameState>()
                ).random()
                Command(MakeMove, game.id, MakeMoveParams(move.from, move.to))
            }

            GameState.WhiteHasProposedDraw -> {
                val event = if (random.nextBoolean()) AcceptDraw else DeclineDraw
                Command(event, game.id)
            }

            else -> {
                log.info("Cannot handle state ${game.state}")
                return JobResult.Abort("Cannot handle state ${game.state}")
            }
        }

        return JobResult.Success(command = command)
    }
}
