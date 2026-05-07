package duckpond.buriedincode.bookcatalogue.exceptions

import duckpond.buriedincode.bookcatalogue.BookCatalogue
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class ExceptionsTest {
  @Nested
  inner class Service {
    @Test
    fun `Test throwing a ServiceException for a 404`() {
      val session = BookCatalogue(cache = null)
      assertThrows(ServiceException::class.java) {
        val uri = session.encodeURI(endpoint = "/invalid")
        session.getRequest(uri = uri)
      }
    }

    @Test
    fun `Test throwing a ServiceException for a timeout`() {
      val session = BookCatalogue(cache = null, timeout = 1.milliseconds)
      assertThrows(ServiceException::class.java) { session.getBook(isbn = "9781974715466") }
    }
  }
}
