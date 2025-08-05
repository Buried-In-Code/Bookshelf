package github.buriedincode.tables

import github.buriedincode.Utils.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils

object BookSeriesTable : LongIdTable(name = "books__series") {
  val bookCol: Column<EntityID<Long>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val seriesCol: Column<EntityID<Long>> =
    reference(
      name = "series_id",
      foreign = SeriesTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val numberCol: Column<Int?> = integer(name = "number").nullable()

  init {
    transaction {
      uniqueIndex(bookCol, seriesCol)
      SchemaUtils.create(this)
    }
  }
}
