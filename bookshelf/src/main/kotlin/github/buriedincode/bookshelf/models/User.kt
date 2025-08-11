package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.transaction
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class User(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<User> {
  companion object : LongEntityClass<User>(UserTable) {
    val comparator = compareBy(User::username)

    fun findOrNull(username: String): User? {
      return find { UserTable.usernameCol eq username }.firstOrNull()
    }
  }

  var imageUrl: String? by UserTable.imageUrlCol
  var password: String by UserTable.passwordCol
  var username: String by UserTable.usernameCol

  val collected by Collected referrersOn CollectedTable.userCol
  val read by Read referrersOn ReadTable.userCol
  val wished by Wished referrersOn WishedTable.userCol

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>(
        "id" to id.value,
        "imageUrl" to imageUrl,
        "password" to password,
        "username" to username,
      )
      .apply {
        if (showAll) {
          put("collected", collected.sortedWith(compareBy(Collected::date, Collected::book)).map { it.book.id.value })
          put("read", read.sortedWith(compareBy(Read::date, Read::book)).map { it.book.id.value })
          put("wished", wished.sortedWith(compareBy(Wished::date, Wished::book)).map { it.book.id.value })
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: User): Int = comparator.compare(this, other)
}

object UserTable : LongIdTable(name = "users") {
  val imageUrlCol: Column<String?> = text(name = "image_url").nullable()
  val passwordCol: Column<String> = text(name = "password")
  val usernameCol: Column<String> = text(name = "username").uniqueIndex()

  init {
    transaction { SchemaUtils.create(this) }
  }
}
