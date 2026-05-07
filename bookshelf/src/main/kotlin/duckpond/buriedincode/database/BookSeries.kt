package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class BookSeries(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<BookSeries>(BookSeriesTable)

  var book: Book by Book referencedOn BookSeriesTable.bookCol
  var index: String by BookSeriesTable.indexCol
  var series: Series by Series referencedOn BookSeriesTable.seriesCol

  var lastUpdated: Instant by BookSeriesTable.lastUpdatedCol
}

@OptIn(ExperimentalTime::class)
object BookSeriesTable : LongIdTable(name = "book_series") {
  val bookCol: Column<EntityID<Long>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val indexCol: Column<String> = text(name = "index")
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val seriesCol: Column<EntityID<Long>> =
    reference(
      name = "series_id",
      foreign = SeriesTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )

  init {
    transaction {
      uniqueIndex(indexCol, bookCol, seriesCol)
      SchemaUtils.create(this)
    }
  }
}
