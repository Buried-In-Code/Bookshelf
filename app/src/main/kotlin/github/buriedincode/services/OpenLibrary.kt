package github.buriedincode.services

import github.buriedincode.Utils
import github.buriedincode.openlibrary.OpenLibrary as Session
import github.buriedincode.openlibrary.SQLiteCache
import github.buriedincode.openlibrary.schemas.Author
import github.buriedincode.openlibrary.schemas.Edition
import github.buriedincode.openlibrary.schemas.Resource
import github.buriedincode.openlibrary.schemas.SearchResponse
import github.buriedincode.openlibrary.schemas.Work
import kotlin.io.path.div

object OpenLibrary {
  private val session = Session(cache = SQLiteCache(path = (Utils.CACHE_ROOT / "openlibrary.sqlite"), expiry = 14))

  fun search(title: String): List<SearchResponse.Work> = session.searchWork(params = mapOf("title" to title))

  fun getEdition(id: String): Edition = session.getEdition(id = id)

  fun getEditionByISBN(isbn: String): Edition = session.getEditionByISBN(isbn = isbn)

  fun getWork(id: String): Work = session.getWork(id = id)

  fun getAuthor(id: String): Author = session.getAuthor(id = id)
}

fun Author.getId(): String = this.key.split("/").last()

fun Edition.getId(): String = this.key.split("/").last()

fun Resource.getId(): String = this.key.split("/").last()

fun SearchResponse.Author.getId(): String = this.key.split("/").last()

fun SearchResponse.Work.getId(): String = this.key.split("/").last()

fun Work.getId(): String = this.key.split("/").last()
