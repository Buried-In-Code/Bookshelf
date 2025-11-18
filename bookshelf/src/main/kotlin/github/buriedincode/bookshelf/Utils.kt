package github.buriedincode.bookshelf

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
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
    System.getenv("XDG_CACHE_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".cache")
  }
  private val XDG_CONFIG_HOME: Path by lazy {
    System.getenv("XDG_CONFIG_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".config")
  }
  private val XDG_DATA_HOME: Path by lazy {
    System.getenv("XDG_DATA_HOME")?.let { Paths.get(it) } ?: (this.USER_HOME / ".local" / "share")
  }

  internal val CACHE_ROOT by lazy { this.XDG_CACHE_HOME / PROJECT_NAME.lowercase() }
  internal val CONFIG_ROOT by lazy { this.XDG_CONFIG_HOME / PROJECT_NAME.lowercase() }
  internal val DATA_ROOT by lazy { this.XDG_DATA_HOME / PROJECT_NAME.lowercase() }

  private val DATABASE: Database by lazy {
    Database.connect(
      url = "jdbc:sqlite:${this.DATA_ROOT / "${PROJECT_NAME.lowercase()}.sqlite"}",
      driver = "org.sqlite.JDBC",
      databaseConfig =
        DatabaseConfig {
          @OptIn(ExperimentalKeywordApi::class)
          preserveKeywordCasing = true
        },
    )
  }

  internal val settings: Settings by lazy { Settings.load() }

  val openLibrary: OpenLibrary by lazy {
    OpenLibrary(cache = SQLiteCache(path = this.CACHE_ROOT / "openlibrary.sqlite", expiry = 1))
  }

  init {
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

  internal fun fetchImage(url: String): ByteArray? {
    return try {
      val client =
        HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.ALWAYS)
          .connectTimeout(30.seconds.toJavaDuration())
          .build()
      val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
      if (response.statusCode() == 200) {
        response.body()
      } else {
        null
      }
    } catch (exc: Exception) {
      LOGGER.error(exc) { "Error fetching image from $url" }
      null
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
      transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = this.DATABASE) {
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
