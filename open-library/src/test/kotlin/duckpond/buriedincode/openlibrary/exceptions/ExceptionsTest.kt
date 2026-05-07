package duckpond.buriedincode.openlibrary.exceptions

import duckpond.buriedincode.openlibrary.OpenLibrary
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExceptionsTest {
  @Nested
  inner class Service {
    @Test
    fun `Test throwing a ServiceException for a 404`() {
      val session = OpenLibrary(cache = null)
      Assertions.assertThrows(ServiceException::class.java) {
        val uri = session.encodeURI(endpoint = "/invalid")
        session.getRequest(uri = uri)
      }
    }

    @Test
    fun `Test throwing a ServiceException for a timeout`() {
      val session = OpenLibrary(cache = null, timeout = 1.milliseconds)
      Assertions.assertThrows(ServiceException::class.java) { session.getEdition(id = "OL26964454M") }
    }
  }
}
