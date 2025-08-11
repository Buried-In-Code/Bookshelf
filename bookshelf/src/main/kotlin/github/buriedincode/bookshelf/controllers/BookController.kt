package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Collected
import github.buriedincode.bookshelf.models.CollectedTable
import github.buriedincode.bookshelf.models.Read
import github.buriedincode.bookshelf.models.ReadTable
import github.buriedincode.bookshelf.models.Wished
import github.buriedincode.bookshelf.models.WishedTable
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.CompositeID

object BookController : StringController<Book>(entity = Book) {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun listPage(ctx: Context) = transaction {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
      return@transaction
    }
    val query = ctx.queryParam("q")
    val results =
      try {
        if (query != null && query.startsWith("OL")) {
          val edition = Utils.openLibrary.getEdition(id = query)
          listOf(BookResult(edition = edition, isLocal = Book.findById(id = edition.id) != null))
        } else {
          (query?.let { Utils.openLibrary.searchWork(params = mapOf("title" to query)) } ?: emptyList()).mapNotNull {
            it.coverEditionKey?.split("/")?.last()?.let { editionId ->
              val edition = Utils.openLibrary.getEdition(id = editionId)
              BookResult(edition = edition, isLocal = Book.findById(id = edition.id) != null)
            }
          }
        }
      } catch (se: ServiceException) {
        LOGGER.error(se) { "Error fetching book results for query: $query" }
        emptyList()
      }
    render(ctx, "list", mapOf("query" to query, "results" to results))
  }

  @OptIn(ExperimentalTime::class)
  override fun readPage(ctx: Context) = transaction {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
      return@transaction
    }
    try {
      ctx.getResource()
    } catch (_: NotFoundResponse) {
      Book.openLibraryImport(ctx.pathParam(this.paramName))?.let {
        it.lastUpdated = Clock.System.now().minus(25, DateTimeUnit.HOUR)
        it
      } ?: throw BadRequestResponse()
    }
    super.readPage(ctx = ctx)
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

  @OptIn(ExperimentalTime::class)
  fun toggleCollected(ctx: Context) = transaction {
    val session = ctx.getSession() ?: throw UnauthorizedResponse()
    val resource = ctx.getResource()
    val collectedId = CompositeID {
      it[CollectedTable.bookCol] = resource.id
      it[CollectedTable.userCol] = session.id
    }
    val exists = Collected.findById(collectedId)
    if (exists == null) {
      Collected.new(collectedId) {
        this.date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
      }
    } else {
      exists.delete()
    }
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
