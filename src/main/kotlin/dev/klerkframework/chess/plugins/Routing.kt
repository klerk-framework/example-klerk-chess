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
import dev.klerkframework.web.DefaultPathProvider
import dev.klerkframework.web.KlerkWeb
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(klerk: Klerk<Ctx, Collections>) {

    val pathProvider = DefaultPathProvider(externalCssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css")

    val klerkWeb = KlerkWeb(
        klerk,
        ApplicationCall::ctx,
        pathProvider = pathProvider,
        classProvider = null,
        useTableForDetails = false
    )

    routing {
        get("/") { listGames(call, klerk, klerkWeb) }
        get("/game/{id}") { renderGame(call, klerk, klerkWeb) }
        post("/game/{id}") { confirmMove(call, klerk) }
        get("/sse/{id}") { handleSse(call, klerk) }

        // GraphQL
        graphQLPostRoute()
        graphQLGetRoute()
        graphiQLRoute()
        graphQLSDLRoute()

        // The auto-generated Admin UI
        apply(klerkWeb.generateRoutes())
    }
}

internal fun showOptionalParameters(event: EventReference) = false

/**
 * Creates a Context from a Call.
 * As authentication is something that should not be handled by Klerk, we will just fake it here.
 */
suspend fun ApplicationCall.ctx(klerk: Klerk<Ctx, Collections>): Ctx {
    val user = klerk.read(Ctx.system()) {
        getFirstWhere(views.users.all) { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}

/**
 * Creates a Context from a GraphQLContext. Used in the GraphQL API.
 *
 * In a real app we would use a session token or similar to figure out who the user is. Here, we always just use the
 * user Alice.
 */
suspend fun GraphQLContext.ctx(klerk: Klerk<Ctx, Collections>): Ctx {
    val user = klerk.read(Ctx.system()) {
        getFirstWhere(views.users.all) { it.props.name.valueWithoutAuthorization == "Alice" }
    }
    return Ctx.fromUser(user)
}

suspend fun canSeeAdminUI(ctx: Ctx): Boolean = true
