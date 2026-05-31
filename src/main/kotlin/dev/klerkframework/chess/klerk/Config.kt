package dev.klerkframework.chess.klerk

import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.game.createGameStateMachine
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.chess.klerk.user.createUserStateMachine
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.collection.ModelViews
import dev.klerkframework.klerk.storage.Persistence
import dev.klerkframework.klerk.storage.SqlPersistence
import dev.klerkframework.web.assets.AssetsPlugin
import kotlinx.html.emptyMap
import org.sqlite.SQLiteDataSource
import kotlin.time.Clock
import kotlin.time.Instant

class Ctx(
    override val actor: ActorIdentity,
    override val auditExtra: String? = null,
    override val time: Instant = Clock.System.now(),
    override val translation: Translation = DefaultTranslation,
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
    val users: ModelViews<User, Ctx>,
    val games: ModelViews<Game, Ctx>,
)

fun createConfig(): Config<Ctx, Collections> {
    val collections = Collections(ModelViews(), ModelViews())
    return ConfigBuilder<Ctx, Collections>(collections).build {
        persistence(createPersistence())
        managedModels {
            model(User::class, createUserStateMachine(), collections.users)
            model(Game::class, createGameStateMachine(collections), collections.games)
        }
        apply(createAuthorizationRules())
        systemContextProvider { systemIdentity -> Ctx(systemIdentity) }
    }.withPlugin(AssetsPlugin(emptySet()))
}

private fun createPersistence(): Persistence {
    val dbFilePath =
        requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
    val ds = SQLiteDataSource()
    ds.url = "jdbc:sqlite:$dbFilePath"
    return SqlPersistence(ds)
}
