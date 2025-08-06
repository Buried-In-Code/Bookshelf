package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import github.buriedincode.openlibrary.ServiceException
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class WorkTest {
  private val session: OpenLibrary

  init {
    val cache = SQLiteCache(path = Paths.get("cache.sqlite"), expiry = null)
    session = OpenLibrary(cache = cache)
  }

  @Nested
  inner class GetWork {
    @Test
    fun `Test GetWork with a valid id`() {
      val result = session.getWork(id = "OL37805541W")
      assertNotNull(result)
      assertAll(
        { assertEquals("/authors/OL2993106A", result.authors[0].author.key) },
        { assertEquals("/type/author_role", result.authors[0].type.key) },
        { assertTrue(result.covers.isEmpty()) },
        { assertNotNull(result.description) },
        { assertEquals("/works/OL37805541W", result.key) },
        { assertEquals(2, result.latestRevision) },
        { assertEquals(2, result.revision) },
        { assertTrue(result.subjects.isEmpty()) },
        { assertEquals("Dr. STONE, Vol. 1", result.title) },
        { assertEquals("/type/work", result.type.key) },
      )
    }

    @Test
    fun `Test GetWork with an invalid id`() {
      assertThrows(ServiceException::class.java) { session.getWork(id = "-1") }
    }
  }

  @Nested
  inner class SearchWork {
    @Test
    fun `Test SearchWork with a valid search`() {
      val results = session.searchWork(params = mapOf("title" to "Dr. Stone, Vol. 1"))
      assertEquals(1, results.size)
      assertAll(
        { assertEquals(0, results[0].alreadyReadCount) },
        { assertTrue(results[0].authorAlternativeName.isEmpty()) },
        { assertEquals("OL2993106A Riichiro Inagaki", results[0].authorFacet[0]) },
        { assertEquals("OL2993106A", results[0].authorKey[0]) },
        { assertEquals("Riichiro Inagaki", results[0].authorName[0]) },
        { assertEquals("Cook, Caleb D., translator", results[0].contributor[0]) },
        { assertEquals("OL38630032M", results[0].coverEditionKey) },
        { assertEquals(12821031, results[0].cover) },
        { assertEquals(0, results[0].currentlyReadingCount) },
        { assertEquals("741.5", results[0].ddc[0]) },
        { assertEquals("741.5", results[0].ddcSort) },
        { assertEquals("no_ebook", results[0].ebookAccess) },
        { assertEquals(0, results[0].ebookCount) },
        { assertEquals(5, results[0].editionCount) },
        { assertEquals("OL38630032M", results[0].editionKey[0]) },
        { assertEquals(2018, results[0].firstPublishYear) },
        { assertTrue(results[0].firstSentence.isEmpty()) },
        { assertEquals("Manga", results[0].format[0]) },
        { assertFalse(results[0].hasFulltext) },
        { assertTrue(results[0].ia.isEmpty()) },
        { assertTrue(results[0].iaBoxId.isEmpty()) },
        { assertTrue(results[0].iaCollection.isEmpty()) },
        { assertNull(results[0].iaCollectionString) },
        { assertTrue(results[0].iaLoadedId.isEmpty()) },
        { assertTrue(results[0].idAmazon.isEmpty()) },
        { assertTrue(results[0].idGoodreads.isEmpty()) },
        { assertTrue(results[0].idGoogle.isEmpty()) },
        { assertTrue(results[0].idLibrarything.isEmpty()) },
        { assertTrue(results[0].idOverdrive.isEmpty()) },
        { assertTrue(results[0].idProjectGutenberg.isEmpty()) },
        { assertTrue(results[0].idWikidata.isEmpty()) },
        { assertTrue(results[0].idDepositoLegal.isEmpty()) },
        { assertTrue(results[0].idIsfdb.isEmpty()) },
        { assertEquals("6076340487", results[0].isbn[0]) },
        { assertEquals("/works/OL37805541W", results[0].key) },
        { assertEquals("spa", results[0].language[0]) },
        { assertEquals(1715902450, results[0].lastModified) },
        { assertEquals("PN-6790.00000000.J34 D7313 2018", results[0].lcc[0]) },
        { assertEquals("2018299499", results[0].lccn[0]) },
        { assertEquals("PN-6790.00000000.J34 D7313 2018", results[0].lccSort) },
        { assertNull(results[0].lendingEdition) },
        { assertNull(results[0].lendingIdentifier) },
        { assertEquals(196, results[0].numberOfPagesMedian) },
        { assertEquals("1054104980", results[0].oclc[0]) },
        { assertNull(results[0].ospCount) },
        { assertTrue(results[0].person.isEmpty()) },
        { assertTrue(results[0].personFacet.isEmpty()) },
        { assertTrue(results[0].personKey.isEmpty()) },
        { assertTrue(results[0].place.isEmpty()) },
        { assertTrue(results[0].placeFacet.isEmpty()) },
        { assertTrue(results[0].placeKey.isEmpty()) },
        { assertNull(results[0].printDisabled) },
        { assertFalse(results[0].publicScan) },
        { assertEquals("2021", results[0].publishDate[0]) },
        { assertTrue(results[0].publishPlace.isEmpty()) },
        { assertEquals(2018, results[0].publishYear[0]) },
        { assertEquals("Panini", results[0].publisher[0]) },
        { assertEquals("Editorial Ivrea", results[0].publisherFacet[0]) },
        { assertNull(results[0].ratingsAverage) },
        { assertNull(results[0].ratingsCount) },
        { assertNull(results[0].oneStarRatings) },
        { assertNull(results[0].twoStarRatings) },
        { assertNull(results[0].threeStarRatings) },
        { assertNull(results[0].fourStarRatings) },
        { assertNull(results[0].fiveStarRatings) },
        { assertNull(results[0].ratingsSortable) },
        { assertEquals(1, results[0].readinglogCount) },
        { assertEquals("/books/OL38630032M", results[0].seed[0]) },
        { assertTrue(results[0].subject.isEmpty()) },
        { assertTrue(results[0].subjectFacet.isEmpty()) },
        { assertTrue(results[0].subjectKey.isEmpty()) },
        { assertTrue(results[0].time.isEmpty()) },
        { assertTrue(results[0].timeFacet.isEmpty()) },
        { assertTrue(results[0].timeKey.isEmpty()) },
        { assertEquals("Dr. STONE, Vol. 1", results[0].title) },
        { assertEquals("Dr. STONE, Vol. 1", results[0].titleSort) },
        { assertEquals("Dr. STONE, Vol. 1", results[0].titleSuggest) },
        { assertEquals("work", results[0].type) },
        { assertEquals(1799254130621939712, results[0].version) },
        { assertEquals(1, results[0].wantToReadCount) },
      )
    }
  }
}
