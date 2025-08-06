package github.buriedincode

import com.sksamuel.hoplite.Secret
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

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
    return@lazy Database.connect(
      url = "jdbc:sqlite:${settings.database.url}",
      driver = "org.sqlite.JDBC",
      databaseConfig =
        DatabaseConfig {
          @OptIn(ExperimentalKeywordApi::class)
          preserveKeywordCasing = true
        },
    )
  }

  init {
    listOf(CACHE_ROOT, CONFIG_ROOT, DATA_ROOT).forEach { if (!it.exists()) it.createDirectories() }
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

  internal fun Secret?.isNullOrBlank(): Boolean = this?.value.isNullOrBlank()

  inline fun <reified T : Enum<T>> String.asEnumOrNull(): T? =
    enumValues<T>().firstOrNull {
      it.name.equals(this, ignoreCase = true) || it.name.replace("_", " ").equals(this, ignoreCase = true)
    }

  fun String.toLocalDateOrNull(pattern: String): LocalDate? {
    return try {
      val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
      java.time.LocalDate.parse(this, formatter).toKotlinLocalDate()
    } catch (e: DateTimeParseException) {
      null
    }
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

  fun LocalDate.toString(pattern: String): String {
    return this.toJavaLocalDate().formatToPattern(pattern)
  }

  private fun TemporalAccessor.formatToPattern(pattern: String): String {
    return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(this)
  }
}
