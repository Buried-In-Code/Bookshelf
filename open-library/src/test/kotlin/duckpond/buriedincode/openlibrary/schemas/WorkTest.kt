package duckpond.buriedincode.openlibrary.schemas

import duckpond.buriedincode.openlibrary.OpenLibrary
import duckpond.buriedincode.openlibrary.cache.SQLiteCache
import duckpond.buriedincode.openlibrary.exceptions.ServiceException
import java.nio.file.Paths
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
        {
          val author = result.authors[0]
          assertAll(
            { assertEquals("/authors/OL2993106A", author.author.key) },
            { assertEquals("/type/author_role", author.type.key) },
          )
        },
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
      assertEquals(2, results.size)
      val result = results[0]
      assertAll(
        { assertEquals("OL2993106A", result.authorIds[0]) },
        {
          val edition = result.editions.docs[0]
          assertAll(
            { assertEquals(5, result.editions.numFound) },
            { assertEquals("/books/OL26964454M", edition.key) },
            { assertEquals("OL26964454M", edition.id) },
            { assertEquals("Stone World", edition.subtitle) },
            { assertEquals("Dr. STONE, Vol. 1", edition.title) },
          )
        },
        { assertEquals("/works/OL37805541W", result.key) },
        { assertEquals("OL37805541W", result.id) },
        { assertNull(result.subtitle) },
        { assertEquals("Dr. STONE, Vol. 1", result.title) },
      )
    }
  }

  @Nested
  inner class WorkEditions {
    @Test
    fun `Test WorkEditions with a valid id`() {
      val result = session.getWorkEditions(id = "OL37805541W")
      assertEquals(5, result.size)
      val edition = result[0]
      assertAll(
        { assertEquals("/books/OL38630032M", edition.key) },
        { assertEquals("OL38630032M", edition.id) },
        { assertNull(edition.subtitle) },
        { assertEquals("Dr. STONE, Vol. 1", edition.title) },
      )
    }
  }
}
