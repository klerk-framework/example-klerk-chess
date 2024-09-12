package dev.klerkframework.chess.klerk

import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.game.createGameStateMachine
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.chess.klerk.user.createUserStateMachine
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.collection.ModelCollections
import dev.klerkframework.klerk.storage.Persistence
import dev.klerkframework.klerk.storage.SqlPersistence
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.sqlite.SQLiteDataSource

class Ctx(
    override val actor: ActorIdentity,
    override val auditExtra: String? = null,
    override val time: Instant = Clock.System.now(),
    override val translator: Translator = DefaultTranslator(),
    val user: Model<User>? = null
) : KlerkContext {

    companion object {
        fun fromUser(user: Model<User>): Ctx {
            return Ctx(ModelIdentity(user), user = user)
        }

        fun unauthenticated(): Ctx = Ctx(Unauthenticated)

        fun authenticationIdentity(): Ctx = Ctx(AuthenticationIdentity)

        fun system(): Ctx = Ctx(SystemIdentity)
    }

}

data class Collections(
    val users: ModelCollections<User, Ctx>,
    val games: ModelCollections<Game, Ctx>,
)

fun createConfig(): Config<Ctx, Collections> {
    val collections = Collections(ModelCollections(), ModelCollections())
    return ConfigBuilder<Ctx, Collections>(collections).build {
        persistence(createPersistence())
        managedModels {
            model(User::class, createUserStateMachine(), collections.users)
            model(Game::class, createGameStateMachine(collections), collections.games)
        }
        apply(createAuthorizationRules())
        contextProvider { actor -> Ctx(actor) }
    }
}

private fun createPersistence(): Persistence {
    val dbFilePath =
        requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
    val ds = SQLiteDataSource()
    ds.url = "jdbc:sqlite:$dbFilePath"
    return SqlPersistence(ds)
}
