package dev.klerkframework.chess

import com.expediagroup.graphql.server.ktor.*
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.UserName
import dev.klerkframework.chess.klerk.createConfig
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.game.GameState.*
import dev.klerkframework.chess.klerk.user.CreateUser
import dev.klerkframework.chess.klerk.user.CreateUserParams
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.chess.plugins.configureRouting
import dev.klerkframework.chess.plugins.context
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.Model
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import dev.klerkframework.klerk.graphql.EventMutationService
import dev.klerkframework.klerk.graphql.GenericQuery
import dev.klerkframework.klerk.read.ModelModification
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

val klerk = Klerk.create(createConfig())

fun main() {
    runBlocking {
        klerk.meta.start()
        if (klerk.meta.modelsCount == 0) {
            createPlayers()
        }
        initAI()
    }
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

suspend fun createPlayers() {
    val commandCreateAlice = Command(
        event = CreateUser,
        model = null,
        params = CreateUserParams(UserName("Alice")),
    )
    klerk.handle(commandCreateAlice, Ctx.system(), ProcessingOptions(CommandToken.simple()))

    val commandCreateRobot = Command(
        event = CreateUser,
        model = null,
        params = CreateUserParams(UserName("Mr. Robot")),
    )
    klerk.handle(commandCreateRobot, Ctx.system(), ProcessingOptions(CommandToken.simple()))
}

fun Application.module() {
    install(GraphQL) {
        schema {
            packages = listOf("dev.klerkframework.klerk.graphql")
            queries = listOf(GenericQuery(klerk, GraphQLContext::context))
            mutations = listOf(EventMutationService(klerk, GraphQLContext::context))
        }
    }
    configureRouting()
}

@OptIn(DelicateCoroutinesApi::class)
/**
 * Subscribes to game changes. Creates a job if AI should act.
 */
suspend fun initAI() {
    log.info { "Initiating AI" }
    val robot = klerk.read(Ctx.system()) { getFirstWhere(data.users.all) { it.props.name.string == "Mr. Robot" } }
    val context = Ctx.fromUser(robot)

    // make AI react to events
    GlobalScope.launch {
        klerk.models.subscribe(context, null).collect {
            if (it is ModelModification.Created || it is ModelModification.Transitioned) {
                val model = klerk.read(context) { get(it.id) }
                if (model.props !is Game) {
                    return@collect
                }
                val game = (model.props as Game)
                if (aiShouldAct(game, model.state, robot)) {
                    @Suppress("UNCHECKED_CAST")
                    klerk.jobs.scheduleAction(CalculateAiAction(model.id as ModelID<Game>))
                }
            }
        }
    }

    // make AI aware of ongoing games
    klerk.read(context) {
        list(data.games.all) { aiShouldAct(it.props, it.state, robot) }
    }.forEach {
        klerk.jobs.scheduleAction(CalculateAiAction(it.id))
    }

}

fun aiShouldAct(game: Game, state: String, robot: Model<User>): Boolean {
    val statesWhereAiShouldAct =
        setOf(BlackTurn.name, BlackPromotePawn.name, WhiteHasProposedDraw.name, WaitingForInvitedPlayer.name)
    return game.blackPlayer == robot.id && statesWhereAiShouldAct.contains(state)
}
