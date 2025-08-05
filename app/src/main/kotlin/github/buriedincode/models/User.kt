package github.buriedincode.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import github.buriedincode.Utils.toString
import github.buriedincode.tables.ReadBookTable
import github.buriedincode.tables.UserTable
import github.buriedincode.tables.WishedTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<User> {
  companion object : LongEntityClass<User>(UserTable) {
    val comparator = compareBy(User::username)

    fun findOrNull(username: String): User? {
      return find { UserTable.usernameCol eq username }.firstOrNull()
    }

    fun findOrCreate(username: String): User {
      return findOrNull(username) ?: new { this.username = username }
    }
  }

  var imageUrl: String? by UserTable.imageUrlCol
  val readBooks by ReadBook referrersOn ReadBookTable.userCol
  var username: String by UserTable.usernameCol
  var wishedBooks by Book via WishedTable

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>("id" to id.value, "imageUrl" to imageUrl, "username" to username)
      .apply {
        if (showAll) {
          put("read", readBooks.groupBy({ it.readDate?.toString("yyyy-MM-dd") ?: "null" }, { it.book.toJson() }))
          put("wished", wishedBooks.sorted().map { it.toJson() })
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: User): Int = comparator.compare(this, other)
}

data class UserInput(
  val readBooks: List<ReadBook> = emptyList(),
  val imageUrl: String? = null,
  val username: String,
  val wishedBooks: List<Long> = emptyList(),
) {
  data class ReadBook(
    val book: Long,
    @param:JsonDeserialize(using = LocalDateDeserializer::class) val readDate: LocalDate? = null,
  )
}
