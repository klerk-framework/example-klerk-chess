package dev.klerkframework.chess.klerk.user

import dev.klerkframework.chess.klerk.Views
import dev.klerkframework.chess.klerk.Ctx
import dev.klerkframework.klerk.InstanceEventNoParameters
import dev.klerkframework.klerk.InstanceEventWithParameters
import dev.klerkframework.klerk.VoidEventWithParameters
import dev.klerkframework.klerk.statemachine.StateMachine
import dev.klerkframework.klerk.statemachine.stateMachine
import dev.klerkframework.chess.klerk.Score
import dev.klerkframework.chess.klerk.user.UserStates.*
import dev.klerkframework.klerk.InstanceEventArgs
import dev.klerkframework.klerk.VoidEventArgs
import dev.klerkframework.klerk.EventVisibility.External

enum class UserStates {
    Created
}

fun createUserStateMachine(): StateMachine<User, Enum<*>, Ctx, Views> =
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

object CreateUser : VoidEventWithParameters<User, CreateUserParams>(External)
object DeleteUser : InstanceEventNoParameters<User>(External)
object UpdateScore : InstanceEventWithParameters<User, UpdateScoreParams>(External)

fun createUser(args: VoidEventArgs<User, CreateUserParams, Ctx, Views>): User {
    return User(name = args.command.params.name, score = Score(0))
}

fun updateScore(args: InstanceEventArgs<User, UpdateScoreParams, Ctx, Views>): User {
    return args.model.props.copy(score = Score(args.model.props.score.value + args.command.params.delta.value))
}
