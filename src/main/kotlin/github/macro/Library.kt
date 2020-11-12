package github.macro

import freemarker.cache.ClassTemplateLoader
import github.macro.book.*
import github.macro.config.Config.Companion.CONFIG
import github.macro.database.BookTable
import github.macro.database.CollectionTable
import github.macro.database.WishlistTable
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-30
 */
object Library {
	private val LOGGER = LogManager.getLogger(Library::class.java)

	init {
		LOGGER.info("Initializing Book Manager")
	}

	private fun checkLogLevels() {
		Level.values().sorted().forEach {
			LOGGER.log(it, "{} is Visible", it.name())
		}
	}

	@JvmStatic
	fun main(args: Array<String>) {
		checkLogLevels()
		embeddedServer(
			Netty,
			port = CONFIG.server.port ?: 6606,
			host = CONFIG.server.hostName ?: "localhost",
			module = Application::module
		).apply { start(wait = true) }
	}
}

fun Application.module() {
	install(ContentNegotiation) {
		register(contentType = ContentType.Application.Json, converter = GsonConverter(gson = Utils.GSON))
	}
	install(DefaultHeaders) {
		header(name = HttpHeaders.Server, value = "Ktor-Book-Manager")
		header(name = "Developer", value = "Macro303")
		header(name = HttpHeaders.ContentLanguage, value = "en-NZ")
	}
	install(Compression)
	install(ConditionalHeaders)
	install(AutoHeadResponse)
	install(XForwardedHeaderSupport)
	install(CallLogging) {
		level = org.slf4j.event.Level.INFO
	}
	install(FreeMarker) {
		templateLoader = ClassTemplateLoader(this::class.java, "/templates")
	}
	install(StatusPages) {
		exception<Throwable> {
			val error = ErrorMessage(
				code = HttpStatusCode.InternalServerError,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = it.message ?: "",
				cause = it
			)
			call.respond(error = error)
		}
		exception<BadRequestException> {
			val error = ErrorMessage(
				code = HttpStatusCode.BadRequest,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = it.message
			)
			call.respond(error = error)
		}
		exception<UnauthorizedException> {
			val error = ErrorMessage(
				code = HttpStatusCode.Unauthorized,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = it.message
			)
			call.respond(error = error)
		}
		exception<ConflictException> {
			val error = ErrorMessage(
				code = HttpStatusCode.Conflict,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = it.message
			)
			call.respond(error = error)
		}
		exception<NotImplementedException> {
			val error = ErrorMessage(
				code = HttpStatusCode.NotImplemented,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = it.message
			)
			call.respond(error = error)
		}
		status(HttpStatusCode.NotFound) {
			val error = ErrorMessage(
				code = HttpStatusCode.NotFound,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = "Unable to Find Endpoint"
			)
			call.respond(error = error)
		}
		status(HttpStatusCode.NotAcceptable) {
			val error = ErrorMessage(
				code = HttpStatusCode.NotAcceptable,
				request = "${call.request.httpMethod.value} ${call.request.local.uri}",
				message = "Invalid Accept Header"
			)
			call.respond(error = error)
		}
	}
	intercept(ApplicationCallPipeline.Monitoring) {
		application.log.debug(">> ${call.request.httpVersion} ${call.request.httpMethod.value} ${call.request.uri}, Accept: ${call.request.accept()}, Content-Type: ${call.request.contentType()}, User-Agent: ${call.request.userAgent()}, Host: ${call.request.origin.remoteHost}:${call.request.port()}")
	}
	install(Routing) {
		trace { application.log.trace(it.buildText()) }
		route(path = "/api") {
			route(path = "/v1") {
				fun ApplicationCall.getISBN() = Isbn.of(parameters["isbn"])
					?: throw BadRequestException("Invalid ISBN")

				fun ApplicationCall.getUUID() = UUID.fromString(parameters["uuid"])
					?: throw BadRequestException("Invalid UUID")

				fun ApplicationCall.getQueryParams() = request.queryParameters.lowerCase()

				suspend fun ApplicationCall.getBook(): Book {
					return withContext(Dispatchers.IO) {
						val request = receiveOrNull<NewBookRequest>()
							?: throw BadRequestException("A Body is Required")
						val isbn = Isbn.of(request.isbn) ?: throw BadRequestException("Invalid ISBN")
						return@withContext Book.create(isbn) ?: throw NotFoundException("Unable to find Book")
					}
				}
				route("/books") {
					get {
						val queryParams = call.getQueryParams()
						val books = BookTable.search(
							title = queryParams["title"],
							subtitle = queryParams["subtitle"],
							author = queryParams["author"]
						).sorted()
						val bookMaps = books.map {
							val output = mutableMapOf<String, Any?>(
								"Book" to it.toJson(),
								"Collection" to (CollectionTable.selectUnique(it.isbn)?.count ?: 0),
								"Wishlist" to (WishlistTable.selectUnique(it.isbn)?.count ?: 0)
							)
							output.toSortedMap()
						}
						call.respond(message = bookMaps)
					}
					post {
						call.respond(
							message = call.getBook().toJson(),
							status = HttpStatusCode.Created
						)
					}
					route(path = "/{isbn}") {
						get {
							val book = BookTable.selectUnique(call.getISBN())
								?: throw NotFoundException("Unable to find book in Database")
							val bookMap = mutableMapOf<String, Any?>(
								"Book" to book.toJson(),
								"Collection" to (CollectionTable.selectUnique(book.isbn)?.count ?: 0),
								"Wishlist" to (WishlistTable.selectUnique(book.isbn)?.count ?: 0)
							)
							call.respond(message = bookMap.toSortedMap())
						}
						put {
							throw NotImplementedException()
						}
						delete {
							throw NotImplementedException()
						}
					}
					route("/collection") {
						get {
							val queryParams = call.getQueryParams()
							val books = CollectionTable.search(
								title = queryParams["title"],
								subtitle = queryParams["subtitle"],
								author = queryParams["author"]
							).sorted()
							call.respond(message = books.map { it.toJson() })
						}
						post {
							val book = call.getBook()
							val entry = CollectionTable.selectUnique(book.isbn) ?: CollectionBook(book.isbn, 0).add()
							entry.count = entry.count + 1
							call.respond(
								message = entry.push().toJson(),
								status = HttpStatusCode.Created
							)
						}
					}
					route("/wishlist") {
						get {
							val queryParams = call.getQueryParams()
							val books = WishlistTable.search(
								title = queryParams["title"],
								subtitle = queryParams["subtitle"],
								author = queryParams["author"]
							).sorted()
							call.respond(message = books.map { it.toJson() })
						}
						post {
							val book = call.getBook()
							val entry = WishlistTable.selectUnique(book.isbn) ?: WishlistBook(book.isbn, 0).add()
							entry.count = entry.count + 1
							call.respond(
								message = entry.push().toJson(),
								status = HttpStatusCode.Created
							)
						}
					}
				}
				route("/contributors") {
					get {
						val contributors = listOf(
							mapOf<String, Any?>(
								"Title" to "Macro303",
								"Image" to "macro303.png",
								"Role" to "Creator and Maintainer"
							).toSortedMap(),
							mapOf<String, Any?>(
								"Title" to "Img Bot App",
								"Image" to "imgbotapp.png",
								"Role" to "Image Processor"
							)
						)
						call.respond(contributors)
					}
				}
			}
		}
		static {
			defaultResource(resource = "/static/index.html")
			resources(resourcePackage = "static/css")
			resources(resourcePackage = "static/images")
			resources(resourcePackage = "static/js")
			resource(remotePath = "/navbar.html", resource = "static/navbar.html")
			resource(remotePath = "/collection", resource = "static/collection.html")
			resource(remotePath = "/wishlist", resource = "static/wishlist.html")
			resource(remotePath = "/loan", resource = "static/loan.html")
			resource(remotePath = "/about", resource = "static/about.html")
		}
	}
}

