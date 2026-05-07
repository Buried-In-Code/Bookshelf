package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class Book(id: EntityID<Long>) : LongEntity(id), Comparable<Book> {
  companion object : LongEntityClass<Book>(BookTable) {
    val comparator =
      compareBy<Book, Series?>(nullsFirst()) { it.series.firstOrNull()?.series }
        .thenBy { it.series.firstOrNull()?.index?.toIntOrNull() ?: Int.MAX_VALUE }
        .thenBy { it.title }

    fun findOrNull(isbn: String): Book? {
      return find { BookTable.isbnCol eq isbn }.firstOrNull()
    }
  }

  var format: String? by BookTable.formatCol
  var imageUrl: String by BookTable.imageUrlCol
  var isbn: String by BookTable.isbnCol
  var publishDate: LocalDate? by BookTable.publishDateCol
  var publisher: String? by BookTable.publisherCol
  var summary: String? by BookTable.summaryCol
  var title: String by BookTable.titleCol

  var isCollected: Boolean by BookTable.isCollectedCol
  var lastUpdated: Instant by BookTable.lastUpdatedCol

  val authors by BookAuthor referrersOn BookAuthorTable.bookCol
  val readers by ReadBook referrersOn ReadBookTable.bookCol
  val series by BookSeries referrersOn BookSeriesTable.bookCol
  val wishers by WishedBook referrersOn WishedBookTable.bookCol

  override fun compareTo(other: Book): Int = comparator.compare(this, other)
}

@OptIn(ExperimentalTime::class)
object BookTable : LongIdTable(name = "books") {
  val formatCol: Column<String?> = text(name = "format").nullable()
  val imageUrlCol: Column<String> = text(name = "image_url")
  val isbnCol: Column<String> = text(name = "isbn").uniqueIndex()
  val isCollectedCol: Column<Boolean> = bool(name = "is_collected").default(false)
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val publishDateCol: Column<LocalDate?> = date(name = "publish_date").nullable()
  val publisherCol: Column<String?> = text(name = "publisher").nullable()
  val summaryCol: Column<String?> = text(name = "summary").nullable()
  val titleCol: Column<String> = text(name = "title")

  init {
    transaction { SchemaUtils.create(this) }
  }
}
