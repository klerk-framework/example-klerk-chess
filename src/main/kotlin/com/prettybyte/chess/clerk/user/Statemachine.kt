package com.prettybyte.chess.klerk.user

import com.prettybyte.chess.klerk.Collections
import com.prettybyte.chess.klerk.Ctx
import com.prettybyte.klerk.InstanceEventNoParameters
import com.prettybyte.klerk.InstanceEventWithParameters
import com.prettybyte.klerk.VoidEventWithParameters
import com.prettybyte.klerk.statemachine.StateMachine
import com.prettybyte.klerk.statemachine.stateMachine
import com.prettybyte.chess.klerk.Score
import com.prettybyte.chess.klerk.user.UserStates.*
import com.prettybyte.klerk.ArgForInstanceEvent
import com.prettybyte.klerk.ArgForVoidEvent

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
