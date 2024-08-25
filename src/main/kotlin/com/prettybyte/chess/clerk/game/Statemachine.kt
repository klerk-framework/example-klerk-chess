package com.prettybyte.chess.klerk.game

import com.prettybyte.chess.klerk.*
import com.prettybyte.chess.klerk.game.Board.Companion.fromMoves
import com.prettybyte.chess.klerk.game.GameState.*
import com.prettybyte.chess.klerk.user.UpdateScore
import com.prettybyte.chess.klerk.user.UpdateScoreParams
import com.prettybyte.klerk.*
import com.prettybyte.klerk.Validity.Invalid
import com.prettybyte.klerk.Validity.Valid
import com.prettybyte.klerk.command.Command
import com.prettybyte.klerk.statemachine.StateMachine
import com.prettybyte.klerk.statemachine.stateMachine
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.ZERO


import kotlin.time.Duration.Companion.minutes

enum class GameState {
    WaitingForInvitedPlayer,
    WhiteTurn,
    BlackTurn,
    WhiteVictory,
    BlackVictory,
    Draw,
    WhitePromotePawn,
    BlackPromotePawn,
    WhiteHasProposedDraw,
    BlackHasProposedDraw,
}

fun createGameStateMachine(collections: Collections): StateMachine<Game, Enum<*>, Ctx, Collections> =
    stateMachine {

        event(CreateGame) {
            validReferences(CreateGameParams::whitePlayer, collections.users.all)
            validReferences(CreateGameParams::blackPlayer, collections.users.all)
            validateWithParameters(::playerMustBeWhite)
            validateWithParameters(::cannotPlayAgainstSelf)
        }

        event(MakeMove) {
            validate(::onlyByCurrentPlayer)
            validateWithParameters(::isValidMove)
        }

        event(Resign) {
            validate(::onlyByCurrentPlayer)
        }

        event(ProposeDraw) {
            validate(::onlyByCurrentPlayer)
        }

        event(AcceptDraw) {
            validate(::onlyByNotCurrentPlayer)
        }

        event(DeclineDraw) {
            validate(::onlyByNotCurrentPlayer)
        }

        event(PromotePawn) {
            validate(::onlyByCurrentPlayer)
            validateWithParameters(::isPromotablePiece)
        }

        event(AcceptInvite) {
            validate(::onlyByBlackPlayer)
        }

        event(DeclineInvite) {
            validate(::onlyByBlackPlayer)
        }


        voidState {
            onEvent(CreateGame) {
                createModel(initialState = WaitingForInvitedPlayer, ::newGame)
            }
        }

        state(WaitingForInvitedPlayer) {
            onEvent(AcceptInvite) {
                transitionTo(WhiteTurn)
            }

            onEvent(DeclineInvite) {
                delete()
                action(::tellUserAboutDecline)
            }
        }

        state(WhiteTurn) {
            onEnter {
                transitionWhen(
                    linkedMapOf(
                        ::blackCanPromotePawn to BlackPromotePawn,
                        ::currentPlayerIsCheckmate to BlackVictory,
                        IsAutomaticDraw::execute to Draw
                    )
                )
            }

            onEvent(MakeMove) {
                transitionTo(BlackTurn)
                update(::makeMove)
            }

            onEvent(ProposeDraw) {
                transitionTo(WhiteHasProposedDraw)
            }

            onEvent(Resign) {
                transitionTo(BlackVictory)
            }

            atTime(::remainingPlayTime) {
                transitionTo(BlackVictory)
            }

            onExit {
                update(::updatePlayerTime)
            }
        }

        state(BlackTurn) {
            onEnter {
                transitionWhen(
                    linkedMapOf(
                        ::whiteCanPromotePawn to WhitePromotePawn,
                        ::currentPlayerIsCheckmate to WhiteVictory,
                        IsAutomaticDraw::execute to Draw
                    )
                )
            }

            onEvent(MakeMove) {
                transitionTo(WhiteTurn)
                update(::makeMove)
            }

            onEvent(ProposeDraw) {
                transitionTo(BlackHasProposedDraw)
            }

            onEvent(Resign) {
                transitionTo(WhiteVictory)
            }

            atTime(::remainingPlayTime) {
                transitionTo(WhiteVictory)
            }

            onExit {
                update(::updatePlayerTime)
            }
        }

        state(WhitePromotePawn) {
            onEvent(PromotePawn) {
                update(::promotePawn)
                transitionTo(BlackTurn)
            }

            onExit {
                update(::updatePlayerTime)
            }
        }

        state(BlackPromotePawn) {
            onEvent(PromotePawn) {
                update(::promotePawn)
                transitionTo(WhiteTurn)
            }

            onExit {
                update(::updatePlayerTime)
            }
        }

        state(WhiteVictory) {
            onEnter {
                createCommands(::updatePlayersRatings)
            }
        }

        state(BlackVictory) {
            onEnter {
                createCommands(::updatePlayersRatings)
            }
        }

        state(Draw) {
            onEnter {
                createCommands(::updatePlayersRatings)
            }
        }

        state(WhiteHasProposedDraw) {
            onEvent(AcceptDraw) {
                transitionTo(Draw)
            }

            onEvent(DeclineDraw) {
                transitionTo(WhiteTurn)
            }
        }

        state(BlackHasProposedDraw) {
            onEvent(AcceptDraw) {
                transitionTo(Draw)
            }

            onEvent(DeclineDraw) {
                transitionTo(BlackTurn)
            }
        }

    }

