package dev.klerkframework.chess.klerk.game

import dev.klerkframework.chess.klerk.*
import dev.klerkframework.chess.klerk.game.Board.Companion.fromMoves
import dev.klerkframework.chess.klerk.game.GameState.*
import dev.klerkframework.chess.klerk.user.UpdateScore
import dev.klerkframework.chess.klerk.user.UpdateScoreParams
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.EventVisibility.External
import dev.klerkframework.klerk.PropertyCollectionValidity.*
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.statemachine.StateMachine
import dev.klerkframework.klerk.statemachine.stateMachine
import kotlin.time.Duration.Companion.ZERO


import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

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

fun createGameStateMachine(views: Views): StateMachine<Game, Enum<*>, Ctx, Views> =
    stateMachine {

        event(CreateGame) {
            validReferences(CreateGameParams::whitePlayer, views.users.all)
            validReferences(CreateGameParams::blackPlayer, views.users.all)
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
                unmanagedJob(::tellUserAboutDecline)
            }
        }

        state(WhiteTurn) {
            onEnter {
                transitionWhen {
                    on(::blackCanPromotePawn, BlackPromotePawn)
                    on(::currentPlayerIsCheckmate, BlackVictory)
                    on(IsAutomaticDraw::execute, Draw)
                }
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
                transitionWhen {
                    on(::whiteCanPromotePawn, WhitePromotePawn)
                    on(::currentPlayerIsCheckmate, WhiteVictory)
                    on(IsAutomaticDraw::execute, Draw)
                }
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
                commands(::updatePlayersRatings)
            }
        }

        state(BlackVictory) {
            onEnter {
                commands(::updatePlayersRatings)
            }
        }

        state(Draw) {
            onEnter {
                commands(::updatePlayersRatings)
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

object CreateGame : VoidEventWithParameters<Game, CreateGameParams>(External)

object AcceptInvite : InstanceEventNoParameters<Game>(External)

object DeclineInvite : InstanceEventNoParameters<Game>(External)

object MakeMove : InstanceEventWithParameters<Game, MakeMoveParams>(External)

object ProposeDraw : InstanceEventNoParameters<Game>(External)

object Resign : InstanceEventNoParameters<Game>(External)

object AcceptDraw : InstanceEventNoParameters<Game>(External)

object DeclineDraw : InstanceEventNoParameters<Game>(External)

object PromotePawn : InstanceEventWithParameters<Game, PromotePawnParams>(External)

private val whitePlayerStates = setOf(WhiteTurn, WhiteHasProposedDraw, WhitePromotePawn)
private val blackPlayerStates = setOf(BlackTurn, BlackHasProposedDraw, BlackPromotePawn)

fun isValidMove(args: InstanceEventArgs<Game, MakeMoveParams, Ctx, Views>): PropertyCollectionValidity {
    val move = CoordinateNotationMove.move(args.command.params.from, args.command.params.to)
    val validMoves = calculateAllValidMoves(fromMoves(args.model.props.moves), args.model.stateAs<GameState>())
    return if (validMoves.contains(move)) Valid else Invalid("Illegal move")
}

fun makeMove(args: InstanceEventArgs<Game, MakeMoveParams, Ctx, Views>): Game {
    val moveInCoordinateNotation = CoordinateNotationMove.move(
        from = Position.fromString(args.command.params.from.value),
        to = Position.fromString(args.command.params.to.value)
    )
    return args.model.props.copy(moves = args.model.props.moves.plus(moveInCoordinateNotation))
}

fun newGame(args: VoidEventArgs<Game, CreateGameParams, Ctx, Views>): Game {
    return Game(
        whitePlayer = args.command.params.whitePlayer,
        blackPlayer = args.command.params.blackPlayer,
        emptyList(),
        whitePlayerTime = PlayTime(ZERO),
        blackPlayerTime = PlayTime(ZERO),
    )
}

fun tellUserAboutDecline(args: InstanceEventArgs<Game, Nothing?, Ctx, Views>) {
    println("Let's pretend we send an email")
}

fun currentPlayerIsCheckmate(args: LifecycleArgs<Game, Ctx, Views>): Boolean {
    val board = fromMoves(args.model.props.moves)
    if (args.model.isIn(WhiteTurn)) {
        return isWhiteCheck(board) &&
                calculateAllValidMoves(board, WhiteTurn, true).isEmpty()
    }
    if (args.model.isIn(BlackTurn)) {
        return isBlackCheck(board) &&
                calculateAllValidMoves(board, BlackTurn, true).isEmpty()
    }
    throw IllegalArgumentException()
}

fun blackCanPromotePawn(args: LifecycleArgs<Game, Ctx, Views>): Boolean =
    fromMoves(args.model.props.moves).canPromotePawn(Color.Black)

fun whiteCanPromotePawn(args: LifecycleArgs<Game, Ctx, Views>): Boolean =
    fromMoves(args.model.props.moves).canPromotePawn(Color.White)

fun isPromotablePiece(args: InstanceEventArgs<Game, PromotePawnParams, Ctx, Views>): PropertyCollectionValidity {
    val piece = args.command.params.piece.value
    if (piece.length != 1) {
        return Invalid("Illegal piece")
    }
    return if ("nqrb".contains(piece)) Valid else Invalid("Illegal piece")
}

fun promotePawn(args: InstanceEventArgs<Game, PromotePawnParams, Ctx, Views>): Game {
    val updatedLastMove = args.model.props.moves.last().withPromotedPawn(args.command.params.piece)
    val updatedMoves = args.model.props.moves.dropLast(1).plus(updatedLastMove)
    return args.model.props.copy(moves = updatedMoves)
}

fun automaticDraw(args: LifecycleArgs<Game, Ctx, Views>): Boolean =
    isStalemate(args) || isDeadPosition(args) || isFivefoldRepetition()

fun updatePlayersRatings(args: LifecycleArgs<Game, Ctx, Views>): List<Command<out Any, out Any>> {
    val whitePoints = when (args.model.stateAs<GameState>()) {
        WhiteVictory -> 2
        Draw -> 1
        else -> 0
    }
    val blackPoints = when (args.model.stateAs<GameState>()) {
        BlackVictory -> 2
        Draw -> 1
        else -> 0
    }
    return listOf(
        Command(UpdateScore, args.model.props.whitePlayer, UpdateScoreParams(Score(whitePoints))),
        Command(UpdateScore, args.model.props.blackPlayer, UpdateScoreParams(Score(blackPoints))),
    )
}

fun onlyByCurrentPlayer(args: InstanceEventArgs<Game, Nothing?, Ctx, Views>): PropertyCollectionValidity {
    val userId = args.context.userId ?: return Invalid("Must be logged in")
    if (args.model.isIn(whitePlayerStates)) {
        return if (userId == args.model.props.whitePlayer) Valid else Invalid("Wrong player")
    }
    if (args.model.isIn(blackPlayerStates)) {
        return if (userId == args.model.props.blackPlayer) Valid else Invalid("Wrong player")
    }
    throw IllegalArgumentException()
}

fun onlyByNotCurrentPlayer(args: InstanceEventArgs<Game, Nothing?, Ctx, Views>): PropertyCollectionValidity {
    val userId = args.context.userId ?: return Invalid("Must be logged in")
    if (args.model.isIn(blackPlayerStates)) {
        return if (userId == args.model.props.whitePlayer) Valid else Invalid("Wrong player")
    }
    if (args.model.isIn(whitePlayerStates)) {
        return if (userId == args.model.props.blackPlayer) Valid else Invalid("Wrong player")
    }
    throw IllegalArgumentException()
}

fun cannotPlayAgainstSelf(args: VoidEventArgs<Game, CreateGameParams, Ctx, Views>): PropertyCollectionValidity {
    return if (args.command.params.whitePlayer == args.command.params.blackPlayer) Invalid("Players must be different") else Valid
}

fun onlyByBlackPlayer(args: InstanceEventArgs<Game, Nothing?, Ctx, Views>): PropertyCollectionValidity {
    val userId = args.context.userId ?: return Invalid("Must be logged in")
    return if (args.model.props.blackPlayer == userId) Valid else Invalid()
}

fun playerMustBeWhite(args: VoidEventArgs<Game, CreateGameParams, Ctx, Views>): PropertyCollectionValidity {
    val user = args.context.user ?: return Invalid("Must be logged in")
    return if (args.command.params.whitePlayer == user.id) Valid else Invalid("You must play white")
}

fun updatePlayerTime(args: LifecycleArgs<Game, Ctx, Views>): Game {
    val delta = args.time.minus(args.model.lastStateTransitionAt)
    return if (args.model.isIn(whitePlayerStates)) {
        args.model.props.copy(whitePlayerTime = PlayTime(args.model.props.whitePlayerTime.value + delta))
    } else {
        args.model.props.copy(blackPlayerTime = PlayTime(args.model.props.blackPlayerTime.value + delta))
    }
}

fun remainingPlayTime(args: LifecycleArgs<Game, Ctx, Views>): Instant {
    val playTime = if (args.model.isIn(whitePlayerStates)) args.model.props.whitePlayerTime else args.model.props.blackPlayerTime
    val remainingTime = 5.minutes.minus(playTime.value)
    return args.time.plus(remainingTime)
}
