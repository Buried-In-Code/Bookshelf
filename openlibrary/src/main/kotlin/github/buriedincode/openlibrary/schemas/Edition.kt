package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.DescriptionSerializer
import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Edition(
  val authors: List<Resource> = emptyList(),
  val byStatement: String? = null,
  val classifications: Classification? = null,
  val contributions: List<String> = emptyList(),
  val contributors: List<Contributor> = emptyList(),
  val copyrightDate: String? = null,
  val covers: List<Int> = emptyList(),
  @Serializable(with = LocalDateTimeSerializer::class) val created: LocalDateTime? = null,
  @Serializable(with = DescriptionSerializer::class) val description: String? = null,
  val deweyDecimalClass: List<String> = emptyList(),
  val editionName: String? = null,
  @Serializable(with = DescriptionSerializer::class) val firstSentence: String? = null,
  val fullTitle: String? = null,
  val genres: List<String> = emptyList(),
  val iaBoxId: List<String> = emptyList(),
  val iaLoadedId: List<String> = emptyList(),
  val identifiers: Identifiers? = null,
  @JsonNames("isbn_10") val isbn10: List<String> = emptyList(),
  @JsonNames("isbn_13") val isbn13: List<String> = emptyList(),
  val key: String,
  val languages: List<Resource> = emptyList(),
  @Serializable(with = LocalDateTimeSerializer::class) val lastModified: LocalDateTime? = null,
  val latestRevision: Int,
  val lcClassifications: List<String> = emptyList(),
  val lccn: List<String> = emptyList(),
  val links: List<Link> = emptyList(),
  val localId: List<String> = emptyList(),
  val location: List<String> = emptyList(),
  @Serializable(with = DescriptionSerializer::class) val notes: String? = null,
  val numberOfPages: Int? = null,
  val ocaid: String? = null,
  val oclcNumber: List<String> = emptyList(),
  val oclcNumbers: List<String> = emptyList(),
  val otherTitles: List<String> = emptyList(),
  val pagination: String? = null,
  val physicalDimensions: String? = null,
  val physicalFormat: String? = null,
  val publishCountry: String? = null,
  @Serializable(with = LocalDateSerializer::class) val publishDate: LocalDate? = null,
  val publishPlaces: List<String> = emptyList(),
  val publishers: List<String> = emptyList(),
  val revision: Int,
  val series: List<String> = emptyList(),
  val sourceRecords: List<String> = emptyList(),
  val subjectPeople: List<String> = emptyList(),
  val subjectPlace: List<String> = emptyList(),
  val subjectPlaces: List<String> = emptyList(),
  val subjects: List<String> = emptyList(),
  val subjectTime: List<String> = emptyList(),
  val subtitle: String? = null,
  val tableOfContents: List<Content> = emptyList(),
  val translatedFrom: List<Resource> = emptyList(),
  val translationOf: String? = null,
  val title: String,
  val type: Resource,
  val uriDescriptions: List<String> = emptyList(),
  val uris: List<String> = emptyList(),
  val url: List<String> = emptyList(),
  val weight: String? = null,
  val works: List<Resource> = emptyList(),
  val workTitle: List<String> = emptyList(),
  val workTitles: List<String> = emptyList(),
) {
  @Serializable data class Classification(val key: String? = null)

  @Serializable
  data class Content(
    val label: String? = null,
    val level: Int,
    val pagenum: String? = null,
    val title: String,
    val type: Resource? = null,
  )

  @Serializable data class Contributor(val name: String, val role: String)

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  data class Identifiers(
    val amazon: List<String> = emptyList(),
    @JsonNames("amazon.co.uk_asin") val amazonCoUkAsin: List<String> = emptyList(),
    val betterWorldBooks: List<String> = emptyList(),
    val goodreads: List<String> = emptyList(),
    val google: List<String> = emptyList(),
    val issn: List<String> = emptyList(),
    val librarything: List<String> = emptyList(),
    val wikidata: List<String> = emptyList(),
  )
}
