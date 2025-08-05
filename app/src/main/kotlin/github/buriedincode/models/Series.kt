package github.buriedincode.models

import github.buriedincode.tables.BookSeriesTable
import github.buriedincode.tables.SeriesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Series(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Series> {
  companion object : LongEntityClass<Series>(SeriesTable) {
    val comparator = compareBy(Series::title)

    fun find(title: String): Series? {
      return Series.find { SeriesTable.titleCol eq title }.firstOrNull()
    }

    fun findOrCreate(title: String): Series {
      return find(title) ?: Series.new { this.title = title }
    }
  }

  val books by BookSeries referrersOn BookSeriesTable.seriesCol
  var title: String by SeriesTable.titleCol

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>("id" to id.value, "title" to title)
      .apply {
        if (showAll) {
          put("books", books.sortedBy { it.book }.map { mapOf("book" to it.book.toJson(), "number" to it.number) })
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: Series): Int = comparator.compare(this, other)
}

data class SeriesInput(val books: List<Book> = emptyList(), val title: String) {
  data class Book(val book: Long, val number: Int? = null)
}
