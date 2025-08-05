package github.buriedincode.models

import github.buriedincode.tables.BookSeriesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class BookSeries(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<BookSeries>(BookSeriesTable) {
    fun find(book: Book, series: Series): BookSeries? {
      return BookSeries.find { (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id) }
        .firstOrNull()
    }

    fun findOrCreate(book: Book, series: Series): BookSeries {
      return find(book, series)
        ?: BookSeries.new {
          this.book = book
          this.series = series
        }
    }
  }

  var book: Book by Book referencedOn BookSeriesTable.bookCol
  var series: Series by Series referencedOn BookSeriesTable.seriesCol
  var number: Int? by BookSeriesTable.numberCol
}
