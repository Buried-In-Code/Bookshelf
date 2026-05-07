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

class Author(id: EntityID<Long>) : LongEntity(id), Comparable<Author> {
  companion object : LongEntityClass<Author>(AuthorTable) {
    val comparator = compareBy(Author::name)

    fun findOrNull(name: String): Author? {
      return find { AuthorTable.nameCol eq name }.firstOrNull()
    }
  }

  var name: String by AuthorTable.nameCol

  var lastUpdated: Instant by AuthorTable.lastUpdatedCol

  val books by BookAuthor referrersOn BookAuthorTable.authorCol

  override fun compareTo(other: Author): Int = comparator.compare(this, other)
}

@OptIn(ExperimentalTime::class)
object AuthorTable : LongIdTable(name = "author") {
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val nameCol: Column<String> = text(name = "name").uniqueIndex()

  init {
    transaction { SchemaUtils.create(this) }
  }
}
