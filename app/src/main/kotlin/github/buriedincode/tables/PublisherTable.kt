package github.buriedincode.tables

import github.buriedincode.Utils.transaction
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object PublisherTable : LongIdTable(name = "publishers") {
  val titleCol: Column<String> = text(name = "title").uniqueIndex()

  init {
    transaction { SchemaUtils.create(this) }
  }
}
