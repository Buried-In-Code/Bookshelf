package github.buriedincode.openlibrary

import github.buriedincode.openlibrary.schemas.Author
import github.buriedincode.openlibrary.schemas.Edition
import github.buriedincode.openlibrary.schemas.SearchResponse
import github.buriedincode.openlibrary.schemas.Work
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
import java.time.Duration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject

class OpenLibrary(private val cache: SQLiteCache? = null, timeout: Duration = Duration.ofSeconds(30)) {
  private val client: HttpClient =
    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(timeout).build()

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
      @Suppress("ktlint:standard:max-line-length", "ktlint:standard:argument-list-wrapping")
      val request =
        HttpRequest.newBuilder()
          .uri(uri)
          .setHeader("Accept", "application/json")
          .setHeader(
            "User-Agent",
            "OpenLibrary/0.5.0 (${System.getProperty("os.name")}/${System.getProperty("os.version")}; Kotlin/${KotlinVersion.CURRENT})",
          )
          .GET()
          .build()
      val response = this.client.send(request, HttpResponse.BodyHandlers.ofString())
      val level =
        when (response.statusCode()) {
          in 100 until 200 -> Level.WARN
          in 200 until 300 -> Level.DEBUG
          in 300 until 400 -> Level.INFO
          in 400 until 500 -> Level.WARN
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
        return JSON.decodeFromString(it)
      } catch (se: SerializationException) {
        LOGGER.warn(se) { "Unable to deserialize cached response" }
        this.cache.delete(url = uri.toString())
      }
    }
    val response = this.performGetRequest(uri = uri)
    this.cache?.insert(url = uri.toString(), response = response)
    return try {
      JSON.decodeFromString(response)
    } catch (se: SerializationException) {
      throw ServiceException(cause = se)
    }
  }

  @Throws(ServiceException::class)
  fun searchWork(params: Map<String, String>): List<SearchResponse.Work> {
    val resultList = mutableListOf<SearchResponse.Work>()
    var page = params.getOrDefault("page", "1").toInt()

    do {
      val uri = this.encodeURI(endpoint = "/search.json", params = params + ("page" to page.toString()))
      val response = this.getRequest<SearchResponse<SearchResponse.Work>>(uri = uri)
      resultList.addAll(response.docs)
      page++
    } while (response.numFound > resultList.size)

    return resultList
  }

  @Throws(ServiceException::class)
  fun searchAuthor(params: Map<String, String>): List<SearchResponse.Author> {
    val resultList = mutableListOf<SearchResponse.Author>()
    var page = params.getOrDefault("page", "1").toInt()

    do {
      val uri = this.encodeURI(endpoint = "/search/authors.json", params = params + ("page" to page.toString()))
      val response = this.getRequest<SearchResponse<SearchResponse.Author>>(uri = uri)
      resultList.addAll(response.docs)
      page++
    } while (response.numFound > resultList.size)

    return resultList
  }

  @Throws(ServiceException::class)
  fun getAuthor(id: String): Author = this.getRequest(uri = this.encodeURI(endpoint = "/author/$id.json"))

  @Throws(ServiceException::class)
  fun getEdition(id: String): Edition = this.getRequest(uri = this.encodeURI(endpoint = "/edition/$id.json"))

  @Throws(ServiceException::class)
  fun getEditionByISBN(isbn: String): Edition = this.getRequest(uri = this.encodeURI(endpoint = "/isbn/$isbn.json"))

  @Throws(ServiceException::class)
  fun getWork(id: String): Work = this.getRequest(uri = this.encodeURI(endpoint = "/work/$id.json"))

  companion object {
    @JvmStatic private val LOGGER = KotlinLogging.logger {}
    private const val BASE_API = "https://openlibrary.org"

    @OptIn(ExperimentalSerializationApi::class)
    private val JSON: Json = Json {
      prettyPrint = true
      encodeDefaults = true
      namingStrategy = JsonNamingStrategy.SnakeCase
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
