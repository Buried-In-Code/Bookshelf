package duckpond.buriedincode.database

import duckpond.buriedincode.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class WishedBook(id: EntityID<CompositeID>) : CompositeEntity(id) {
  companion object : CompositeEntityClass<WishedBook>(WishedBookTable)

  var book: Book by Book referencedOn WishedBookTable.bookCol
  var date: LocalDate? by WishedBookTable.dateCol
  var user: User by User referencedOn WishedBookTable.userCol

  var lastUpdated: Instant by WishedBookTable.lastUpdatedCol
}

@OptIn(ExperimentalTime::class)
object WishedBookTable : CompositeIdTable(name = "wished_books") {
  val bookCol: Column<EntityID<Long>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val dateCol: Column<LocalDate?> = date(name = "date").nullable()
  val lastUpdatedCol: Column<Instant> =
    timestamp(name = "last_updated").default(Clock.System.now()).clientDefault { Clock.System.now() }
  val userCol: Column<EntityID<Long>> =
    reference(
      name = "user_id",
      foreign = UserTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )

  override val primaryKey = PrimaryKey(bookCol, userCol)

  init {
    transaction {
      addIdColumn(bookCol)
      addIdColumn(userCol)
      SchemaUtils.create(this)
    }
  }
}
