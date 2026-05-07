package duckpond.buriedincode.bookcatalogue

import duckpond.buriedincode.bookcatalogue.cache.SQLiteCache
import duckpond.buriedincode.bookcatalogue.exceptions.ServiceException
import duckpond.buriedincode.bookcatalogue.schemas.ListResponse
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject

class BookCatalogue(private val cache: SQLiteCache? = null, timeout: Duration = 30.seconds) {
  private val client: HttpClient =
    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(timeout.toJavaDuration()).build()

  fun encodeURI(endpoint: String, params: Map<String, String> = emptyMap()): URI {
    val encodedParams =
      params.entries
        .sortedBy { it.key }
        .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}" }
    return URI.create("$BASE_API$endpoint${if (encodedParams.isNotEmpty()) "?$encodedParams" else ""}")
  }

  @Throws(ServiceException::class)
  private fun performGetRequest(uri: URI): String {
    try {
      val request =
        HttpRequest.newBuilder()
          .uri(uri)
          .setHeader("Accept", "application/json")
          .setHeader("User-Agent", "Bookshelf/2026.1.1 ($OS_AGENT; $LANGUAGE_AGENT)")
          .GET()
          .build()
      val response = this.client.send(request, HttpResponse.BodyHandlers.ofString())
      val level =
        when (response.statusCode()) {
          in 100 until 200 -> Level.WARN
          in 200 until 300 -> Level.INFO
          in 300 until 400 -> Level.WARN
          in 400 until 500 -> Level.ERROR
          else -> Level.ERROR
        }
      LOGGER.log(level) { "GET: ${response.statusCode()} - $uri" }
      if (response.statusCode() == 200) {
        return response.body()
      }

      val content = JSON.parseToJsonElement(response.body()).jsonObject
      LOGGER.error { content.toString() }
      throw ServiceException(content.toString())
    } catch (ioe: IOException) {
      throw ServiceException(cause = ioe)
    } catch (hcte: HttpConnectTimeoutException) {
      throw ServiceException(cause = hcte)
    } catch (ie: InterruptedException) {
      throw ServiceException(cause = ie)
    } catch (se: SerializationException) {
      throw ServiceException(cause = se)
    }
  }

  @Throws(ServiceException::class)
  internal inline fun <reified T> getRequest(uri: URI): T {
    this.cache?.select(url = uri.toString())?.let {
      try {
        LOGGER.debug { "Using cached response for $uri" }
        return JSON.decodeFromString(it.response)
      } catch (se: SerializationException) {
        LOGGER.warn(se) { "Unable to deserialize cached response" }
        this.cache.delete(url = uri.toString())
      }
    }
    val response = performGetRequest(uri = uri)
    this.cache?.upsert(url = uri.toString(), response = response)
    return try {
      JSON.decodeFromString(response)
    } catch (se: SerializationException) {
      throw ServiceException(cause = se)
    }
  }

  @Throws(ServiceException::class)
  internal inline fun <reified T> fetchItem(endpoint: String): ResponseData {
    val results =
      getRequest<ListResponse>(uri = encodeURI(endpoint = endpoint)).results.results.filter { it.status == "OK" }
    return results.firstOrNull { it is ResponseData.BookCatalogue }
      ?: results.firstOrNull { it is ResponseData.OpenLibrary }
      ?: results.firstOrNull { it is ResponseData.Google }
      ?: results.firstOrNull { it is ResponseData.Goodreads }
      ?: throw ServiceException("No results found for endpoint: $endpoint")
  }

  @Throws(ServiceException::class)
  fun getBook(isbn: String): ResponseData = fetchItem<ResponseData>(endpoint = "/search/isbn/$isbn")

  companion object {
    private val LOGGER = KotlinLogging.logger {}
    private val OS_AGENT = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
    private val LANGUAGE_AGENT = "Kotlin v${KotlinVersion.CURRENT}/Java v${System.getProperty("java.version")}"
    private const val BASE_API = "https://book-catalogue.com/api"

    @OptIn(ExperimentalSerializationApi::class)
    private val JSON: Json = Json {
      prettyPrint = true
      encodeDefaults = true
      namingStrategy = JsonNamingStrategy.SnakeCase
      classDiscriminator = "source"
    }
  }
}

private fun KLogger.log(level: Level, message: () -> Any?) {
  when (level) {
    Level.TRACE -> this.trace(message)
    Level.DEBUG -> this.debug(message)
    Level.INFO -> this.info(message)
    Level.WARN -> this.warn(message)
    Level.ERROR -> this.error(message)
    else -> return
  }
}
