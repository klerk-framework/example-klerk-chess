package dev.klerkframework.chess

import dev.klerkframework.chess.klerk.Views
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.UserName
import dev.klerkframework.chess.klerk.createConfig
import dev.klerkframework.chess.klerk.createSettings
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.game.GameState.*
import dev.klerkframework.chess.klerk.user.CreateUser
import dev.klerkframework.chess.klerk.user.CreateUserParams
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.chess.plugins.configureRouting
import dev.klerkframework.chess.plugins.ctx
import dev.klerkframework.klerk.view.asSequence
import dev.klerkframework.klerk.view.isEmpty
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.Model
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.graphql.installKlerkGraphQL
import dev.klerkframework.klerk.read.ModelModification
import dev.klerkframework.mcp.createMcpServer
import graphql.GraphQLContext
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.modelcontextprotocol.kotlin.sdk.server.mcpStatelessStreamableHttp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main() {
    val klerk = Klerk.create(createConfig(), createSettings())
    runBlocking {
        klerk.meta.start()
        if (klerk.read(Ctx.system()) { views.users.all.isEmpty() }) {
            createPlayers(klerk)
        }
        initAI(klerk)
    }

    val mcpServer = createMcpServer(klerk, {
        val user = klerk.read(Ctx.system()) {
            views.users.all.asSequence().first { it.props.name.value == "Alice" }
        }
        Ctx.fromUser(user)
    }, "Chess application", "1.0.0")

    suspend fun graphQlContextProvider(graphQlContext: GraphQLContext) = graphQlContext.ctx(klerk)

    val port = System.getenv("CHESS_PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port, host = "0.0.0.0", module = {
        installKlerkGraphQL(klerk, ::graphQlContextProvider)
        configureRouting(klerk)
        mcpStatelessStreamableHttp {
            mcpServer
        }
    }).start(wait = true)
}

suspend fun createPlayers(klerk: Klerk<Ctx, Views>) {
    val commandCreateAlice = Command(
        CreateUser,
        CreateUserParams(UserName("Alice"))
    )
    klerk.handle(commandCreateAlice, Ctx.system())

    val commandCreateRobot = Command(
        CreateUser,
        CreateUserParams(UserName("Mr. Robot"))
    )
    klerk.handle(commandCreateRobot, Ctx.system())
}


@OptIn(DelicateCoroutinesApi::class)
/**
 * Subscribes to game changes. Creates a job if AI should act.
 */
suspend fun initAI(klerk: Klerk<Ctx, Views>) {
    log.info { "Initiating AI" }
    val robot = klerk.read(Ctx.system()) {
        views.users.all.asSequence().first { it.props.name.value == "Mr. Robot" }
    }
    val context = Ctx.fromUser(robot)

    // make AI react to events
    GlobalScope.launch {
        klerk.modelChanges.subscribe(null, context).collect {
            if (it is ModelModification.Created || it is ModelModification.Transitioned) {
                val model = klerk.read(context) { get(it.id) }
                if (model.props !is Game) {
                    return@collect
                }
                val game = model.props as Game
                if (aiShouldAct(game, model.state, robot)) {
                    // A fresh context, since Ctx.time is stamped when the context is created.
                    val aiContext = Ctx.fromUser(robot)
                    @Suppress("UNCHECKED_CAST")
                    klerk.jobs.schedule(
                        CalculateAiAction.declare(AiCursor(model.id as ModelID<Game>)), aiContext
                    )
                }
            }
        }
    }

    // make AI aware of ongoing games
    klerk.read(context) {
        views.games.all.asSequence().filter { aiShouldAct(it.props, it.state, robot) }.toList()
    }.forEach {
        val aiContext = Ctx.fromUser(robot)
        klerk.jobs.schedule(
            CalculateAiAction.declare(AiCursor(it.id), scheduleAt = aiContext.time + AI_THINKING_TIME),
            aiContext
        )
    }

}

fun aiShouldAct(game: Game, state: String, robot: Model<User>): Boolean {
    val statesWhereAiShouldAct =
        setOf(BlackTurn.name, BlackPromotePawn.name, WhiteHasProposedDraw.name, WaitingForInvitedPlayer.name)
    return game.blackPlayer == robot.id && statesWhereAiShouldAct.contains(state)
}
