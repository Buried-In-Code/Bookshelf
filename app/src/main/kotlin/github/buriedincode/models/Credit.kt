package github.buriedincode.models

import github.buriedincode.tables.CreditTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class Credit(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<Credit>(CreditTable) {
    fun find(book: Book, creator: Creator, role: Role): Credit? {
      return Credit.find {
          (CreditTable.bookCol eq book.id) and
            (CreditTable.creatorCol eq creator.id) and
            (CreditTable.roleCol eq role.id)
        }
        .firstOrNull()
    }

    fun findOrCreate(book: Book, creator: Creator, role: Role): Credit {
      return find(book, creator, role)
        ?: Credit.new {
          this.book = book
          this.creator = creator
          this.role = role
        }
    }
  }

  var book: Book by Book referencedOn CreditTable.bookCol
  var creator: Creator by Creator referencedOn CreditTable.creatorCol
  var role: Role by Role referencedOn CreditTable.roleCol
}
