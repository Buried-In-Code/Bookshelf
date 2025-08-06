package github.buriedincode.models

import github.buriedincode.tables.CreatorTable
import github.buriedincode.tables.CreditTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Creator(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Creator> {
  companion object : LongEntityClass<Creator>(CreatorTable) {
    val comparator = compareBy(Creator::name)

    fun find(name: String): Creator? {
      return Creator.find { CreatorTable.nameCol eq name }.firstOrNull()
    }

    fun findOrCreate(name: String): Creator {
      return find(name) ?: Creator.new { this.name = name }
    }
  }

  val credits by Credit referrersOn CreditTable.creatorCol
  var imageUrl: String? by CreatorTable.imageUrlCol
  var name: String by CreatorTable.nameCol

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>("id" to id.value, "imageUrl" to imageUrl, "name" to name)
      .apply {
        if (showAll) {
          put(
            "credits",
            credits.groupBy({ it.role }, { it.book }).toSortedMap().map { (role, books) ->
              mapOf("role" to role.toJson(), "books" to books.map { it.toJson() })
            },
          )
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: Creator): Int = comparator.compare(this, other)
}

data class CreatorInput(val credits: List<Credit> = emptyList(), val imageUrl: String? = null, val name: String) {
  data class Credit(val book: Long, val role: Long)
}
