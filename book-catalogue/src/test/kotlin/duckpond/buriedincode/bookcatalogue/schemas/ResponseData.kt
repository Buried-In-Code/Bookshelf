package duckpond.buriedincode.bookcatalogue.schemas

import duckpond.buriedincode.bookcatalogue.BookCatalogue
import duckpond.buriedincode.bookcatalogue.cache.SQLiteCache
import duckpond.buriedincode.bookcatalogue.exceptions.ServiceException
import java.nio.file.Paths
import kotlin.time.Instant
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class ResponseDataTest {
  private val session: BookCatalogue

  init {
    val cache = SQLiteCache(path = Paths.get("cache.sqlite"), expiry = null)
    session = BookCatalogue(cache = cache)
  }

  @Nested
  inner class GetBcBook {
    @Test
    fun `Test GetBook of type BCBD with a valid isbn`() {
      val result = session.getBook(isbn = "9781974715466")
      assertNotNull(result)
      assertEquals("OK", result.status)
      assertInstanceOf(ResponseData.BookCatalogue::class.java, result)
      val book = (result as ResponseData.BookCatalogue).book!!
      assertAll(
        { assertTrue(book.anthologies.isEmpty()) },
        { assertNull(book.anthology) },
        {
          val author = book.authors[0]
          assertAll(
            { assertEquals(0, author.authorPosition) },
            { assertEquals("Endo", author.familyName) },
            { assertEquals("Tatsuya", author.givenNames) },
          )
        },
        { assertEquals(Instant.parse("2020-06-02T00:00:00Z"), book.datePublished) },
        { assertEquals("https://covers.openlibrary.org/b/id/14582895-L.jpg", book.defaultThumbnail) },
        { assertNull(book.format) },
        { assertNull(book.genre) },
        { assertEquals("9781974715466", book.isbn) },
        { assertNull(book.listPrice) },
        { assertNull(book.pages) },
        { assertEquals("SHONEN JUMP", book.publisher) },
        {
          val series = book.series[0]
          assertAll(
            { assertEquals(0, series.seriesPosition) },
            { assertEquals("スパイファミリー [SPY×FAMILY]", series.seriesName) },
            { assertEquals("1", series.seriesNum) },
          )
        },
        { assertEquals("Spy x Family, Vol. 1", book.title) },
      )
    }

    @Test
    fun `Test GetBook with an invalid isbn`() {
      assertThrows(ServiceException::class.java) { session.getBook(isbn = "invalid") }
    }
  }
}
