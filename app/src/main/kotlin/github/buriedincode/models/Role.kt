package github.buriedincode.models

import github.buriedincode.tables.CreditTable
import github.buriedincode.tables.RoleTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Role(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Role> {
  companion object : LongEntityClass<Role>(RoleTable) {
    val comparator = compareBy(Role::title)

    fun find(title: String): Role? {
      return Role.find { RoleTable.titleCol eq title }.firstOrNull()
    }

    fun findOrCreate(title: String): Role {
      return find(title) ?: Role.new { this.title = title }
    }
  }

  val credits by Credit referrersOn CreditTable.roleCol
  var title: String by RoleTable.titleCol

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>("id" to id.value, "title" to title)
      .apply {
        if (showAll) {
          put(
            "credits",
            credits.groupBy({ it.creator }, { it.book }).toSortedMap().map { (creator, books) ->
              mapOf("creator" to creator.toJson(), "books" to books.map { it.toJson() })
            },
          )
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: Role): Int = comparator.compare(this, other)
}

data class RoleInput(val credits: List<Credit> = emptyList(), val title: String) {
  data class Credit(val book: Long, val creator: Long)
}
