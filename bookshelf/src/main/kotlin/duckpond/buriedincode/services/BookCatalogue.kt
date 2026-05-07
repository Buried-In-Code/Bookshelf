package duckpond.buriedincode.services

import duckpond.buriedincode.bookcatalogue.schemas.Author as BCAuthor
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData.Amazon
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData.BookCatalogue
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData.Goodreads
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData.Google
import duckpond.buriedincode.bookcatalogue.schemas.ResponseData.OpenLibrary
import duckpond.buriedincode.bookcatalogue.schemas.Series as BCSeries
import duckpond.buriedincode.database.Author
import duckpond.buriedincode.database.Book
import duckpond.buriedincode.database.BookAuthor
import duckpond.buriedincode.database.BookAuthorTable
import duckpond.buriedincode.database.BookSeries
import duckpond.buriedincode.database.BookSeriesTable
import duckpond.buriedincode.database.Series
import duckpond.buriedincode.transaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq

fun BCAuthor.toAuthor(): Author = transaction { Author.new { this@new.name = this@toAuthor.fullName } }

fun BCSeries.toSeries(): Series = transaction { Series.new { this@new.title = this@toSeries.seriesName } }

fun BookCatalogue.Book.toBook(): Book = transaction {
  Book.new {
    this@new.format = this@toBook.format
    this@new.imageUrl = this@toBook.defaultThumbnail
    this@new.isbn = this@toBook.isbn
    this@new.publishDate = this@toBook.datePublished?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
    this@new.publisher = this@toBook.publisher
    this@new.summary = this@toBook.description
    this@new.title = this@toBook.title
  }
}

fun BookCatalogue.Book.Author.toAuthor(): Author = transaction {
  Author.new { this@new.name = this@toAuthor.givenNames + " " + this@toAuthor.familyName }
}

fun BookCatalogue.convert(): Book = transaction {
  val localBook = Book.findOrNull(isbn = this.book!!.isbn) ?: this.book!!.toBook()
  this.book!!.authors.forEach {
    val localAuthor = Author.findOrNull(name = it.givenNames + " " + it.familyName) ?: it.toAuthor()
    val authorId = CompositeID {
      it[BookAuthorTable.authorCol] = localAuthor.id
      it[BookAuthorTable.bookCol] = localBook.id
    }
    if (BookAuthor.findById(authorId) == null) {
      BookAuthor.new(authorId) {}
    }
  }
  this.book!!.series.forEach {
    val localSeries = Series.findOrNull(title = it.seriesName) ?: it.toSeries()
    if (
      BookSeries.find {
          (BookSeriesTable.bookCol eq localBook.id) and
            (BookSeriesTable.seriesCol eq localSeries.id) and
            (BookSeriesTable.indexCol eq it.seriesNum)
        }
        .firstOrNull() == null
    ) {
      BookSeries.new {
        this.book = localBook
        this.series = localSeries
        this.index = it.seriesNum
      }
    }
  }
  localBook
}

fun Google.Book.toBook(): Book = transaction {
  Book.new {
    this@new.format = this@toBook.format
    this@new.imageUrl = this@toBook.defaultThumbnail
    this@new.isbn = this@toBook.isbn
    this@new.publishDate = this@toBook.datePublished
    this@new.publisher = this@toBook.publisher
    this@new.summary = this@toBook.description
    this@new.title = this@toBook.title
  }
}

fun Google.convert(): Book = transaction {
  val localBook = Book.findOrNull(isbn = this.book!!.isbn) ?: this.book!!.toBook()
  this.book!!.authors.forEach {
    val localAuthor = Author.findOrNull(name = it.fullName) ?: it.toAuthor()
    val authorId = CompositeID {
      it[BookAuthorTable.authorCol] = localAuthor.id
      it[BookAuthorTable.bookCol] = localBook.id
    }
    if (BookAuthor.findById(authorId) == null) {
      BookAuthor.new(authorId) {}
    }
  }
  this.book!!.series.forEach {
    val localSeries = Series.findOrNull(title = it.seriesName) ?: it.toSeries()
    if (
      BookSeries.find {
          (BookSeriesTable.bookCol eq localBook.id) and
            (BookSeriesTable.seriesCol eq localSeries.id) and
            (BookSeriesTable.indexCol eq it.seriesNum)
        }
        .firstOrNull() == null
    ) {
      BookSeries.new {
        this.book = localBook
        this.series = localSeries
        this.index = it.seriesNum
      }
    }
  }
  localBook
}

fun Goodreads.Book.toBook(): Book = transaction {
  Book.new {
    this@new.format = this@toBook.format
    this@new.imageUrl = this@toBook.defaultThumbnail
    this@new.isbn = this@toBook.isbn
    // this@new.publishDate = this@toBook.datePublished
    this@new.publisher = this@toBook.publisher
    this@new.summary = this@toBook.description
    this@new.title = this@toBook.title
  }
}

fun Goodreads.convert(): Book = transaction {
  val localBook = Book.findOrNull(isbn = this.book!!.isbn) ?: this.book!!.toBook()
  this.book!!.authors.forEach {
    val localAuthor = Author.findOrNull(name = it.fullName) ?: it.toAuthor()
    val authorId = CompositeID {
      it[BookAuthorTable.authorCol] = localAuthor.id
      it[BookAuthorTable.bookCol] = localBook.id
    }
    if (BookAuthor.findById(authorId) == null) {
      BookAuthor.new(authorId) {}
    }
  }
  this.book!!.series.forEach {
    val localSeries = Series.findOrNull(title = it.seriesName) ?: it.toSeries()
    if (
      BookSeries.find {
          (BookSeriesTable.bookCol eq localBook.id) and
            (BookSeriesTable.seriesCol eq localSeries.id) and
            (BookSeriesTable.indexCol eq it.seriesNum)
        }
        .firstOrNull() == null
    ) {
      BookSeries.new {
        this.book = localBook
        this.series = localSeries
        this.index = it.seriesNum
      }
    }
  }
  localBook
}

fun OpenLibrary.Book.toBook(): Book = transaction {
  Book.new {
    this@new.format = this@toBook.format
    this@new.imageUrl = this@toBook.defaultThumbnail
    this@new.isbn = this@toBook.isbn
    this@new.publishDate = this@toBook.datePublished
    this@new.publisher = this@toBook.publisher
    this@new.summary = this@toBook.description
    this@new.title = this@toBook.title
  }
}

fun OpenLibrary.convert(): Book = transaction {
  val localBook = Book.findOrNull(isbn = this.book!!.isbn) ?: this.book!!.toBook()
  this.book!!.authors.forEach {
    val localAuthor = Author.findOrNull(name = it.fullName) ?: it.toAuthor()
    val authorId = CompositeID {
      it[BookAuthorTable.authorCol] = localAuthor.id
      it[BookAuthorTable.bookCol] = localBook.id
    }
    if (BookAuthor.findById(authorId) == null) {
      BookAuthor.new(authorId) {}
    }
  }
  this.book!!.series.forEach {
    val localSeries = Series.findOrNull(title = it.seriesName) ?: it.toSeries()
    if (
      BookSeries.find {
          (BookSeriesTable.bookCol eq localBook.id) and
            (BookSeriesTable.seriesCol eq localSeries.id) and
            (BookSeriesTable.indexCol eq it.seriesNum)
        }
        .firstOrNull() == null
    ) {
      BookSeries.new {
        this.book = localBook
        this.series = localSeries
        this.index = it.seriesNum
      }
    }
  }
  localBook
}

fun Amazon.Book.toBook(): Book = TODO("Not yet implemented")

fun Amazon.convert(): Book = TODO("Not yet implemented")
