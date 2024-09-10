package dev.klerkframework.chess.plugins

import com.expediagroup.graphql.server.ktor.graphQLGetRoute
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.klerkframework.chess.klerk
import dev.klerkframework.chess.klerk.game.IsAutomaticDraw
import dev.klerkframework.chess.html.*
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.klerk.EventReference
import dev.klerkframework.webutils.LowCodeConfig
import dev.klerkframework.webutils.LowCodeMain
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        get("/") { listGames(call) }
        get("/game/{id}") { renderGame(call) }
        post("/game/{id}") { confirmMove(call) }
        get("/sse/{id}") { handleSse(call) }

        // GraphQL
        graphQLPostRoute()
        graphQLGetRoute()
        graphiQLRoute()
        graphQLSDLRoute()

        // The auto-generated Admin UI
        apply(autoAdminUI.registerRoutes())

    }
}

private val autoAdminUI = LowCodeMain(
    klerk, LowCodeConfig(
        basePath = "/admin",
        contextProvider = ::contextFromCall,
        showOptionalParameters = ::showOptionalParameters,
        cssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css",
        knownAlgorithms = setOf(IsAutomaticDraw)
    )
)

suspend fun contextFromCall(call: ApplicationCall): Ctx = call.context()

internal fun showOptionalParameters(event: EventReference): Boolean {
    return false
}

/**
 * Creates a Context from a Call.
 * As authentication is something that should not be handled by Klerk, we will just fake it here.
 */
suspend fun ApplicationCall.context(): Ctx {
    val user = klerk.read(Ctx.system()) {
        getFirstWhere(data.users.all) { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}

/**
 * Creates a Context from a GraphQLContext. Used in the GraphQL API.
 *
 * In a real app we would use a session token or similar to figure out who the user is. Here, we always just use the
 * user Alice.
 */
suspend fun GraphQLContext.context(): Ctx {
    val user = klerk.read(Ctx.authenticationIdentity()) {
        getFirstWhere(data.users.all) { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}
