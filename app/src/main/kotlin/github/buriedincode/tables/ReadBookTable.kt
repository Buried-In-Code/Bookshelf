package github.buriedincode.tables

import github.buriedincode.Utils.transaction
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.kotlin.datetime.date

object ReadBookTable : LongIdTable(name = "read_books") {
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
  val readDateCol: Column<LocalDate?> = date(name = "read_date").nullable()

  init {
    transaction {
      uniqueIndex(bookCol, userCol)
      SchemaUtils.create(this)
    }
  }
}
