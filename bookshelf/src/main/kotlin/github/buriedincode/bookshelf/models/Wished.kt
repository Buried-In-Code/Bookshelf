package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.transaction
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class Wished(id: EntityID<CompositeID>) : CompositeEntity(id) {
  companion object : CompositeEntityClass<Wished>(WishedTable)

  var date: LocalDate? by WishedTable.dateCol

  val book: Book
    get() = Book[this.id.value.get(WishedTable.bookCol)]

  val user: User
    get() = User[this.id.value.get(WishedTable.userCol)]
}

object WishedTable : CompositeIdTable(name = "wished_books") {
  val bookCol: Column<EntityID<String>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val dateCol: Column<LocalDate?> = date(name = "date").nullable()
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
