package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object SeriesTable : LongIdTable(name = "series"), Logging {
    val summaryCol: Column<String?> = text(name = "summary").nullable()
    val titleCol: Column<String> = text(name = "title").uniqueIndex()

    init {
        Utils.query {
            SchemaUtils.create(this)
        }
    }
}
