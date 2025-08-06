package github.buriedincode.routers.api

import github.buriedincode.Utils.asEnumOrNull
import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.BookInput
import github.buriedincode.models.BookSeries
import github.buriedincode.models.Creator
import github.buriedincode.models.Credit
import github.buriedincode.models.Format
import github.buriedincode.models.IdInput
import github.buriedincode.models.ImportBook
import github.buriedincode.models.Publisher
import github.buriedincode.models.ReadBook
import github.buriedincode.models.Role
import github.buriedincode.models.Series
import github.buriedincode.models.User
import github.buriedincode.openlibrary.schemas.Edition
import github.buriedincode.openlibrary.schemas.Work
import github.buriedincode.services.OpenLibrary
import github.buriedincode.services.getId
import github.buriedincode.tables.BookSeriesTable
import github.buriedincode.tables.BookTable
import github.buriedincode.tables.CreditTable
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

object BookApiRouter : BaseApiRouter<Book>(entity = Book) {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun list(ctx: Context): Unit = transaction {
    val query = BookTable.selectAll()
    ctx.queryParam("creator-id")?.toLongOrNull()?.let {
      Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
    }
    ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format -> query.andWhere { BookTable.formatCol eq format } }
    ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull()?.let { isCollected ->
      query.andWhere { BookTable.isCollectedCol eq isCollected }
    }
    ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
      Publisher.findById(it)?.let { publisher -> query.andWhere { BookTable.publisherCol eq publisher.id } }
    }
    ctx.queryParam("series-id")?.toLongOrNull()?.let {
      Series.findById(it)?.let { series -> query.andWhere { BookSeriesTable.seriesCol eq series.id } }
    }
    ctx.queryParam("title")?.let { title ->
      query.andWhere { (BookTable.titleCol like "%$title%") or (BookTable.subtitleCol like "%$title%") }
    }
    ctx.json(Book.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
  }

  override fun create(ctx: Context) =
    ctx.processInput<BookInput> { body ->
      transaction {
        Book.find(body.title, body.subtitle, body.identifiers?.isbn, body.identifiers?.openLibrary)?.let {
          throw ConflictResponse("Book already exists")
        }
        val resource =
          Book.findOrCreate(body.title, body.subtitle, body.identifiers?.isbn, body.identifiers?.openLibrary).apply {
            body.credits.forEach {
              Credit.new {
                this.book = this@apply
                this.creator = Creator.findById(it.creator) ?: throw NotFoundResponse("Creator not found.")
                this.role = Role.findById(it.role) ?: throw NotFoundResponse("Role not found.")
              }
            }
            format = body.format
            goodreads = body.identifiers?.goodreads
            googleBooks = body.identifiers?.googleBooks
            imageUrl = body.imageUrl
            isCollected = body.isCollected
            libraryThing = body.identifiers?.libraryThing
            publishDate = body.publishDate
            publisher = body.publisher?.let { Publisher.findById(it) ?: throw NotFoundResponse("Publisher not found.") }
            body.readers.forEach {
              ReadBook.new {
                this.book = this@apply
                this.user = User.findById(it.user) ?: throw NotFoundResponse("User not found.")
                this.readDate = it.readDate
              }
            }
            body.series.forEach {
              BookSeries.new {
                this.book = this@apply
                this.series = Series.findById(it.series) ?: throw NotFoundResponse("Series not found.")
                this.number = it.number
              }
            }
            summary = body.summary
            wishers =
              SizedCollection(body.wishers.map { User.findById(it) ?: throw NotFoundResponse("User not found.") })
          }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
      }
    }

  override fun update(ctx: Context) =
    manage<BookInput>(ctx) { body, book ->
      Book.find(body.title, body.subtitle, body.identifiers?.isbn, body.identifiers?.openLibrary)
        ?.takeIf { it != book }
        ?.let { throw ConflictResponse("Book already exists") }

      book.apply {
        credits.forEach { it.delete() }
        body.credits.forEach {
          Credit.findOrCreate(
            this,
            Creator.findById(it.creator) ?: throw NotFoundResponse("Creator not found."),
            Role.findById(it.role) ?: throw NotFoundResponse("Role not found."),
          )
        }
        format = body.format
        goodreads = body.identifiers?.goodreads
        googleBooks = body.identifiers?.googleBooks
        imageUrl = body.imageUrl
        isbn = body.identifiers?.isbn
        isCollected = body.isCollected
        libraryThing = body.identifiers?.libraryThing
        openLibrary = body.identifiers?.openLibrary
        publishDate = body.publishDate
        publisher = body.publisher?.let { Publisher.findById(it) ?: throw NotFoundResponse("Publisher not found.") }
        readers.forEach { it.delete() }
        body.readers.forEach {
          ReadBook.findOrCreate(this, User.findById(it.user) ?: throw NotFoundResponse("User not found.")).apply {
            readDate = it.readDate
          }
        }
        series.forEach { it.delete() }
        body.series.forEach {
          BookSeries.findOrCreate(this, Series.findById(it.series) ?: throw NotFoundResponse("Series not found."))
            .apply { number = it.number }
        }
        subtitle = body.subtitle
        summary = body.summary
        title = body.title
        wishers = SizedCollection(body.wishers.map { User.findById(it) ?: throw NotFoundResponse("User not found.") })
      }
    }

  override fun delete(ctx: Context) = transaction {
    ctx.getResource().apply {
      credits.forEach { it.delete() }
      readers.forEach { it.delete() }
      series.forEach { it.delete() }
      delete()
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  private fun manageStatus(ctx: Context, block: (Book) -> Unit) = transaction {
    val resource = ctx.getResource()
    block(resource)
    ctx.json(resource.toJson(showAll = true))
  }

  fun collectBook(ctx: Context) =
    manageStatus(ctx) { resource ->
      resource.isCollected = true
      resource.wishers = SizedCollection()
    }

  fun discardBook(ctx: Context) =
    manageStatus(ctx) { resource ->
      resource.isCollected = false
      resource.readers.forEach { it.delete() }
    }

  fun addReader(ctx: Context) =
    manage<BookInput.Reader>(ctx) { body, book ->
      val user = User.findById(body.user) ?: throw NotFoundResponse("User not found.")
      if (!book.isCollected) {
        throw BadRequestResponse("Book hasn't been collected")
      }
      ReadBook.findOrCreate(book, user).apply { readDate = body.readDate }
    }

  fun removeReader(ctx: Context) =
    manage<IdInput>(ctx) { body, book ->
      val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
      if (!book.isCollected) {
        throw BadRequestResponse("Book hasn't been collected")
      }
      ReadBook.find(book, user)?.delete()
    }

  fun addWisher(ctx: Context) =
    manage<IdInput>(ctx) { body, book ->
      val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
      if (book.isCollected) {
        throw BadRequestResponse("Book has been collected")
      }
      book.wishers = SizedCollection(book.wishers + user)
    }

  fun removeWisher(ctx: Context) =
    manage<IdInput>(ctx) { body, book ->
      val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
      if (book.isCollected) {
        throw BadRequestResponse("Book has been collected")
      }
      book.wishers = SizedCollection(book.wishers - user)
    }

  fun addCredit(ctx: Context) =
    manage<BookInput.Credit>(ctx) { body, book ->
      Credit.findOrCreate(
        book,
        Creator.findById(body.creator) ?: throw NotFoundResponse("Creator not found."),
        Role.findById(body.role) ?: throw NotFoundResponse("Role not found."),
      )
    }

  fun removeCredit(ctx: Context) =
    manage<BookInput.Credit>(ctx) { body, book ->
      Credit.find(
          book,
          Creator.findById(body.creator) ?: throw NotFoundResponse("Creator not found."),
          Role.findById(body.role) ?: throw NotFoundResponse("Role not found."),
        )
        ?.delete()
    }

  fun addSeries(ctx: Context) =
    manage<BookInput.Series>(ctx) { body, book ->
      BookSeries.findOrCreate(book, Series.findById(body.series) ?: throw NotFoundResponse("Series not found.")).apply {
        number = if (body.number == 0) null else body.number
      }
    }

  fun removeSeries(ctx: Context) =
    manage<IdInput>(ctx) { body, book ->
      BookSeries.find(book, Series.findById(body.id) ?: throw NotFoundResponse("Series not found."))?.delete()
    }

  private fun Book.applyOpenLibrary(edition: Edition, work: Work): Book =
    this.apply {
      var format = edition.physicalFormat?.asEnumOrNull<Format>()
      if (edition.physicalFormat.equals("Hardback", ignoreCase = true)) {
        format = Format.HARDCOVER
      } else if (edition.physicalFormat.equals("Mass Market Paperback", ignoreCase = true)) {
        format = Format.PAPERBACK
      } else if (format == null) {
        LOGGER.warn { "Unmapped Format: ${edition.physicalFormat}" }
      }

      this.format = format ?: Format.PAPERBACK
      goodreads = edition.identifiers?.goodreads?.firstOrNull()
      googleBooks = edition.identifiers?.google?.firstOrNull()
      imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.getId()}-L.jpg"
      isbn = isbn ?: edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull()
      libraryThing = edition.identifiers?.librarything?.firstOrNull()
      openLibrary = edition.getId()
      publishDate = edition.publishDate
      publisher = edition.publishers.firstOrNull()?.let { Publisher.findOrCreate(it) }
      summary = edition.description ?: work.description

      credits.forEach { it.delete() }
      work.authors
        .map { OpenLibrary.getAuthor(it.author.getId()) }
        .map {
          Creator.findOrCreate(it.name).apply {
            it.photos.firstOrNull()?.let { imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg" }
          }
        }
        .forEach { Credit.findOrCreate(this, it, Role.findOrCreate("Author")) }
      edition.contributors.forEach {
        Credit.findOrCreate(this, Creator.findOrCreate(it.name), Role.findOrCreate(it.role))
      }
    }

  fun search(ctx: Context) =
    ctx.processInput<BookInput> { body ->
      val results = OpenLibrary.search(title = body.title)
      ctx.json(results)
    }

  fun import(ctx: Context) =
    ctx.processInput<ImportBook> { body ->
      transaction {
        val edition =
          body.isbn?.let { OpenLibrary.getEditionByISBN(it) }
            ?: body.openLibraryId?.let { OpenLibrary.getEdition(it) }
            ?: throw NotImplementedResponse("Import only supports OpenLibrary currently")
        val work = OpenLibrary.getWork(edition.works.first().getId())

        Book.find(
            edition.title,
            edition.subtitle,
            edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull(),
            edition.getId(),
          )
          ?.let { throw ConflictResponse("Book already exists") }
        val resource =
          Book.findOrCreate(
              edition.title,
              edition.subtitle,
              edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull(),
              edition.getId(),
            )
            .applyOpenLibrary(edition, work)
            .apply { isCollected = body.isCollected }

        ctx.json(resource.toJson(showAll = true))
      }
    }

  fun reimport(ctx: Context) = transaction {
    val resource = ctx.getResource()
    val edition =
      resource.isbn?.let { OpenLibrary.getEditionByISBN(it) }
        ?: resource.openLibrary?.let { OpenLibrary.getEdition(it) }
        ?: throw NotImplementedResponse("Import only supports OpenLibrary currently")
    val work = OpenLibrary.getWork(edition.works.first().getId())

    Book.find(
        edition.title,
        edition.subtitle,
        edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull(),
        edition.getId(),
      )
      ?.takeIf { it != resource }
      ?.let { throw ConflictResponse("Book already exists") }
    resource.applyOpenLibrary(edition, work)

    ctx.json(resource.toJson(showAll = true))
  }
}
