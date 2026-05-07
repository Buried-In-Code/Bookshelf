package duckpond.buriedincode.controllers

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import duckpond.buriedincode.BOOK_CATALOGUE
import duckpond.buriedincode.OPEN_LIBRARY
import duckpond.buriedincode.Utils
import duckpond.buriedincode.Utils.toPath
import duckpond.buriedincode.bookcatalogue.exceptions.ServiceException as BookCatalogueException
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData
import duckpond.buriedincode.database.Book
import duckpond.buriedincode.database.BookSeries
import duckpond.buriedincode.database.BookSeriesTable
import duckpond.buriedincode.database.BookTable
import duckpond.buriedincode.database.ReadBook
import duckpond.buriedincode.database.ReadBookTable
import duckpond.buriedincode.database.Series
import duckpond.buriedincode.database.WishedBook
import duckpond.buriedincode.database.WishedBookTable
import duckpond.buriedincode.openlibrary.exceptions.ServiceException as OpenLibraryException
import duckpond.buriedincode.openlibrary.schemas.Edition
import duckpond.buriedincode.services.USER_LIST
import duckpond.buriedincode.services.convert
import duckpond.buriedincode.services.getSession
import duckpond.buriedincode.services.getSessionOrNull
import duckpond.buriedincode.toUrl
import duckpond.buriedincode.transaction
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import java.io.IOException
import kotlin.collections.emptyList
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like

object BookController {
  private val LOGGER = KotlinLogging.logger {}
  private val WRITER = WebpWriter.MAX_LOSSLESS_COMPRESSION

  private fun Context.getResourceOrNull(): Book? =
    this.pathParam("book-id").toLongOrNull()?.let { Book.findById(id = it) }

  private fun Context.getResource(): Book = this.getResourceOrNull() ?: throw NotFoundResponse("Book not found.")

