package dev.klerkframework.chess.plugins

import com.expediagroup.graphql.server.ktor.graphQLGetRoute
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.klerkframework.chess.klerk.game.IsAutomaticDraw
import dev.klerkframework.chess.html.*
import dev.klerkframework.chess.klerk.Collections
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.klerk.EventReference
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.web.LowCodeConfig
import dev.klerkframework.web.LowCodeMain
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(klerk: Klerk<Ctx, Collections>) {
    suspend fun contextFromCall(call: ApplicationCall): Ctx = call.context(klerk)
    val lowCodeConfig = LowCodeConfig(
        basePath = "/admin",
        contextProvider = ::contextFromCall,
        showOptionalParameters = ::showOptionalParameters,
        cssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css",
        knownAlgorithms = setOf(IsAutomaticDraw)
    )

    routing {
        get("/") { listGames(call, klerk, lowCodeConfig) }
        get("/game/{id}") { renderGame(call, klerk, lowCodeConfig) }
        post("/game/{id}") { confirmMove(call, klerk) }
        get("/sse/{id}") { handleSse(call, klerk) }

        // GraphQL
        graphQLPostRoute()
        graphQLGetRoute()
        graphiQLRoute()
        graphQLSDLRoute()

        // The auto-generated Admin UI
        val autoAdminUI = LowCodeMain(klerk, lowCodeConfig)
        apply(autoAdminUI.registerRoutes())
    }
}

internal fun showOptionalParameters(event: EventReference) = false

/**
 * Creates a Context from a Call.
 * As authentication is something that should not be handled by Klerk, we will just fake it here.
 */
suspend fun ApplicationCall.context(klerk: Klerk<Ctx, Collections>): Ctx {
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
suspend fun GraphQLContext.context(klerk: Klerk<Ctx, Collections>): Ctx {
    val user = klerk.read(Ctx.authenticationIdentity()) {
        getFirstWhere(data.users.all) { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}
