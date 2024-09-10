package dev.klerkframework.chess.html

import dev.klerkframework.chess.klerk
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.plugins.context
import dev.klerkframework.chess.plugins.contextFromCall
import dev.klerkframework.chess.plugins.showOptionalParameters
import dev.klerkframework.webutils.ButtonTargets
import dev.klerkframework.webutils.LowCodeConfig
import dev.klerkframework.webutils.LowCodeCreateEvent
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*

suspend fun listGames(call: ApplicationCall) {
    val context = call.context()
    klerk.readSuspend(context) {
        call.respondHtml {
            head {
                title = "Chess"
                styleLink("https://unpkg.com/sakura.css/css/sakura.css")
            }
            body {
                nav {
                    style = "text-align: right;"
                    a("/admin") {
                        style = "margin: 20px;"
                        +"Admin UI"
                    }
                    a("/graphiql") { +"GraphQL" }
                    hr()
                }

                h2 { +"Top scores" }
                table {
                    list(data.users.all).sortedByDescending { it.props.score.int }.forEach { user ->
                        tr {
                            td { +"${user.props.name}" }
                            td { +"${user.props.score}" }
                        }
                    }
                }

                h2 { +"Games" }
                ul {
                    list(data.games.all).forEach { game ->
                        li {
                            a(href = "/game/${game.id}") {
                                +"${get(game.props.whitePlayer).props.name} vs ${get(game.props.blackPlayer).props.name} (${game.state})"
                            }
                        }
                    }
                }

                h2 { +"Actions" }
                getPossibleVoidEvents(Game::class).forEach {
                    apply(LowCodeCreateEvent.renderButton(it, klerk, null, createLCConfig(), buttonTargets, context))
                    br()
                }
            }
        }
    }
}

val buttonTargets = ButtonTargets(back = "/", model = "/game/{id}", "/")

internal fun createLCConfig(): LowCodeConfig<Ctx> {
    return LowCodeConfig(
        basePath = "/admin",
        contextProvider = ::contextFromCall,
        showOptionalParameters = ::showOptionalParameters,
        cssPath = "https://unpkg.com/sakura.css/css/sakura.css"
    )
}
