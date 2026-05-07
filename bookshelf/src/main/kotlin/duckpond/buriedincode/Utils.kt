package duckpond.buriedincode

import duckpond.buriedincode.bookcatalogue.BookCatalogue
import duckpond.buriedincode.bookcatalogue.cache.SQLiteCache as BCCache
import duckpond.buriedincode.openlibrary.OpenLibrary
import duckpond.buriedincode.openlibrary.cache.SQLiteCache as OLCache
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import java.net.URI
import java.net.URL
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
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.measureTimedValue
import kotlin.time.toDuration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ExperimentalKeywordApi
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val LOGGER = KotlinLogging.logger {}
internal const val VERSION = "2026.1.0"
internal const val PROJECT = "Bookshelf"

internal val BOOK_CATALOGUE: BookCatalogue by lazy {
  BookCatalogue(cache = BCCache(path = Utils.CACHE_ROOT / "book-catalogue.sqlite", expiry = 1.days))
}
internal val OPEN_LIBRARY: OpenLibrary by lazy {
  OpenLibrary(cache = OLCache(path = Utils.CACHE_ROOT / "open-library.sqlite", expiry = 1.days))
}

internal val DATABASE: Database by lazy {
  Database.connect(
    url = "jdbc:sqlite:${Utils.DATA_ROOT / "${PROJECT.lowercase()}.sqlite"}",
    driver = "org.sqlite.JDBC",
    databaseConfig =
      DatabaseConfig {
        @OptIn(ExperimentalKeywordApi::class)
        preserveKeywordCasing = true
      },
  )
}

internal val SETTINGS: Settings by lazy { Settings.load() }

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

internal fun String.toUrl(): URL = URI.create(this).toURL()

internal fun <T> transaction(block: () -> T): T {
  val result = measureTimedValue {
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = DATABASE) {
      addLogger(Slf4jSqlDebugLogger)
      block()
    }
  }
  LOGGER.debug { "Took ${result.duration.toHumanReadable()}" }
  return result.value
}

object Utils {
  private val LOGGER = KotlinLogging.logger {}

  private val USER_HOME: Path by lazy { Paths.get(System.getProperty("user.home")) }
  private val XDG_CACHE_HOME: Path by lazy {
    System.getenv("XDG_CACHE_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".cache")
  }
  private val XDG_CONFIG_HOME: Path by lazy {
    System.getenv("XDG_CONFIG_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".config")
  }
  private val XDG_DATA_HOME: Path by lazy {
    System.getenv("XDG_DATA_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".local" / "share")
  }

  internal val CACHE_ROOT by lazy { this.XDG_CACHE_HOME / PROJECT.lowercase() }
  internal val CONFIG_ROOT by lazy { this.XDG_CONFIG_HOME / PROJECT.lowercase() }
  internal val DATA_ROOT by lazy { this.XDG_DATA_HOME / PROJECT.lowercase() }

  init {
    SETTINGS.ssl?.let {
      System.setProperty("javax.net.ssl.trustStore", it.trustStore)
      System.setProperty("javax.net.ssl.trustStorePassword", it.password)
    }
    listOf(this.CACHE_ROOT, this.CONFIG_ROOT, this.DATA_ROOT).forEach { if (!it.exists()) it.createDirectories() }
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

  internal fun URL.toPath(): Path = Paths.get(this.path)

  inline fun <reified T : Enum<T>> String.asEnumOrNull(): T? =
    enumValues<T>().firstOrNull {
      it.name.equals(this, ignoreCase = true) ||
        it.name.replace("_", " ").equals(this, ignoreCase = true) ||
        it.name.equals(this.replace("-", "_"), ignoreCase = true)
    }

  fun LocalDate.toHumanReadable(): String {
    val pattern = "d'${getDayNumberSuffix(this.day)}' MMM yyyy"
    return this.toJavaLocalDate().formatToPattern(pattern)
  }

  fun LocalTime.toHumanReadable(): String {
    return this.toString()
  }

  fun LocalDateTime.toHumanReadable(): String {
    val datePart = this.date.toHumanReadable()
    val timePart = this.time.toHumanReadable()
    return "$datePart $timePart"
  }

  @OptIn(ExperimentalTime::class)
  fun Instant.toHumanReadable(): String = this.toLocalDateTime(TimeZone.currentSystemDefault()).toHumanReadable()
}
