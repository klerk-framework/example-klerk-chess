package dev.klerkframework.chess.klerk

import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.NegativeAuthorization.Deny
import dev.klerkframework.klerk.NegativeAuthorization.Pass
import dev.klerkframework.klerk.PositiveAuthorization.Allow
import dev.klerkframework.klerk.PositiveAuthorization.NoOpinion

fun createAuthorizationRules(): ConfigBuilder<Ctx, Collections>.() -> Unit = {
    authorization {
        commands {
            positive {
                rule(::authenticatedCanDoEverything)
            }
            negative {
            }
        }
        readModels {
            positive {
                rule(::everybodyCanReadGames)
                rule(::canReadTheirOwnUserAndMrRobot)
            }
            negative {
            }
        }
        readProperties {
            positive {
                rule(::everybodyCanReadAnyProperty)
            }
            negative {
                rule(::canOnlySeeMovesInGamesWhichYouAreInvolvedIn)
            }
        }
        eventLog {
            positive {
                rule(::aliceCanReadTheEventLog)
            }
            negative {
                rule(::mustBeAuthenticatedToReadTheEventLog)
            }
        }
    }
}

fun canReadTheirOwnUserAndMrRobot(args: ArgModelContextReader<Ctx, Collections>): PositiveAuthorization {
    val p = (args.model.props as? User) ?: return NoOpinion
    if (p.name.string == "Mr. Robot") {
        return Allow
    }
    return if (p.name.string == args.context.user?.props?.name?.string) Allow else NoOpinion
}

fun everybodyCanReadGames(args: ArgModelContextReader<Ctx, Collections>): PositiveAuthorization {
    return if (args.model.props is Game) Allow else NoOpinion
}

fun authenticatedCanDoEverything(args: ArgCommandContextReader<*, Ctx, Collections>): PositiveAuthorization {
    return if (args.context.actor is Unauthenticated) NoOpinion else Allow
}

fun everybodyCanReadAnyProperty(args: ArgsForPropertyAuth<Ctx, Collections>): PositiveAuthorization {
    return Allow
}

fun aliceCanReadTheEventLog(args: ArgContextReader<Ctx, Collections>): PositiveAuthorization {
    return if (args.context.user?.props?.name?.string == "Alice") Allow else NoOpinion
}

fun mustBeAuthenticatedToReadTheEventLog(args: ArgContextReader<Ctx, Collections>): NegativeAuthorization {
    return if (args.context.actor is Unauthenticated) Deny else Pass
}

fun canOnlySeeMovesInGamesWhichYouAreInvolvedIn(args: ArgsForPropertyAuth<Ctx, Collections>): NegativeAuthorization {
    val p = (args.model.props as? Game)?: return Pass
    return if ((setOf(p.whitePlayer, p.blackPlayer).contains(args.context.user?.id))) Pass else Deny
}
