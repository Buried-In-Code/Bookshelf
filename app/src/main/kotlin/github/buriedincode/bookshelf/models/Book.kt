package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import github.buriedincode.bookshelf.Utils.toString
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.WishedTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Book(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Book> {
    companion object : LongEntityClass<Book>(BookTable) {
        val comparator = compareBy<Book> { it.series.firstOrNull()?.series }
            .thenBy { it.series.firstOrNull()?.number ?: Int.MAX_VALUE }
            .thenBy(Book::title)
            .thenBy(nullsFirst(), Book::subtitle)

        fun find(title: String, subtitle: String? = null, isbn: String? = null, openLibraryId: String? = null): Book? {
            var result: Book? = null
            if (result == null && openLibraryId != null) {
                result = Book.find { BookTable.openLibraryCol eq openLibraryId }.firstOrNull()
            }
            if (result == null && isbn != null) {
                result = Book.find { BookTable.isbnCol eq isbn }.firstOrNull()
            }
            if (result == null) {
                result = Book.find { (BookTable.titleCol eq title) and (BookTable.subtitleCol eq subtitle) }.firstOrNull()
            }
            return result
        }

        fun findOrCreate(title: String, subtitle: String? = null, isbn: String? = null, openLibraryId: String? = null): Book {
            return find(title, subtitle, isbn, openlibrary) ?: Book.new {
                this.title = title
                this.subtitle = subtitle
                this.isbn = isbn
                this.openLibraryId = openLibraryId
            }
        }
    }

    val credits by Credit referrersOn CreditTable.bookCol
    var format: Format by BookTable.formatCol
    var goodreads: String? by BookTable.goodreadsCol
    var googleBooks: String? by BookTable.googleBooksCol
    var imageUrl: String? by BookTable.imageUrlCol
    var isbn: String? by BookTable.isbnCol
    var isCollected: Boolean by BookTable.isCollectedCol
    var libraryThing: String? by BookTable.libraryThingCol
    var openLibrary: String? by BookTable.openLibraryCol
    var publishDate: LocalDate? by BookTable.publishDateCol
    var publisher: Publisher? by Publisher optionalReferencedOn BookTable.publisherCol
    val readers by ReadBook referrersOn ReadBookTable.bookCol
    val series by BookSeries referrersOn BookSeriesTable.bookCol
    var subtitle: String? by BookTable.subtitleCol
    var summary: String? by BookTable.summaryCol
    var title: String by BookTable.titleCol
    var wishers by User via WishedTable

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "format" to format.name,
            "identifiers" to mapOf(
                "bookshelf" to id.value,
                "goodreads" to goodreads,
                "googleBooks" to googleBooks,
                "isbn" to isbn,
                "libraryThing" to libraryThing,
                "openLibrary" to openLibrary,
            ),
            "imageUrl" to imageUrl,
            "isCollected" to isCollected,
            "publishDate" to publishDate?.toString("yyyy-MM-dd"),
            "publisher" to publisher?.id?.value,
            "subtitle" to subtitle,
            "summary" to summary,
            "title" to title,
        ).apply {
            if (showAll) {
                put("credits", credits.groupBy({ it.role.id.value }, { it.creator.id.value }))
                put("readers", readers.groupBy({ it.user.id.value }, { it.readDate?.toString("yyyy-MM-dd") }))
                put("series", series.sortedBy { it.series }.map { it.series.id.value to it.number })
                put("wishers", wishers.sorted().map { it.id.value })
            }
        }.toSortedMap()
    }

    override fun compareTo(other: Book): Int = comparator.compare(this, other)
}

data class BookInput(
    val credits: List<Credit> = emptyList(),
    val format: Format = Format.PAPERBACK,
    val identifiers: Identifiers? = null,
    val imageUrl: String? = null,
    val isCollected: Boolean = false,
    val publishDate: LocalDate? = null,
    val publisher: Long? = null,
    val readers: List<Reader> = emptyList(),
    val series: List<Series> = emptyList(),
    val subtitle: String? = null,
    val summary: String? = null,
    val title: String,
    val wishers: List<Long> = emptyList(),
) {
    data class Credit(
        val creator: Long,
        val role: Long,
    )

    data class Identifiers(
        val goodreads: String? = null,
        val googleBooks: String? = null,
        val isbn: String? = null,
        val libraryThing: String? = null,
        val openLibrary: String? = null,
    )

    data class Reader(
        val user: Long,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        val readDate: LocalDate? = null,
    )

    data class Series(
        val series: Long,
        val number: Int? = null,
    )
}

data class ImportBook(
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val isbn: String? = null,
    val isCollected: Boolean = false,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
)
