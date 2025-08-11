package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.toString
import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.openlibrary.ServiceException
import github.buriedincode.openlibrary.schemas.id
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

@OptIn(ExperimentalTime::class)
class Book(id: EntityID<String>) : Entity<String>(id), IJson, Comparable<Book> {
  companion object : EntityClass<String, Book>(BookTable) {
    @JvmStatic private val LOGGER = KotlinLogging.logger {}

    val comparator = compareBy(Book::title).thenBy(nullsLast(), Book::subtitle)

    fun openLibraryImport(openLibraryId: String): Book? {
      try {
        val edition = Utils.openLibrary.getEdition(openLibraryId)
        val work = Utils.openLibrary.getWork(edition.works.first().id)
        return new(id = edition.id) {
          this.format = edition.physicalFormat
          this.goodreadsId = edition.identifiers?.goodreads?.firstOrNull()
          this.googleBooksId = edition.identifiers?.google?.firstOrNull()
          this.imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.id}-L.jpg"
          this.isbn10 = edition.isbn10.firstOrNull()
          this.isbn13 = edition.isbn13.firstOrNull()
          this.libraryThingId = edition.identifiers?.librarything?.firstOrNull()
          this.publishDate = edition.publishDate
          this.publisher = edition.publishers.firstOrNull()
          this.subtitle = edition.subtitle
          this.summary = edition.description ?: work.description
          this.title = edition.title
        }
      } catch (se: ServiceException) {
        LOGGER.error(se) { "" }
        return null
      }
    }
  }

  var format: String? by BookTable.formatCol
  var goodreadsId: String? by BookTable.goodreadsIdCol
  var googleBooksId: String? by BookTable.googleBooksIdCol
  var imageUrl: String by BookTable.imageUrlCol
  var isbn10: String? by BookTable.isbn10Col
  var isbn13: String? by BookTable.isbn13Col
  var lastUpdated: Instant by BookTable.lastUpdatedCol
  var libraryThingId: String? by BookTable.libraryThingIdCol
  var publishDate: LocalDate? by BookTable.publishDateCol
  var publisher: String? by BookTable.publisherCol
  var subtitle: String? by BookTable.subtitleCol
  var summary: String? by BookTable.summaryCol
  var title: String by BookTable.titleCol

  // var credits by Credit referrersOn CreditTable.bookCol
  // val series by BookSeries referrersOn BookSeriesTable.bookCol

  val collectors by Collected referrersOn CollectedTable.bookCol
  val readers by Read referrersOn ReadTable.bookCol
  val wishers by Wished referrersOn WishedTable.bookCol

  fun openLibraryImport(): Book {
    try {
      val edition = Utils.openLibrary.getEdition(this.id.value)
      val work = Utils.openLibrary.getWork(edition.works.first().id)

      this.format = edition.physicalFormat
      this.goodreadsId = edition.identifiers?.goodreads?.firstOrNull()
      this.googleBooksId = edition.identifiers?.google?.firstOrNull()
      this.imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.id}-L.jpg"
      this.isbn10 = edition.isbn10.firstOrNull()
      this.isbn13 = edition.isbn13.firstOrNull()
      this.libraryThingId = edition.identifiers?.librarything?.firstOrNull()
      this.publishDate = edition.publishDate
      this.publisher = edition.publishers.firstOrNull()
      this.subtitle = edition.subtitle
      this.summary = edition.description ?: work.description
      this.title = edition.title
    } catch (se: ServiceException) {
      LOGGER.error(se) { "" }
    }
    return this
  }

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>(
        "id" to id.value,
        "format" to format,
        "goodreadsId" to goodreadsId,
        "googleBooksId" to googleBooksId,
        "imageUrl" to imageUrl,
        "isbn10" to isbn10,
        "isbn13" to isbn13,
        "lastUpdated" to lastUpdated.toString(),
        "libraryThingId" to libraryThingId,
        "publishDate" to publishDate?.toString(),
        "publisher" to publisher,
        "subtitle" to subtitle,
        "title" to title,
      )
      .apply {
        if (showAll) {
          put("summary", summary)
          put("collectors", collectors.sortedWith(compareBy(Collected::date, Collected::user)).map { it.user.id.value })
          put("readers", readers.sortedWith(compareBy(Read::date, Read::user)).map { it.user.id.value })
          put("wishers", wishers.sortedWith(compareBy(Wished::date, Wished::user)).map { it.user.id.value })
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: Book): Int = comparator.compare(this, other)
}

@OptIn(ExperimentalTime::class)
object BookTable : IdTable<String>(name = "books") {
  override val id: Column<EntityID<String>> = text(name = "id").entityId()
  override val primaryKey: PrimaryKey = PrimaryKey(id)

  val formatCol: Column<String?> = text(name = "format").nullable()
  val goodreadsIdCol: Column<String?> = text(name = "goodreads_id").nullable().uniqueIndex()
  val googleBooksIdCol: Column<String?> = text(name = "google_books_id").nullable().uniqueIndex()
  val imageUrlCol: Column<String> = text(name = "image_url")
  val isbn10Col: Column<String?> = text(name = "isbn_10").nullable().uniqueIndex()
  val isbn13Col: Column<String?> = text(name = "isbn_13").nullable().uniqueIndex()
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").clientDefault { Clock.System.now().minus(25, DateTimeUnit.HOUR) }
  val libraryThingIdCol: Column<String?> = text(name = "library_thing_id").nullable().uniqueIndex()
  val publishDateCol: Column<LocalDate?> = date(name = "publish_date").nullable()
  val publisherCol: Column<String?> = text(name = "publisher").nullable()
  val subtitleCol: Column<String?> = text(name = "subtitle").nullable()
  val summaryCol: Column<String?> = text(name = "summary").nullable()
  val titleCol: Column<String> = text(name = "title")

  init {
    transaction {
      uniqueIndex(titleCol, subtitleCol)
      SchemaUtils.create(this)
    }
  }
}
