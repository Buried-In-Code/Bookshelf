package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Creator(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Creator> {
    companion object : LongEntityClass<Creator>(CreatorTable), Logging {
        val comparator = compareBy(Creator::name)
    }

    val credits by Credit referrersOn CreditTable.creatorCol
    var imageUrl: String? by CreatorTable.imageUrlCol
    var name: String by CreatorTable.nameCol
    var summary: String? by CreatorTable.summaryCol

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "imageUrl" to imageUrl,
            "name" to name,
            "summary" to summary,
        )
        if (showAll) {
            output["credits"] = credits.sortedWith(
                compareBy<Credit> { it.book }.thenBy { it.role },
            ).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "role" to it.role.toJson(),
                )
            }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Creator): Int = comparator.compare(this, other)
}

data class CreatorInput(
    val credits: List<Credit> = ArrayList(),
    val imageUrl: String? = null,
    val name: String,
    val summary: String? = null,
) {
    data class Credit(
        val bookId: Long,
        val roleId: Long,
    )
}
