package com.prettybyte.chess

import com.expediagroup.graphql.server.ktor.*
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.chess.klerk.UserName
import com.prettybyte.chess.klerk.createConfig
import com.prettybyte.chess.klerk.game.Game
import com.prettybyte.chess.klerk.game.GameState.*
import com.prettybyte.chess.klerk.user.CreateUser
import com.prettybyte.chess.klerk.user.CreateUserParams
import com.prettybyte.chess.plugins.configureRouting
import com.prettybyte.chess.plugins.context
import com.prettybyte.klerk.Klerk
import com.prettybyte.klerk.ModelID
import com.prettybyte.klerk.command.Command
import com.prettybyte.klerk.command.CommandToken
import com.prettybyte.klerk.command.ProcessingOptions
import com.prettybyte.klerk.graphql.EventMutationService
import com.prettybyte.klerk.graphql.GenericQuery
import com.prettybyte.klerk.read.ModelModification
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
            packages = listOf("com.prettybyte.klerk.graphql")
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
    val statesWhereAiShouldAct =
        setOf(BlackTurn.name, BlackPromotePawn.name, WhiteHasProposedDraw.name, WaitingForInvitedPlayer.name)

    // make AI react to events
    GlobalScope.launch {
        klerk.models.subscribe(context, null).collect {
            if (it is ModelModification.Created || it is ModelModification.Transitioned) {
                val model = klerk.read(context) { get(it.id) }
                if (model.props !is Game) {
                    return@collect
                }
                val game = (model.props as Game)
                if (game.blackPlayer == robot.id && statesWhereAiShouldAct.contains(model.state)) {
                    @Suppress("UNCHECKED_CAST")
                    klerk.jobs.scheduleAction(CalculateAiAction(model.id as ModelID<Game>))
                }
            }
        }
    }

    // make AI aware of ongoing games
    val gamesWithBlackTurn = klerk.read(context) {
        list(data.games.all) { it.state == BlackTurn.name }
    }
    gamesWithBlackTurn.forEach {
        klerk.jobs.scheduleAction(CalculateAiAction(it.id))
    }

}
