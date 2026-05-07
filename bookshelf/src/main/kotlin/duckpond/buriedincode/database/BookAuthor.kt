package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class BookAuthor(id: EntityID<CompositeID>) : CompositeEntity(id) {
  companion object : CompositeEntityClass<BookAuthor>(BookAuthorTable)

  var author: Author by Author referencedOn BookAuthorTable.authorCol
  var book: Book by Book referencedOn BookAuthorTable.bookCol

  var lastUpdated: Instant by BookAuthorTable.lastUpdatedCol
}

@OptIn(ExperimentalTime::class)
object BookAuthorTable : CompositeIdTable(name = "book_authors") {
  val authorCol: Column<EntityID<Long>> =
    reference(
      name = "author_id",
      foreign = AuthorTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val bookCol: Column<EntityID<Long>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }

  override val primaryKey = PrimaryKey(authorCol, bookCol)

  init {
    transaction {
      addIdColumn(authorCol)
      addIdColumn(bookCol)
      SchemaUtils.create(this)
    }
  }
}
