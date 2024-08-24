package com.prettybyte.chess.klerk

import com.prettybyte.klerk.*
import com.prettybyte.klerk.PositiveAuthorization.Allow

fun createAuthorizationRules(): ConfigBuilder<Ctx, Collections>.() -> Unit = {
    authorization {
        commands {
            positive {
                rule(::everybodyAreAllowedToDoEverything)
            }
            negative {
            }
        }
        readModels {
            positive {
                rule(::everybodyAreAllowedReadEverything)
            }
            negative { }
        }
        readProperties {
            positive {
                rule(::everybodyCanReadAnyProperty)
            }
            negative {
            }
        }
        eventLog {
            positive {
                rule(::everybodyCanReadEventLog)
            }
            negative {
            }
        }
    }
}

fun everybodyAreAllowedReadEverything(args: ArgModelContextReader<Ctx, Collections>): PositiveAuthorization {
    return Allow
}

fun everybodyAreAllowedToDoEverything(args: ArgCommandContextReader<*, Ctx, Collections>): PositiveAuthorization {
    return Allow
}

fun everybodyCanReadAnyProperty(args: ArgsForPropertyAuth<Ctx, Collections>): PositiveAuthorization {
    return Allow
}

fun everybodyCanReadEventLog(args: ArgContextReader<Ctx, Collections>): PositiveAuthorization {
    return Allow
}