object CreateGame : VoidEventWithParameters<Game, CreateGameParams>(Game::class, true, CreateGameParams::class)

object AcceptInvite : InstanceEventNoParameters<Game>(Game::class, true)

object DeclineInvite : InstanceEventNoParameters<Game>(Game::class, true)

object MakeMove : InstanceEventWithParameters<Game, MakeMoveParams>(Game::class, true, MakeMoveParams::class)

object ProposeDraw : InstanceEventNoParameters<Game>(Game::class, true)

object Resign : InstanceEventNoParameters<Game>(Game::class, true)

object AcceptDraw : InstanceEventNoParameters<Game>(Game::class, true)

object DeclineDraw : InstanceEventNoParameters<Game>(Game::class, true)

object PromotePawn : InstanceEventWithParameters<Game, PromotePawnParams>(Game::class, true, PromotePawnParams::class)

private val whitePlayerStates = setOf(WhiteTurn.name, WhiteHasProposedDraw.name, WhitePromotePawn.name)
private val blackPlayerStates = setOf(BlackTurn.name, BlackHasProposedDraw.name, BlackPromotePawn.name)

fun isValidMove(args: ArgForInstanceEvent<Game, MakeMoveParams, Ctx, Collections>): Validity {
    val move = CoordinateNotationMove.move(args.command.params.from, args.command.params.to)
    val validMoves = calculateAllValidMoves(fromMoves(args.model.props.moves), valueOf(args.model.state))
    return if (validMoves.contains(move)) Valid else Invalid("Illegal move")
}

fun makeMove(args: ArgForInstanceEvent<Game, MakeMoveParams, Ctx, Collections>): Game {
    val moveInCoordinateNotation = CoordinateNotationMove.move(
        from = Position.fromString(args.command.params.from.string),
        to = Position.fromString(args.command.params.to.string)
    )
    return args.model.props.copy(moves = args.model.props.moves.plus(moveInCoordinateNotation))
}

fun newGame(args: ArgForVoidEvent<Game, CreateGameParams, Ctx, Collections>): Game {
    return Game(
        whitePlayer = args.command.params.whitePlayer,
        blackPlayer = args.command.params.blackPlayer,
        emptyList(),
        whitePlayerTime = PlayTime(ZERO),
        blackPlayerTime = PlayTime(ZERO),
    )
}

fun tellUserAboutDecline(args: ArgForInstanceEvent<Game, Nothing?, Ctx, Collections>) {
    println("Let's pretend we send an email")
}

