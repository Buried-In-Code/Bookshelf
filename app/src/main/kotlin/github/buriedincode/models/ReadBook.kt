package github.buriedincode.models

import github.buriedincode.tables.ReadBookTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class ReadBook(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<ReadBook>(ReadBookTable) {
    fun find(book: Book, user: User): ReadBook? {
      return ReadBook.find { (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id) }.firstOrNull()
    }

    fun findOrCreate(book: Book, user: User): ReadBook {
      return find(book, user)
        ?: ReadBook.new {
          this.book = book
          this.user = user
        }
    }
  }

  var book: Book by Book referencedOn ReadBookTable.bookCol
  var user: User by User referencedOn ReadBookTable.userCol
  var readDate: LocalDate? by ReadBookTable.readDateCol
}