  private fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    ctx.render(
      "templates/book/${ template }.kte",
      mapOf("session" to ctx.getSessionOrNull(), "users" to USER_LIST) + model,
    )
  }

  private fun parseLocalDateOrNull(raw: String?): LocalDate? {
    val trimmed = raw?.trim() ?: return null
    if (trimmed.isEmpty()) {
      return null
    }
    if (trimmed == "today") {
      return Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
    return try {
      LocalDate.parse(trimmed)
    } catch (_: IllegalArgumentException) {
      throw BadRequestResponse("Invalid LocalDate")
    }
  }

  fun listPage(ctx: Context): Unit = transaction {
    val query = ctx.queryParam("q")
    val results = query?.let { Book.find { BookTable.titleCol like "%$query%" } } ?: Book.all()
    render(
      ctx = ctx,
      template = "list",
      model =
        mapOf(
          "query" to query,
          "results" to results.filter { !it.wishers.empty() || it.isCollected || !it.readers.empty() }.sorted().toList(),
        ),
    )
  }

  private fun isIsbn(query: String): Boolean {
    val normalized = query.replace("-", "").replace(" ", "")
    val isbnRegex = Regex("""^(?:\d{9}[0-9Xx]|\d{13})$""")
    return isbnRegex.matches(normalized)
  }

  fun searchPage(ctx: Context) {
    val query = ctx.queryParam("q")?.trim()
    var errorMessage: String? = null
    var results: List<SearchResult> = emptyList()
    if (query != null) {
      val isbn = query.replace("-", "").replace(" ", "")
      val localBook = transaction { Book.findOrNull(isbn = isbn) }
      if (localBook != null) {
        results = listOf(SearchResult.Local(localBook))
      } else {
        try {
          val bcData = BOOK_CATALOGUE.getBook(isbn = isbn)
          results =
            when (bcData) {
              is ResponseData.BookCatalogue -> listOf(SearchResult.BookCatalogue(bcData))
              is ResponseData.Google -> listOf(SearchResult.BookCatalogue(bcData))
              is ResponseData.Goodreads -> listOf(SearchResult.BookCatalogue(bcData))
              is ResponseData.OpenLibrary -> listOf(SearchResult.BookCatalogue(bcData))
              is ResponseData.Amazon -> listOf(SearchResult.BookCatalogue(bcData))
            }
        } catch (bce: BookCatalogueException) {
          LOGGER.error(bce) { "Error fetching book results for '$query' from BookCatalogue" }
          try {
            results = listOf(SearchResult.OpenLibrary(OPEN_LIBRARY.getEditionByIsbn(isbn = isbn)))
          } catch (ole: OpenLibraryException) {
            LOGGER.error(ole) { "Error fetching book results for '$query' from OpenLibrary" }
            errorMessage = "${ole.javaClass.simpleName}: Error fetching book results for '$query' from OpenLibrary"
          }
        }
      }
    }
    transaction {
      render(
        ctx = ctx,
        template = "search",
        model = mapOf("query" to query, "results" to results, "errorMessage" to errorMessage),
      )
    }
  }

  fun add(ctx: Context) {
    val session = transaction { ctx.getSession() }
    val body = ctx.bodyAsClass(AddBookRequest::class.java)
    if (!isIsbn(query = body.isbn)) {
      throw BadRequestResponse()
    }
    var book = transaction { Book.findOrNull(isbn = body.isbn) }
    if (book != null) {
      throw ConflictResponse()
    }
    try {
      val bcData = BOOK_CATALOGUE.getBook(isbn = body.isbn)
      book =
        when (bcData) {
          is ResponseData.BookCatalogue -> bcData.convert()
          is ResponseData.Google -> bcData.convert()
          is ResponseData.Goodreads -> bcData.convert()
          is ResponseData.OpenLibrary -> bcData.convert()
          is ResponseData.Amazon -> bcData.convert()
        }
    } catch (bce: BookCatalogueException) {
      LOGGER.error(bce) { "Error fetching book results for '${body.isbn}' from BookCatalogue" }
    }
    if (book == null) {
      try {
        val olData = OPEN_LIBRARY.getEditionByIsbn(isbn = body.isbn)
      } catch (ole: OpenLibraryException) {
        LOGGER.error(ole) { "Error fetching book results for '${body.isbn}' from OpenLibrary" }
      }
    }
    if (book == null) {
      throw BadRequestResponse()
    }
    if (body.wish) {
      transaction {
        val wishedId = CompositeID {
          it[WishedBookTable.bookCol] = book.id
          it[WishedBookTable.userCol] = session.id
        }
        WishedBook.new(wishedId) { this.date = Clock.System.todayIn(TimeZone.currentSystemDefault()) }
      }
    }
    if (body.collect) {
      transaction { book.isCollected = true }
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  fun updateState(ctx: Context) = transaction {
    val session = ctx.getSession()
    val resource = ctx.getResource()
    val body = ctx.bodyAsClass(UpdateBookStateRequest::class.java)
    val wishedId = CompositeID {
      it[WishedBookTable.bookCol] = resource.id
      it[WishedBookTable.userCol] = session.id
    }
    val readId = CompositeID {
      it[ReadBookTable.bookCol] = resource.id
      it[ReadBookTable.userCol] = session.id
    }
    val currentWished = WishedBook.findById(wishedId) != null
    val currentCollected = resource.isCollected
    val currentRead = ReadBook.findById(readId) != null
    var nextWished = body.wish ?: currentWished
    var nextCollected = body.collect ?: currentCollected
    var nextRead = body.read ?: currentRead
    if (nextWished) {
      nextCollected = false
      nextRead = false
    }
    if (nextRead) {
      nextCollected = true
      nextWished = false
    }
    if (!nextCollected) {
      nextRead = false
    }
    val wishedEntry = WishedBook.findById(wishedId)
    if (nextWished && wishedEntry == null) {
      WishedBook.new(wishedId) { this.date = Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    } else if (!nextWished) {
      wishedEntry?.delete()
    }
    resource.isCollected = nextCollected
    val readEntry = ReadBook.findById(readId)
    if (nextRead) {
      val readDate = parseLocalDateOrNull(body.readDate)
      if (readEntry == null) {
        ReadBook.new(readId) { this.date = readDate }
      } else if (readDate != null) {
        readEntry.date = readDate
      }
    } else {
      readEntry?.delete()
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  fun update(ctx: Context) = transaction {
    val session = ctx.getSession()
    val resource = ctx.getResource()
    val body = ctx.bodyAsClass(UpdateBookRequest::class.java)
    val format = body.format?.trim()?.takeIf { it.isNotBlank() }
    if (format != resource.format) {
      resource.format = format
    }
    if (body.isbn.trim() != resource.isbn) {
      resource.isbn = body.isbn.trim()
    }
    val publishDate = body.publishDate?.trim()?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
    if (publishDate != resource.publishDate) {
      resource.publishDate = publishDate
    }
    val publisher = body.publisher?.trim()?.takeIf { it.isNotBlank() }
    if (publisher != resource.publisher) {
      resource.publisher = publisher
    }
    val summary = body.summary?.trim()?.takeIf { it.isNotBlank() }?.replace("<br /", "\n")?.replace("&amp;", "&")
    if (summary != resource.summary) {
      resource.summary = summary
    }
    if (body.title.trim() != resource.title) {
      resource.title = body.title.trim()
    }
    if (body.readDate != null) {
      val readId = CompositeID {
        it[ReadBookTable.bookCol] = resource.id
        it[ReadBookTable.userCol] = session.id
      }
      ReadBook.findById(readId)?.let { it.date = parseLocalDateOrNull(body.readDate) }
    }
    body.series.forEach { seriesBody ->
      val series = Series.findById(seriesBody.seriesId) ?: throw BadRequestResponse("Unknown series")
      val entry =
        BookSeries.find { (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id) }
          .firstOrNull()
      if (seriesBody.remove) {
        entry?.delete()
      } else {
        if (entry == null) {
          BookSeries.new {
            this.book = resource
            this.series = series
            this.index = if (seriesBody.index.isBlank()) "0" else seriesBody.index.trim()
          }
        } else {
          entry.index = if (seriesBody.index.isBlank()) "0" else seriesBody.index.trim()
        }
      }
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  fun viewPage(ctx: Context): Unit = transaction {
    render(ctx = ctx, template = "view", model = mapOf("resource" to ctx.getResource()))
  }

  fun editPage(ctx: Context): Unit = transaction {
    val resource = ctx.getResource()
    val session =
      ctx.getSessionOrNull()
        ?: run {
          ctx.redirect("/books/${ resource.id.value }")
          return@transaction
        }
    val seriesOptions = Series.all().toList()
    render(ctx = ctx, template = "edit", model = mapOf("resource" to resource, "seriesOptions" to seriesOptions))
  }

  fun bookImage(ctx: Context) {
    val resource = transaction { ctx.getResourceOrNull() }
    val errorImage = javaClass.getResource("/static/img/placeholder.webp")?.toPath() ?: throw BadRequestResponse()
    if (resource == null) {
      ctx.contentType("image/webp")
      ctx.result(errorImage.readBytes())
      return
    }
    var cachedImage = Utils.CACHE_ROOT / "covers" / "${resource.id.value}.webp"
    if (cachedImage.exists()) {
      ctx.contentType("image/webp")
      ctx.result(cachedImage.readBytes())
      return
    }
    cachedImage.createParentDirectories()
    try {
      var image = ImmutableImage.loader().fromUrl(resource.imageUrl.toUrl())
      image = image.cover(640, 960)
      image.output(WRITER, cachedImage.toFile())
    } catch (ioe: IOException) {
      LOGGER.error(ioe) { "Error fetching image for book ${resource.id.value}" }
      cachedImage = errorImage
    }
    ctx.contentType("image/webp")
    ctx.result(cachedImage.readBytes())
  }
}

sealed interface SearchResult {
  @JvmInline value class Local(val value: Book) : SearchResult

  @JvmInline value class BookCatalogue(val value: ResponseData) : SearchResult

  @JvmInline value class OpenLibrary(val value: Edition) : SearchResult
}

data class AddBookRequest(val isbn: String, val wish: Boolean = false, val collect: Boolean = false)

data class UpdateBookStateRequest(
  val wish: Boolean? = null,
  val collect: Boolean? = null,
  val read: Boolean? = null,
  val readDate: String? = null,
)

data class UpdateBookRequest(
  val format: String?,
  val isbn: String,
  val publishDate: String?,
  val publisher: String?,
  val summary: String?,
  val title: String,
  val readDate: String? = null,
  val series: List<SeriesRequest> = emptyList(),
) {
  data class SeriesRequest(val seriesId: Long, val index: String, val remove: Boolean = false)
}
