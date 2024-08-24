package com.prettybyte.chess.html

import com.prettybyte.chess.klerk
import com.prettybyte.chess.klerk.CoordinateNotationMove
import com.prettybyte.chess.klerk.Position
import com.prettybyte.chess.klerk.UserName
import com.prettybyte.chess.klerk.game.Board
import com.prettybyte.chess.klerk.game.Game
import com.prettybyte.chess.klerk.game.MakeMove
import com.prettybyte.chess.klerk.game.MakeMoveParams
import com.prettybyte.chess.plugins.context
import com.prettybyte.klerk.*
import com.prettybyte.klerk.command.Command
import com.prettybyte.klerk.command.ProcessingOptions
import com.prettybyte.klerk.command.CommandToken
import com.prettybyte.webutils.LowCodeCreateEvent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.html.*

private data class RenderGameData(
    val game: Model<Game>,
    val whiteName: UserName,
    val blackName: UserName,
    val possibleEvents: Set<EventReference>
)

suspend fun renderGame(call: ApplicationCall) {
    val context = call.context()
    val gameId = ModelID.from<Game>(requireNotNull(call.parameters["id"]))
    val dryRunMove = moveInQueryParameters(call)
    val dryRunResult = if (dryRunMove == null) {
        null
    } else {
        klerk.handle(
            Command(MakeMove, gameId, MakeMoveParams(dryRunMove.from, dryRunMove.to)),
            context,
            ProcessingOptions(CommandToken.simple(), dryRun = true)
        )
    }

    val renderGameData = klerk.read(context) {
        val game = if (dryRunResult == null) get(gameId) else {
            @Suppress("UNCHECKED_CAST")
            when (dryRunResult) {
                is CommandResult.Failure -> get(gameId)
                is CommandResult.Success -> dryRunResult.authorizedModels[gameId] as Model<Game>
            }
        }
        val possibleEvents = getPossibleEvents(gameId)
        RenderGameData(
            game = game,
            whiteName = get(game.props.whitePlayer).props.name,
            blackName = get(game.props.blackPlayer).props.name,
            possibleEvents
        )
    }

    call.respondHtml {
        head {
            title = "Chess"
            styleLink("https://unpkg.com/sakura.css/css/sakura.css")
        }
        body {
            style = "max-width: 120em;"
            nav {
                a("/") { +"Home" }
            }
            h1 { +"${renderGameData.whiteName} (white) vs ${renderGameData.blackName} (black)" }

            // state
            p {
                val maybeArrow = if (dryRunResult is CommandResult.Success) "➔" else ""
                +"State: $maybeArrow ${renderGameData.game.state}"
            }

            p {
                +"Player times: White: ${renderGameData.game.props.whitePlayerTime}, Black: ${renderGameData.game.props.blackPlayerTime}"
            }

            div {
                style = "display: grid; grid-template-columns: 800px 200px 200px;"

                // board
                div(classes = "chessboard") {
                    unsafe {
                        +renderSquares(
                            Board.fromMoves(renderGameData.game.props.moves),
                            renderGameData.possibleEvents.contains(MakeMove.id)
                        )
                    }
                }

                // actions
                div {
                    h3 { +"Actions" }
                    renderGameData.possibleEvents
                        .filter { it != MakeMove.id }   // we handle MakeMove separately
                        .forEach {
                            apply(LowCodeCreateEvent.renderButton(it, klerk, renderGameData.game.id, createLCConfig(), buttonTargets, context))
                            br()
                        }

                    if (dryRunMove != null && dryRunResult is CommandResult.Success) {
                        form(action = "/game/${gameId}", method = FormMethod.post) {
                            hiddenInput {
                                name = "move"
                                value = dryRunMove.string
                            }
                            submitInput { value = "Make move ${dryRunMove.string}" }
                        }

                        form(action = "/game/${gameId}", method = FormMethod.get) {
                            submitInput { value = "Cancel move ${dryRunMove.string}" }
                        }
                    }
                }

                // moves
                div {
                    h3 { +"Moves" }
                    table {
                        renderGameData.game.props.moves.chunked(2).forEach { chunk ->
                            tr {
                                td { +chunk.first().toString() }
                                chunk.getOrNull(1)?.let {
                                    td { +it.toString() }
                                }
                            }
                        }
                    }
                }
            }

            div {
                id = "events"
            }

            unsafe { +includeStyle() }
            unsafe { +includeJS(renderGameData.game.id) }
        }
    }
}

private fun moveInQueryParameters(call: ApplicationCall): CoordinateNotationMove? {
    val moveString = call.request.queryParameters["dryrunmove"] ?: return null
    return CoordinateNotationMove(moveString)
}