fun currentPlayerIsCheckmate(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean {
    val board = fromMoves(args.model.props.moves)
    if (args.model.state == WhiteTurn.name) {
        return isWhiteCheck(board) &&
                calculateAllValidMoves(board, WhiteTurn, true).isEmpty()
    }
    if (args.model.state == BlackTurn.name) {
        return isBlackCheck(board) &&
                calculateAllValidMoves(board, BlackTurn, true).isEmpty()
    }
    throw IllegalArgumentException()
}

fun blackCanPromotePawn(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean =
    fromMoves(args.model.props.moves).canPromotePawn(Color.Black)

fun whiteCanPromotePawn(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean =
    fromMoves(args.model.props.moves).canPromotePawn(Color.White)

fun isPromotablePiece(args: ArgForInstanceEvent<Game, PromotePawnParams, Ctx, Collections>): Validity {
    val piece = args.command.params.piece.string
    if (piece.length != 1) {
        return Invalid("Illegal piece")
    }
    return if ("nqrb".contains(piece)) Valid else Invalid("Illegal piece")
}

fun promotePawn(args: ArgForInstanceEvent<Game, PromotePawnParams, Ctx, Collections>): Game {
    val updatedLastMove = args.model.props.moves.last().withPromotedPawn(args.command.params.piece)
    val updatedMoves = args.model.props.moves.dropLast(1).plus(updatedLastMove)
    return args.model.props.copy(moves = updatedMoves)
}

fun automaticDraw(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Boolean =
    isStalemate(args) || isDeadPosition(args) || isFivefoldRepetition()

fun updatePlayersRatings(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): List<Command<out Any, out Any>> {
    val whitePoints = when (args.model.state) {
        WhiteVictory.name -> 2
        Draw.name -> 1
        else -> 0
    }
    val blackPoints = when (args.model.state) {
        BlackVictory.name -> 2
        Draw.name -> 1
        else -> 0
    }
    return listOf(
        Command(UpdateScore, args.model.props.whitePlayer, UpdateScoreParams(Score(whitePoints))),
        Command(UpdateScore, args.model.props.blackPlayer, UpdateScoreParams(Score(blackPoints))),
    )
}

fun onlyByCurrentPlayer(args: ArgForInstanceEvent<Game, Nothing?, Ctx, Collections>): Validity {
    val user = args.context.user ?: return Invalid("Must be logged in")
    if (whitePlayerStates.contains(args.model.state)) {
        return if (user.id == args.model.props.whitePlayer) Valid else Invalid("Wrong player")
    }
    if (blackPlayerStates.contains(args.model.state)) {
        return if (user.id == args.model.props.blackPlayer) Valid else Invalid("Wrong player")
    }
    throw IllegalArgumentException()
}

fun onlyByNotCurrentPlayer(args: ArgForInstanceEvent<Game, Nothing?, Ctx, Collections>): Validity {
    val user = args.context.user ?: return Invalid("Must be logged in")
    if (blackPlayerStates.contains(args.model.state)) {
        return if (user.id == args.model.props.whitePlayer) Valid else Invalid("Wrong player")
    }
    if (whitePlayerStates.contains(args.model.state)) {
        return if (user.id == args.model.props.blackPlayer) Valid else Invalid("Wrong player")
    }
    throw IllegalArgumentException()
}

fun cannotPlayAgainstSelf(args: ArgForVoidEvent<Game, CreateGameParams, Ctx, Collections>): Validity {
    return if (args.command.params.whitePlayer == args.command.params.blackPlayer) Invalid("Players must be different") else Valid
}

fun onlyByBlackPlayer(args: ArgForInstanceEvent<Game, Nothing?, Ctx, Collections>): Validity {
    val user = args.context.user ?: return Invalid("Must be logged in")
    return if (args.model.props.blackPlayer == user.id) Valid else Invalid()
}

fun playerMustBeWhite(args: ArgForVoidEvent<Game, CreateGameParams, Ctx, Collections>): Validity {
    val user = args.context.user ?: return Invalid("Must be logged in")
    return if (args.command.params.whitePlayer == user.id) Valid else Invalid("You must play white")
}

fun updatePlayerTime(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Game {
    val delta = args.time.minus(args.model.lastStateTransitionAt)
    return if (whitePlayerStates.contains(args.model.state)) {
        args.model.props.copy(whitePlayerTime = PlayTime(args.model.props.whitePlayerTime.duration + delta))
    } else {
        args.model.props.copy(blackPlayerTime = PlayTime(args.model.props.blackPlayerTime.duration + delta))
    }
}

fun remainingPlayTime(args: ArgForInstanceNonEvent<Game, Ctx, Collections>): Instant {
    val playTime = if (whitePlayerStates.contains(args.model.state)) args.model.props.whitePlayerTime else args.model.props.blackPlayerTime
    val remainingTime = 1.minutes.minus(playTime.duration)
    return args.time.plus(remainingTime)
}
