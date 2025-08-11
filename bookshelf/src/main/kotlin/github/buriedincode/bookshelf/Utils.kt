package github.buriedincode.bookshelf

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlin.time.toDuration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ExperimentalKeywordApi
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal const val VERSION = "0.5.0"
internal const val PROJECT_NAME = "Bookshelf"

object Utils {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  private val USER_HOME: Path by lazy { Paths.get(System.getProperty("user.home")) }
  private val XDG_CACHE_HOME: Path by lazy {
    System.getenv("XDG_CACHE_HOME")?.let { Paths.get(it) } ?: (USER_HOME / ".cache")
  }
  private val XDG_CONFIG_HOME: Path by lazy {
    System.getenv("XDG_CONFIG_HOME")?.let { Paths.get(it) } ?: (USER_HOME / ".config")
  }
  private val XDG_DATA_HOME: Path by lazy {
    System.getenv("XDG_DATA_HOME")?.let { Paths.get(it) } ?: (USER_HOME / ".local" / "share")
  }

  internal val CACHE_ROOT by lazy { XDG_CACHE_HOME / PROJECT_NAME.lowercase() }
  internal val CONFIG_ROOT by lazy { XDG_CONFIG_HOME / PROJECT_NAME.lowercase() }
  internal val DATA_ROOT by lazy { XDG_DATA_HOME / PROJECT_NAME.lowercase() }

  private val DATABASE: Database by lazy {
    val settings = Settings.load()
    Database.connect(
      url = "jdbc:sqlite:${settings.database.url}",
      driver = "org.sqlite.JDBC",
      databaseConfig =
        DatabaseConfig {
          @OptIn(ExperimentalKeywordApi::class)
          preserveKeywordCasing = true
        },
    )
  }

  val openLibrary: OpenLibrary by lazy {
    val settings = Settings.load()
    OpenLibrary(cache = SQLiteCache(path = CACHE_ROOT / "openlibrary.sqlite", expiry = 1))
  }

  init {
    listOf(CACHE_ROOT, CONFIG_ROOT, DATA_ROOT).forEach { if (!it.exists()) it.createDirectories() }
  }

  private fun getDayNumberSuffix(day: Int): String {
    return if (day in 11..13) {
      "th"
    } else {
      when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
      }
    }
  }

  private fun TemporalAccessor.formatToPattern(pattern: String): String {
    return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(this)
  }

  internal fun KLogger.log(level: Level, message: () -> Any?) {
    when (level) {
      Level.TRACE -> this.trace(message)
      Level.DEBUG -> this.debug(message)
      Level.INFO -> this.info(message)
      Level.WARN -> this.warn(message)
      Level.ERROR -> this.error(message)
      else -> return
    }
  }

  internal fun Duration.toHumanReadable(): String = this.toString()

  internal fun Long.toHumanReadable(): String = this.toDuration(DurationUnit.MILLISECONDS).toHumanReadable()

  internal fun Float.toHumanReadable(): String = this.toLong().toHumanReadable()

  inline fun <reified T : Enum<T>> String.asEnumOrNull(): T? =
    enumValues<T>().firstOrNull {
      it.name.equals(this, ignoreCase = true) ||
        it.name.replace("_", " ").equals(this, ignoreCase = true) ||
        it.name.equals(this.replace("-", "_"), ignoreCase = true)
    }

  fun <T> transaction(block: () -> T): T {
    val result = measureTimedValue {
      transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = DATABASE) {
        addLogger(Slf4jSqlDebugLogger)
        block()
      }
    }
    LOGGER.debug { "Took ${result.duration.toHumanReadable()}" }
    return result.value
  }

  fun LocalDate.toHumanReadable(showFull: Boolean = false): String {
    val pattern =
      if (showFull) {
        "EEE, d'${getDayNumberSuffix(this.day)}' MMM yyyy"
      } else {
        "d'${getDayNumberSuffix(this.day)}' MMM yyyy"
      }
    return this.toJavaLocalDate().formatToPattern(pattern)
  }
}
