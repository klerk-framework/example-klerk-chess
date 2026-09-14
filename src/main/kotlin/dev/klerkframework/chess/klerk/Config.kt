package dev.klerkframework.chess.klerk

import dev.klerkframework.chess.CalculateAiAction
import dev.klerkframework.chess.klerk.game.Game
import dev.klerkframework.chess.klerk.game.createGameStateMachine
import dev.klerkframework.chess.klerk.user.User
import dev.klerkframework.chess.klerk.user.createUserStateMachine
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.view.ModelViews
import dev.klerkframework.klerk.storage.AttachedBlobStore
import dev.klerkframework.klerk.storage.Persistence
import dev.klerkframework.klerk.storage.SqlPersistence
import dev.klerkframework.web.assets.AssetsPlugin
import kotlinx.html.emptyMap
import org.sqlite.SQLiteDataSource
import kotlin.time.Clock
import kotlin.time.Instant

class Ctx(
    override val actor: ActorIdentity,
    override val eventLogExtra: String? = null,
    override val time: Instant = Clock.System.now(),
    override val translation: Translation = DefaultTranslation,
    val user: Model<User>? = null
) : KlerkContext {

    /**
     * The acting user, if any. Unlike [user] this also works when the actor was rebuilt from storage (e.g. in a job),
     * where only the id is available.
     */
    val userId: ModelID<*>? get() = user?.id ?: actor.id

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

fun createConfig(): Specification<Ctx, Collections> {
    val collections = Collections(ModelViews(), ModelViews())
    return SpecificationBuilder<Ctx, Collections>(collections).build {
        plugins(AssetsPlugin(emptySet()))
        managedModels {
            model(User::class, createUserStateMachine(), collections.users)
            model(Game::class, createGameStateMachine(collections), collections.games)
        }
        jobs {
            register(CalculateAiAction)
        }
        //apply(createAuthorizationRules())
        authorization {
            allowEverythingInsecurely()   // TODO
        }
        systemContextProvider { Ctx(SystemIdentity) }
        jobContextProvider(::jobContext)
    }
}

/** A job step runs as the actor that scheduled it, which arrives as a plain id — see [Ctx.userId]. */
fun jobContext(request: JobContextRequest): Ctx = Ctx(actor = request.actor, time = request.time)

fun createSettings(): KlerkSettings =
    KlerkSettings(persistence = createPersistence(), attachedBlobStore = AttachedBlobStore.Database)

private fun createPersistence(): Persistence {
    val dbFilePath =
        requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
    val ds = SQLiteDataSource()
    ds.url = "jdbc:sqlite:$dbFilePath"
    return SqlPersistence(ds)
}
