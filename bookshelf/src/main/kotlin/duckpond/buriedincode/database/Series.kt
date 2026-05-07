package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class Series(id: EntityID<Long>) : LongEntity(id), Comparable<Series> {
  companion object : LongEntityClass<Series>(SeriesTable) {
    val comparator = compareBy(Series::title)

    fun findOrNull(title: String): Series? {
      return find { SeriesTable.titleCol eq title }.firstOrNull()
    }
  }

  var title: String by SeriesTable.titleCol

  var lastUpdated: Instant by SeriesTable.lastUpdatedCol

  val books by BookSeries referrersOn BookSeriesTable.seriesCol

  override fun compareTo(other: Series): Int = comparator.compare(this, other)
}

@OptIn(ExperimentalTime::class)
object SeriesTable : LongIdTable(name = "series") {
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val titleCol: Column<String> = text(name = "title").uniqueIndex()

  init {
    transaction { SchemaUtils.create(this) }
  }
}
