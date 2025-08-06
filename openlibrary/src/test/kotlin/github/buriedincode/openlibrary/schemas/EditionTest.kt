package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import github.buriedincode.openlibrary.ServiceException
import java.nio.file.Paths
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class EditionTest {
  private val session: OpenLibrary

  init {
    val cache = SQLiteCache(path = Paths.get("cache.sqlite"), expiry = null)
    session = OpenLibrary(cache = cache)
  }

  @Nested
  inner class GetEdition {
    @Test
    fun `Test GetEdition with a valid id`() {
      val result = session.getEdition(id = "OL26964454M")
      assertNotNull(result)
      assertAll(
        { assertEquals("/authors/OL2993106A", result.authors[0].key) },
        { assertNotNull(result.byStatement) },
        { assertEquals("Boichi, ill", result.contributions[0]) },
        { assertEquals("Boichi", result.contributors[0].name) },
        { assertEquals("Illustrator", result.contributors[0].role) },
        { assertEquals(14577228, result.covers[0]) },
        { assertEquals("741.5", result.deweyDecimalClass[0]) },
        { assertNull(result.fullTitle) },
        { assertTrue(result.identifiers?.goodreads?.isEmpty() ?: false) },
        { assertTrue(result.identifiers?.google?.isEmpty() ?: false) },
        { assertTrue(result.identifiers?.librarything?.isEmpty() ?: false) },
        { assertEquals("1974702618", result.isbn10[0]) },
        { assertEquals("9781974702619", result.isbn13[0]) },
        { assertEquals("/books/OL26964454M", result.key) },
        { assertEquals("/languages/eng", result.languages[0].key) },
        { assertEquals(10, result.latestRevision) },
        { assertEquals("PN6790", result.lcClassifications[0]) },
        { assertEquals("2018299499", result.lccn[0]) },
        { assertEquals("urn:sfpl:31223111921111", result.localId[0]) },
        { assertNotNull(result.notes) },
        { assertEquals(200, result.numberOfPages) },
        { assertEquals("1054104980", result.oclcNumbers[0]) },
        { assertEquals("Stone world", result.otherTitles[0]) },
        { assertEquals("1 v. (unpaged)", result.pagination) },
        { assertEquals("Manga", result.physicalFormat) },
        { assertEquals("cau", result.publishCountry) },
        { assertEquals(LocalDate(2018, 9, 4), result.publishDate) },
        { assertEquals("SHONEN JUMP", result.publishers[0]) },
        { assertEquals(10, result.revision) },
        {
          assertEquals(
            "marc:marc_openlibraries_sanfranciscopubliclibrary/sfpl_chq_2018_12_24_run06.mrc:171004390:2606",
            result.sourceRecords[0],
          )
        },
        { assertEquals("Survival", result.subjects[0]) },
        { assertEquals("Stone World", result.subtitle) },
        { assertEquals("/languages/jpn", result.translatedFrom[0].key) },
        { assertEquals("Dr. STONE, Vol. 1", result.title) },
        { assertEquals("/type/edition", result.type.key) },
        { assertEquals("/works/OL37805541W", result.works[0].key) },
      )
    }

    @Test
    fun `Test GetEdition with an invalid id`() {
      assertThrows(ServiceException::class.java) { session.getEdition(id = "-1") }
    }
  }

  @Nested
  inner class GetEditionByISBN {
    @Test
    fun `Test GetEditionByISBN with a valid id`() {
      val result = session.getEditionByISBN(isbn = "9781974702619")
      assertNotNull(result)
      assertAll(
        { assertEquals("/authors/OL2993106A", result.authors[0].key) },
        { assertNotNull(result.byStatement) },
        { assertEquals("Boichi, ill", result.contributions[0]) },
        { assertEquals("Boichi", result.contributors[0].name) },
        { assertEquals("Illustrator", result.contributors[0].role) },
        { assertEquals(14577228, result.covers[0]) },
        { assertEquals("741.5", result.deweyDecimalClass[0]) },
        { assertNull(result.fullTitle) },
        { assertTrue(result.identifiers?.goodreads?.isEmpty() ?: false) },
        { assertTrue(result.identifiers?.google?.isEmpty() ?: false) },
        { assertTrue(result.identifiers?.librarything?.isEmpty() ?: false) },
        { assertEquals("1974702618", result.isbn10[0]) },
        { assertEquals("9781974702619", result.isbn13[0]) },
        { assertEquals("/books/OL26964454M", result.key) },
        { assertEquals("/languages/eng", result.languages[0].key) },
        { assertEquals(10, result.latestRevision) },
        { assertEquals("PN6790", result.lcClassifications[0]) },
        { assertEquals("2018299499", result.lccn[0]) },
        { assertEquals("urn:sfpl:31223111921111", result.localId[0]) },
        { assertNotNull(result.notes) },
        { assertEquals(200, result.numberOfPages) },
        { assertEquals("1054104980", result.oclcNumbers[0]) },
        { assertEquals("Stone world", result.otherTitles[0]) },
        { assertEquals("1 v. (unpaged)", result.pagination) },
        { assertEquals("Manga", result.physicalFormat) },
        { assertEquals("cau", result.publishCountry) },
        { assertEquals(LocalDate(2018, 9, 4), result.publishDate) },
        { assertEquals("SHONEN JUMP", result.publishers[0]) },
        { assertEquals(10, result.revision) },
        {
          assertEquals(
            "marc:marc_openlibraries_sanfranciscopubliclibrary/sfpl_chq_2018_12_24_run06.mrc:171004390:2606",
            result.sourceRecords[0],
          )
        },
        { assertEquals("Survival", result.subjects[0]) },
        { assertEquals("Stone World", result.subtitle) },
        { assertEquals("/languages/jpn", result.translatedFrom[0].key) },
        { assertEquals("Dr. STONE, Vol. 1", result.title) },
        { assertEquals("/type/edition", result.type.key) },
        { assertEquals("/works/OL37805541W", result.works[0].key) },
      )
    }

    @Test
    fun `Test GetEditionByISBN with an invalid id`() {
      assertThrows(ServiceException::class.java) { session.getEditionByISBN(isbn = "-1") }
    }
  }
}
