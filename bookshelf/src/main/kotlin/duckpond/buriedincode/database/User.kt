package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class User(id: EntityID<Long>) : LongEntity(id), Comparable<User> {
  companion object : LongEntityClass<User>(UserTable) {
    val comparator = compareBy(User::username)

    fun findOrNull(username: String): User? {
      return find { UserTable.usernameCol eq username }.firstOrNull()
    }
  }

  var imageUrl: String? by UserTable.imageUrlCol
  var username: String by UserTable.usernameCol

  var lastUpdated: Instant by UserTable.lastUpdatedCol

  val readBooks by ReadBook referrersOn ReadBookTable.userCol
  val wishedBooks by WishedBook referrersOn WishedBookTable.userCol

  val collected: List<Book>
    get() = Book.find { BookTable.isCollectedCol eq true }.toList()

  override fun compareTo(other: User): Int = comparator.compare(this, other)
}

@OptIn(ExperimentalTime::class)
object UserTable : LongIdTable(name = "users") {
  val imageUrlCol: Column<String?> = text(name = "image_url").nullable()
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val usernameCol: Column<String> = text(name = "username").uniqueIndex()

  init {
    transaction { SchemaUtils.create(this) }
  }
}