private fun includeJS(id: ModelID<Game>) = """
<script>
var source = new EventSource('/sse/${id}');
                                    var eventsUl = document.getElementById('events');
                                    function logEvent(text) {
                                        console.log(text);
                                    }
                                    source.addEventListener('message', function(e) {
                                        logEvent('message:' + e.data);
                                        if (e.data == 'reload') {
                                            window.location.reload(true);
                                        }
                                    }, false);
                                    source.addEventListener('open', function(e) {
                                        logEvent('open');
                                    }, false);
                                    source.addEventListener('error', function(e) {
                                        if (e.readyState == EventSource.CLOSED) {
                                            logEvent('closed');
                                        } else {
                                            logEvent('error');
                                            console.log(e);
                                        }
                                    }, false);
                                    
                                    

function drag(ev) {
  ev.dataTransfer.setData("square", ev.target.id);
}

function allowDrop(ev) {
  ev.preventDefault();
}

async function drop(ev) {
  ev.preventDefault();
  var from = ev.dataTransfer.getData("square");
  var to = ev.target.id;
  console.log("from:" + from +  " to:" + ev.target.id);
  
  window.location.replace("/game/$id?dryrunmove="+from +"-"+to);
  
  /*
  const response = await fetch("/execute-event-json", {
method: 'POST',
headers: {
  'Accept': 'application/json',
  'Content-Type': 'application/json'
},
body: `{
   "eventname": "Make move",
   "modelid": "${id}",
   "from": "` + from + `",
   "to": "` + to + `"
  }`,
});


response.text().then(data => {
  console.log(data);
  var li = document.createElement('li')
  li.innerText = data;
  eventsUl.appendChild(li);
});
*/

}

</script>
"""

private fun renderSquares(board: Board, canMakeMove: Boolean): String {
    val columns = "abcdefgh"
    val pieces = board.pieces
    var result = ""
    var startLineWithWhiteColor = true
    var index = 63
    for (row in 8.downTo(1)) {
        for (c in 1..8) {
            val color =
                if (startLineWithWhiteColor && (c % 2 == 0) || !startLineWithWhiteColor && (c % 2 != 0)) "white" else "black"
            val position = Position.from(columns[c - 1], row)
            val piece = getPiece(position, pieces)
            result += """<div id="${position}" class="$color" draggable="${if (canMakeMove && piece.isNotEmpty()) "true" else "false"}" ondragstart="drag(event)" ondrop="drop(event)" ondragover="allowDrop(event)" >$piece</div>"""
            index--
        }
        startLineWithWhiteColor = !startLineWithWhiteColor
    }
    return result
}

private fun getPiece(square: Position, board: List<String>): String {
    val p = board[square.toCartesian()];
    if (p.isEmpty()) {
        return ""
    }
    var piece = ""
    if (p.startsWith("w")) {
        piece = toUnicode(true, p);
    }
    if (board[square.toCartesian()].startsWith("b")) {
        piece = toUnicode(false, p)
    }
    return piece
}

private fun toUnicode(isWhite: Boolean, piece: String): String {
    if (piece.isEmpty()) {
        return ""
    }
    when (piece.substring(1, 2)) {
        "p" -> return if (isWhite) "♙" else "♟"
        "r" -> return if (isWhite) "♖" else "♜"
        "n" -> return if (isWhite) "♘" else "♞"
        "b" -> return if (isWhite) "♗" else "♝"
        "q" -> return if (isWhite) "♕" else "♛"
        "k" -> return if (isWhite) "♔" else "♚"
    }
    throw Error()
}

private fun includeStyle() = """
    <style type="text/css">

    .chessboard {
        width: 640px;
        height: 640px;
        margin: 20px;
        border: 25px solid #333;
    }
        .black {
            float: left;
            width: 80px;
            height: 80px;
            background-color: #bbb;
            font-size:50px;
            text-align:center;
            display: table-cell;
            vertical-align:middle;
        }
        .white {
            float: left;
            width: 80px;
            height: 80px;
            background-color: #fff;
            font-size:50px;
            text-align:center;
            display: table-cell;
            vertical-align:middle;
        }

    </style>"""

suspend fun confirmMove(call: ApplicationCall) {
    val gameId = ModelID.from<Game>(requireNotNull(call.parameters["id"]))
    val moveString = call.receiveParameters()["move"]
    moveString?.let {
        val move = CoordinateNotationMove(it)
        klerk.handle(
            Command(MakeMove, gameId, MakeMoveParams(move.from, move.to)),
            call.context(),
            ProcessingOptions(CommandToken.simple())
        )

    }
    call.respondRedirect("/game/${gameId}")
}

suspend fun handleSse(call: ApplicationCall) {
    //There is better support for SSE in ktor 3
    val id = ModelID.from<Any>(requireNotNull(call.parameters["id"]))
    val events = klerk.models.subscribe(call.context(), id)
    call.response.cacheControl(CacheControl.NoCache(null))
    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        events.collect {
            write("data: reload\n\n")
            flush()
        }
        throw IllegalStateException("We should never reach this")
    }
}
