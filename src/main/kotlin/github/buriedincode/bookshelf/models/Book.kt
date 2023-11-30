package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.DATE_FORMATTER
import github.buriedincode.bookshelf.tables.BookGenreTable
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.WishedTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class Book(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Book> {
    companion object : LongEntityClass<Book>(BookTable), Logging {
        val comparator = compareBy<Book> { it.series.firstOrNull()?.series }
            .thenBy { it.series.firstOrNull()?.number ?: Int.MAX_VALUE }
            .thenBy(Book::title)
            .thenBy(nullsFirst(), Book::subtitle)
    }

    val credits by Credit referrersOn CreditTable.bookCol
    var description: String? by BookTable.descriptionCol
    var format: Format by BookTable.formatCol
    var genres by Genre via BookGenreTable
    var goodreadsId: String? by BookTable.goodreadsCol
    var googleBooksId: String? by BookTable.googleBooksCol
    var imageUrl: String? by BookTable.imageUrlCol
    var isbn: String? by BookTable.isbnCol
    var isCollected: Boolean by BookTable.isCollectedCol
    var libraryThingId: String? by BookTable.libraryThingCol
    var openLibraryId: String? by BookTable.openLibraryCol
    var publishDate: LocalDate? by BookTable.publishDateCol
    var publisher: Publisher? by Publisher optionalReferencedOn BookTable.publisherCol
    val readers by ReadBook referrersOn ReadBookTable.bookCol
    val series by BookSeries referrersOn BookSeriesTable.bookCol
    var subtitle: String? by BookTable.subtitleCol
    var title: String by BookTable.titleCol
    var wishers by User via WishedTable

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "description" to description,
            "format" to format.name,
            "goodreadsId" to goodreadsId,
            "googleBooksId" to googleBooksId,
            "imageUrl" to imageUrl,
            "isbn" to isbn,
            "isCollected" to isCollected,
            "libraryThingId" to libraryThingId,
            "openLibraryId" to openLibraryId,
            "publishDate" to publishDate?.format(DATE_FORMATTER),
            "subtitle" to subtitle,
            "title" to title,
        )
        if (showAll) {
            output["credits"] = credits.sortedWith(
                compareBy<Credit> { it.creator }.thenBy { it.role },
            ).map {
                mapOf(
                    "creator" to it.creator.toJson(),
                    "role" to it.role.toJson(),
                )
            }
            output["genres"] = genres.sorted().map { it.toJson() }
            output["publisher"] = publisher?.toJson()
            output["readers"] = readers.sortedWith(
                compareBy<ReadBook> { it.user }.thenBy { it.readDate ?: LocalDate.of(2000, 1, 1) },
            ).map {
                mapOf(
                    "readDate" to it.readDate?.format(DATE_FORMATTER),
                    "user" to it.user.toJson(),
                )
            }
            output["series"] = series.sortedWith(
                compareBy<BookSeries> { it.series }.thenBy { it.number ?: Int.MAX_VALUE },
            ).map {
                mapOf(
                    "number" to it.number,
                    "series" to it.series.toJson(),
                )
            }
            output["wishers"] = wishers.sorted().map { it.toJson() }
        } else {
            output["publisherId"] = publisher?.id?.value
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Book): Int = comparator.compare(this, other)
}
