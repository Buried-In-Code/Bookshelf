package github.buriedincode.tables

import github.buriedincode.Utils.transaction
import github.buriedincode.models.Format
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.kotlin.datetime.date

object BookTable : LongIdTable(name = "books") {
  val formatCol: Column<Format> =
    enumerationByName(name = "format", length = 16, klass = Format::class).default(defaultValue = Format.PAPERBACK)
  val goodreadsCol: Column<String?> = text(name = "goodreads_id").nullable()
  val googleBooksCol: Column<String?> = text(name = "google_books_id").nullable()
  val imageUrlCol: Column<String?> = text(name = "image_url").nullable()
  val isCollectedCol: Column<Boolean> = bool(name = "is_collected").default(defaultValue = false)
  val isbnCol: Column<String?> = text(name = "isbn").nullable().uniqueIndex()
  val libraryThingCol: Column<String?> = text(name = "library_thing_id").nullable()
  val openLibraryCol: Column<String?> = text(name = "open_library_id").nullable().uniqueIndex()
  val publishDateCol: Column<LocalDate?> = date(name = "publish_date").nullable()
  val publisherCol: Column<EntityID<Long>?> =
    optReference(
      name = "publisher_id",
      foreign = PublisherTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
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
