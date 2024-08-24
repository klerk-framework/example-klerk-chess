package com.prettybyte.chess.html

import com.prettybyte.chess.klerk
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.chess.klerk.game.Game
import com.prettybyte.chess.plugins.context
import com.prettybyte.chess.plugins.contextFromCall
import com.prettybyte.chess.plugins.showOptionalParameters
import com.prettybyte.webutils.ButtonTargets
import com.prettybyte.webutils.LowCodeConfig
import com.prettybyte.webutils.LowCodeCreateEvent
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
