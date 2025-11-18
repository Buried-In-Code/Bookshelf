package github.buriedincode.bookshelf.controllers

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.controllers.AuthenticationController.getSession
import github.buriedincode.bookshelf.database.Book
import github.buriedincode.bookshelf.database.Read
import github.buriedincode.bookshelf.database.ReadTable
import github.buriedincode.bookshelf.database.Wished
import github.buriedincode.bookshelf.database.WishedTable
import github.buriedincode.openlibrary.ServiceException
import github.buriedincode.openlibrary.schemas.Edition
import github.buriedincode.openlibrary.schemas.id
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import kotlin.collections.emptyList
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.CompositeID

object BookController {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}
  @JvmStatic private val WRITER = WebpWriter.MAX_LOSSLESS_COMPRESSION

  private fun Context.getResource(): Book {
    return this.pathParam("book-id").let { Book.findById(id = it) ?: throw NotFoundResponse("Book not found.") }
      ?: throw BadRequestResponse("Bad book id")
  }

  private fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
    } else {
      ctx.render("templates/book/$template.kte", mapOf<String, Any?>("session" to session) + model)
    }
  }

  private fun isIsbn(query: String): Boolean {
    val normalized = query.replace("-", "").replace(" ", "")
    val isbnRegex = Regex("""^(?:\d{9}[0-9Xx]|\d{13})$""")
    return isbnRegex.matches(normalized)
  }

  fun listBooksPage(ctx: Context) = transaction {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
      return@transaction
    }
    val query = ctx.queryParam("q")
    var errorMessage: String? = null
    val results =
      query?.let {
        try {
          if (it.startsWith("OL")) {
            val edition = Utils.openLibrary.getEdition(id = it)
            listOf(BookResult(edition = edition, isLocal = Book.findById(id = edition.id) != null))
          } else if (isIsbn(it)) {
            val edition = Utils.openLibrary.getEditionByISBN(isbn = it.replace("-", "").replace(" ", ""))
            listOf(BookResult(edition = edition, isLocal = Book.findById(id = edition.id) != null))
          } else {
            Utils.openLibrary.searchWork(params = mapOf("title" to it)).mapNotNull {
              it.coverEditionKey?.split("/")?.last()?.let { editionId ->
                val edition = Utils.openLibrary.getEdition(id = editionId)
                BookResult(edition = edition, isLocal = Book.findById(id = edition.id) != null)
              }
            }
          }
        } catch (se: ServiceException) {
          LOGGER.error(se) { "Error fetching book results for query: $query" }
          errorMessage = "${se::class.simpleName}: Error fetching book results for query '$query'"
          emptyList()
        }
      } ?: emptyList()
    render(ctx, "list", mapOf("query" to query, "results" to results, "errorMessage" to errorMessage))
  }

  @OptIn(ExperimentalTime::class)
  fun viewBookPage(ctx: Context) = transaction {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
      return@transaction
    }
    try {
      ctx.getResource()
    } catch (_: NotFoundResponse) {
      Book.openLibraryImport(ctx.pathParam("book-id"))?.let {
        it.lastUpdated = Clock.System.now().minus(25, DateTimeUnit.HOUR)
        it
      } ?: throw BadRequestResponse()
    }
    render(ctx = ctx, template = "view", model = mapOf<String, Any?>("resource" to ctx.getResource()))
  }

  fun overviewTabPartial(ctx: Context): Unit = transaction {
    val session = ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    ctx.render("components/book/tabs/overview.kte", mapOf("session" to session, "resource" to resource))
  }

  fun creditsTabPartial(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    ctx.getResource()
    ctx.render("components/book/tabs/credits.kte")
  }

  fun readersTabPartial(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    ctx.render("components/book/tabs/readers.kte", mapOf("readers" to resource.readers.toList()))
  }

  fun wishersTabPartial(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    ctx.render("components/book/tabs/wishers.kte", mapOf("wishers" to resource.wishers.toList()))
  }

  fun bookImage(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    val cachedImage = Utils.CACHE_ROOT / "covers" / "${ resource.id.value }.webp"
    if (!cachedImage.exists()) {
      cachedImage.createParentDirectories()
      val remoteImage = Utils.fetchImage(resource.imageUrl) ?: throw BadRequestResponse("Unable to fetch image.")
      var image = ImmutableImage.loader().fromBytes(remoteImage)
      image = image.cover(640, 960)
      image.output(WRITER, cachedImage.toFile())
    }
    ctx.contentType("image/webp")
    ctx.result(cachedImage.readBytes())
  }

  @OptIn(ExperimentalTime::class)
  fun refresh(ctx: Context) = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    val resetInstant = Clock.System.now().minus(24, DateTimeUnit.HOUR)
    if (resource.lastUpdated < resetInstant) {
      resource.openLibraryImport()
      resource.lastUpdated = Clock.System.now()
      ctx.status(HttpStatus.NO_CONTENT)
    } else {
      ctx.status(HttpStatus.NOT_MODIFIED)
    }
  }

  fun toggleCollected(ctx: Context) = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    resource.isCollected = !resource.isCollected
    ctx.status(status = HttpStatus.NO_CONTENT)
  }

  @OptIn(ExperimentalTime::class)
  fun toggleRead(ctx: Context) = transaction {
    val session = ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    val readId = CompositeID {
      it[ReadTable.bookCol] = resource.id
      it[ReadTable.userCol] = session.id
    }
    val exists = Read.findById(readId)
    if (exists == null) {
      Read.new(readId) { this.date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    } else {
      exists.delete()
    }
    ctx.status(status = HttpStatus.NO_CONTENT)
  }

  @OptIn(ExperimentalTime::class)
  fun toggleWished(ctx: Context) = transaction {
    val session = ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    val wishedId = CompositeID {
      it[WishedTable.bookCol] = resource.id
      it[WishedTable.userCol] = session.id
    }
    val exists = Wished.findById(wishedId)
    if (exists == null) {
      Wished.new(wishedId) { this.date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    } else {
      exists.delete()
    }
    ctx.status(status = HttpStatus.NO_CONTENT)
  }
}

data class BookResult(val edition: Edition, val isLocal: Boolean = false)
