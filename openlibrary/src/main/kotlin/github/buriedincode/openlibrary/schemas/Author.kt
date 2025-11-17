package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.DescriptionSerializer
import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Author(
  val alternateNames: List<String> = emptyList(),
  @Serializable(with = DescriptionSerializer::class) val bio: String? = null,
  val birthDate: String? = null,
  val comment: String? = null,
  @Serializable(with = LocalDateTimeSerializer::class) val created: LocalDateTime? = null,
  val date: String? = null,
  @Serializable(with = LocalDateSerializer::class) val deathDate: LocalDate? = null,
  val entityType: String? = null,
  val fullerName: String? = null,
  val id: Long? = null,
  val key: String,
  @Serializable(with = LocalDateTimeSerializer::class) val lastModified: LocalDateTime?,
  val latestRevision: Int? = null,
  val links: List<Link> = emptyList(),
  val location: String? = null,
  val name: String,
  val personalName: String? = null,
  val photos: List<Int> = emptyList(),
  val remoteIds: RemoteIds? = null,
  val revision: Int,
  val sourceRecords: List<String> = emptyList(),
  val title: String? = null,
  val type: Resource,
  val wikipedia: String? = null,
) {
  @Serializable
  data class RemoteIds(
    val amazon: String? = null,
    val bookbrainz: String? = null,
    val goodreads: String? = null,
    val isni: String? = null,
    val librarything: String? = null,
    val librivox: String? = null,
    val projectGutenberg: String? = null,
    val storygraph: String? = null,
    val viaf: String? = null,
    val wikidata: String? = null,
  )
}
