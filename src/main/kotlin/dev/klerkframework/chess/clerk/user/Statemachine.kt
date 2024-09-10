package dev.klerkframework.chess.klerk.user

import dev.klerkframework.chess.klerk.Collections
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.klerk.InstanceEventNoParameters
import dev.klerkframework.klerk.InstanceEventWithParameters
import dev.klerkframework.klerk.VoidEventWithParameters
import dev.klerkframework.klerk.statemachine.StateMachine
import dev.klerkframework.klerk.statemachine.stateMachine
import dev.klerkframework.chess.klerk.Score
import dev.klerkframework.chess.klerk.user.UserStates.*
import dev.klerkframework.klerk.ArgForInstanceEvent
import dev.klerkframework.klerk.ArgForVoidEvent

enum class UserStates {
    Created
}

fun createUserStateMachine(): StateMachine<User, Enum<*>, Ctx, Collections> =
    stateMachine {

        event(CreateUser) { }
        event(DeleteUser) { }
        event(UpdateScore) { }

        voidState {
            onEvent(CreateUser) {
                createModel(initialState = Created, ::createUser)
            }
        }

        state(Created) {
            onEvent(DeleteUser) {
                delete()
            }

            onEvent(UpdateScore) {
                update(::updateScore)
            }
        }

    }

object CreateUser : VoidEventWithParameters<User, CreateUserParams>(User::class, true, CreateUserParams::class)
object DeleteUser : InstanceEventNoParameters<User>(User::class, true)
object UpdateScore : InstanceEventWithParameters<User, UpdateScoreParams>(User::class, false, UpdateScoreParams::class)

fun createUser(args: ArgForVoidEvent<User, CreateUserParams, Ctx, Collections>): User {
    return User(name = args.command.params.name, score = Score(0))
}

fun updateScore(args: ArgForInstanceEvent<User, UpdateScoreParams, Ctx, Collections>): User {
    return args.model.props.copy(score = Score(args.model.props.score.int + args.command.params.delta.int))
}
