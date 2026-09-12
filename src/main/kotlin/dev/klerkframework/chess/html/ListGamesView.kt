package dev.klerkframework.chess.html

import dev.klerkframework.chess.klerk.Collections
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.plugins.ctx
import dev.klerkframework.klerk.collection.asSequence
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.web.KlerkWeb
import dev.klerkframework.web.eventButton
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*

suspend fun listGames(call: ApplicationCall, klerk: Klerk<Ctx, Collections>, klerkWeb: KlerkWeb<Ctx, Collections>) {
    val context = call.ctx(klerk)
    klerk.readSuspend(call::ctx) {
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
                    views.users.all.asSequence().sortedByDescending { it.props.score.int }.forEach { user ->
                        tr {
                            td { +"${user.props.name}" }
                            td { +"${user.props.score}" }
                        }
                    }
                }

                h2 { +"Games" }
                ul {
                    views.games.all.asSequence().forEach { game ->
                        li {
                            a(href = "/game/${game.id}") {
                                +"${get(game.props.whitePlayer).props.name} vs ${get(game.props.blackPlayer).props.name} (${game.state})"
                            }
                        }
                    }
                }

                h2 { +"Actions" }
                with(klerkWeb.support) {
                    getPossibleVoidEvents(Game::class).forEach {
                        eventButton(
                            it, null, context,
                            onCancelPath = "/",
                            onSuccessAndModelExistPath = "/game/{id}",
                            onErrorPath = "/"
                        )
                        br()
                    }
                }
            }
        }
    }
}
