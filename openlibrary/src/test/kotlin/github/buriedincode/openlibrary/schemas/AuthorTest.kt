package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import github.buriedincode.openlibrary.ServiceException
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
class AuthorTest {
  private val session: OpenLibrary

  init {
    val cache = SQLiteCache(path = Paths.get("cache.sqlite"), expiry = null)
    session = OpenLibrary(cache = cache)
  }

  @Nested
  inner class GetAuthor {
    @Test
    fun `Test GetAuthor with a valid id`() {
      val result = session.getAuthor(id = "OL2993106A")
      assertNotNull(result)
      assertAll(
        { assertEquals(11497441, result.id) },
        { assertEquals("/authors/OL2993106A", result.key) },
        { assertEquals("Riichiro Inagaki", result.name) },
        { assertEquals(1, result.revision) },
        { assertEquals("/type/author", result.type.key) },
      )
    }

    @Test
    fun `Test GetAuthor with an invalid id`() {
      assertThrows(ServiceException::class.java) { session.getAuthor(id = "-1") }
    }
  }

  @Nested
  inner class SearchAuthor {
    @Test
    fun `Test SearchAuthor with a valid search`() {
      val results = session.searchAuthor(params = mapOf("q" to "Riichiro Inagaki"))
      assertEquals(5, results.size)
      assertAll(
        { assertTrue(results[0].alternateNames.isEmpty()) },
        { assertNull(results[0].dateOfBirth) },
        { assertNull(results[0].date) },
        { assertNull(results[0].dateOfDeath) },
        { assertEquals("OL2993106A", results[0].key) },
        { assertEquals("Riichiro Inagaki", results[0].name) },
        { assertEquals("Comic books, strips", results[0].topSubjects[0]) },
        { assertEquals("Eyeshield 21", results[0].topWork) },
        { assertEquals("author", results[0].type) },
        { assertEquals(213, results[0].workCount) },
        { assertEquals(1796011843638001667, results[0].version) },
      )
    }
  }
}
