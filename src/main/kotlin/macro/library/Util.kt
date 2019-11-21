package macro.library

import com.google.gson.GsonBuilder
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.UnirestException
import macro.library.config.Config.Companion.CONFIG
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.nio.charset.Charset

/**
 * Created by Macro303 on 2019-Oct-30
 */
object Util {
	private val LOGGER = LogManager.getLogger(Util::class.java)
	private val HEADERS = mapOf(
		"Accept" to ContentType.Application.Json.withCharset(Charset.forName("UTF-8")).toString(),
		"User-Agent" to "Book-Manager"
	)
	val SQLITE_DATABASE = "Book-Manager.sqlite"
	internal val DATABASE_URL = "jdbc:sqlite:$SQLITE_DATABASE"
	internal val GSON = GsonBuilder()
		.setPrettyPrinting()
		.serializeNulls()
		.disableHtmlEscaping()
		.create()

	init {
		Unirest.config().enableCookieManagement(false)
		if(CONFIG.proxy.hostName != null && CONFIG.proxy.port != null)
			Unirest.config().proxy(CONFIG.proxy.hostName, CONFIG.proxy.port!!)
	}

	@JvmOverloads
	fun httpRequest(url: String, headers: Map<String, String> = HEADERS): JsonNode? {
		val request = Unirest.get(url)
		request.headers(headers)
		LOGGER.debug("GET : >>> - ${request.url} - $headers")
		val response: HttpResponse<JsonNode>
		try {
			response = request.asJson()
		} catch (ue: UnirestException) {
			LOGGER.error("Unable to load URL: $ue")
			return null
		}

		var level = Level.ERROR
		when {
			response.status < 100 -> level = Level.ERROR
			response.status < 200 -> level = Level.INFO
			response.status < 300 -> level = Level.INFO
			response.status < 400 -> level = Level.WARN
			response.status < 500 -> level = Level.WARN
		}
		LOGGER.log(level, "GET: ${response.status} ${response.statusText} - ${request.url}")
		LOGGER.debug("Response: ${response.body}")
		return if (response.status != 200) null else response.body
	}

	@JvmOverloads
	fun httpStrRequest(url: String, headers: Map<String, String> = HEADERS): String? {
		val request = Unirest.get(url)
		request.headers(headers)
		LOGGER.debug("GET : >>> - ${request.url} - $headers")
		val response: HttpResponse<String>
		try {
			response = request.asString()
		} catch (ue: UnirestException) {
			LOGGER.error("Unable to load URL: $ue")
			return null
		}

		var level = Level.ERROR
		when {
			response.status < 100 -> level = Level.ERROR
			response.status < 200 -> level = Level.INFO
			response.status < 300 -> level = Level.INFO
			response.status < 400 -> level = Level.WARN
			response.status < 500 -> level = Level.WARN
		}
		LOGGER.log(level, "GET: ${response.status} ${response.statusText} - ${request.url}")
		LOGGER.debug("Response: ${response.body}")
		return if (response.status != 200) null else response.body
	}
}