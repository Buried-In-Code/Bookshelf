package github.buriedincode.models

import github.buriedincode.tables.BookTable
import github.buriedincode.tables.PublisherTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Publisher(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Publisher> {
  companion object : LongEntityClass<Publisher>(PublisherTable) {
    val comparator = compareBy(Publisher::title)

    fun find(title: String): Publisher? {
      return Publisher.find { PublisherTable.titleCol eq title }.firstOrNull()
    }

    fun findOrCreate(title: String): Publisher {
      return find(title) ?: Publisher.new { this.title = title }
    }
  }

  var title: String by PublisherTable.titleCol

  val books by Book optionalReferrersOn BookTable.publisherCol

  override fun toJson(showAll: Boolean): Map<String, Any?> {
    return mutableMapOf<String, Any?>("id" to id.value, "title" to title)
      .apply {
        if (showAll) {
          put("books", books.sorted().map { it.toJson() })
        }
      }
      .toSortedMap()
  }

  override fun compareTo(other: Publisher): Int = comparator.compare(this, other)
}

data class PublisherInput(val title: String)
