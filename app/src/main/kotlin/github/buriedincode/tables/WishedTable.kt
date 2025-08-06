package github.buriedincode.tables

import github.buriedincode.Utils.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

object WishedTable : Table(name = "wished_books") {
  val bookCol: Column<EntityID<Long>> =
    reference(
      name = "book_id",
      foreign = BookTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  val userCol: Column<EntityID<Long>> =
    reference(
      name = "user_id",
      foreign = UserTable,
      onUpdate = ReferenceOption.CASCADE,
      onDelete = ReferenceOption.CASCADE,
    )
  override val primaryKey = PrimaryKey(bookCol, userCol)

  init {
    transaction { SchemaUtils.create(this) }
  }
}
