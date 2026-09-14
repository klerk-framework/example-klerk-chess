package dev.klerkframework.chess.klerk

import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.NegativeAuthorization.Deny
import dev.klerkframework.klerk.NegativeAuthorization.Pass
import dev.klerkframework.klerk.PositiveAuthorization.Allow
import dev.klerkframework.klerk.PositiveAuthorization.NoOpinion

fun createAuthorizationRules(): SpecificationBuilder<Ctx, Views>.() -> Unit = {
    authorization {
        commands {
            positive(::authenticatedCanDoEverything)
        }
        readModels {
            positive(::everybodyCanReadGames, ::canReadTheirOwnUserAndMrRobot)
        }
        readProperties {
            positive(::everybodyCanReadAnyProperty)
            negative(::canOnlySeeMovesInGamesWhichYouAreInvolvedIn)
        }
        eventLog {
            positive(::aliceCanReadTheEventLog)
            negative(::mustBeAuthenticatedToReadTheEventLog)
        }
    }
}

fun canReadTheirOwnUserAndMrRobot(args: ModelReadRuleArgs<Ctx, Views>): PositiveAuthorization {
    val p = (args.model.props as? User) ?: return NoOpinion
    if (p.name.value == "Mr. Robot") {
        return Allow
    }
    return if (p.name.value == args.context.user?.props?.name?.value) Allow else NoOpinion
}

fun everybodyCanReadGames(args: ModelReadRuleArgs<Ctx, Views>): PositiveAuthorization {
    return if (args.model.props is Game) Allow else NoOpinion
}

fun authenticatedCanDoEverything(args: CommandRuleArgs<*, Ctx, Views>): PositiveAuthorization {
    return if (args.context.actor is Unauthenticated) NoOpinion else Allow
}

fun everybodyCanReadAnyProperty(args: PropertyReadRuleArgs<Ctx, Views>): PositiveAuthorization {
    return Allow
}

fun aliceCanReadTheEventLog(args: EventLogRuleArgs<Ctx, Views>): PositiveAuthorization {
    return if (args.context.user?.props?.name?.value == "Alice") Allow else NoOpinion
}

fun mustBeAuthenticatedToReadTheEventLog(args: EventLogRuleArgs<Ctx, Views>): NegativeAuthorization {
    return if (args.context.actor is Unauthenticated) Deny else Pass
}

fun canOnlySeeMovesInGamesWhichYouAreInvolvedIn(args: PropertyReadRuleArgs<Ctx, Views>): NegativeAuthorization {
    val p = (args.model.props as? Game)?: return Pass
    return if ((setOf(p.whitePlayer, p.blackPlayer).contains(args.context.user?.id))) Pass else Deny
}