suspend fun ApplicationCall.respond(error: ErrorMessage) {
	if (request.local.uri.startsWith("/api"))
		respond(message = error.toJson(), status = error.code)
	else
		respond(
			message = FreeMarkerContent(template = "exception.ftl", model = error),
			status = error.code
		)
	when {
		error.code.value < 100 -> application.log.error("${error.code}: ${request.httpMethod.value} - ${request.uri} => ${error.message}")
		error.code.value in (100 until 200) -> application.log.info("${error.code}: ${request.httpMethod.value} - ${request.uri} => ${error.message}")
		error.code.value in (200 until 300) -> application.log.info("${error.code}: ${request.httpMethod.value} - ${request.uri}")
		error.code.value in (300 until 400) -> application.log.debug("${error.code}: ${request.httpMethod.value} - ${request.uri} => ${error.message}")
		error.code.value in (400 until 500) -> application.log.warn("${error.code}: ${request.httpMethod.value} - ${request.uri} => ${error.message}")
		error.code.value >= 500 -> application.log.error("${error.code}: ${request.httpMethod.value} - ${request.uri} => ${error.message}")
	}
}

fun Parameters.lowerCase(): Map<String, String?> =
	this.toMap().mapKeys { it.key.toLowerCase() }.mapValues { it.value.firstOrNull() }