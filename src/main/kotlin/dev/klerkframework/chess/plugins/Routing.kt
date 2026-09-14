package dev.klerkframework.chess.plugins

import dev.klerkframework.chess.html.*
import dev.klerkframework.chess.klerk.Views
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.graphql.klerkGraphQLRoutes
import dev.klerkframework.klerk.view.asSequence
import dev.klerkframework.klerk.EventReference
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.web.KlerkWeb
import dev.klerkframework.web.Layout
import dev.klerkframework.web.klerkWebRoutes
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(klerk: Klerk<Ctx, Views>) {

    val klerkWeb = KlerkWeb(
        klerk,
        ApplicationCall::ctx,
        canSeeAdminUI = ::canSeeAdminUI,
        layout = Layout(externalCssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css"),
    )

    routing {
        get("/") { listGames(call, klerk, klerkWeb) }
        get("/game/{id}") { renderGame(call, klerk, klerkWeb) }
        post("/game/{id}") { confirmMove(call, klerk) }
        get("/sse/{id}") { handleSse(call, klerk) }

        klerkGraphQLRoutes()

        // The auto-generated Admin UI and model pages
        klerkWebRoutes(klerkWeb, setOf(Game::class, User::class))
    }
}

internal fun showOptionalParameters(event: EventReference) = false

/**
 * Creates a Context from a Call.
 * As authentication is something that should not be handled by Klerk, we will just fake it here.
 */
suspend fun ApplicationCall.ctx(klerk: Klerk<Ctx, Views>): Ctx {
    val user = klerk.read(Ctx.system()) {
        views.users.all.asSequence().first { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}

/**
 * Creates a Context from a GraphQLContext. Used in the GraphQL API.
 *
 * In a real app we would use a session token or similar to figure out who the user is. Here, we always just use the
 * user Alice.
 */
suspend fun GraphQLContext.ctx(klerk: Klerk<Ctx, Views>): Ctx {
    val user = klerk.read(Ctx.system()) {
        views.users.all.asSequence().first { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}

suspend fun canSeeAdminUI(ctx: Ctx): Boolean = true
