package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SearchResponse<T>(
    val docs: List<T> = listOf(),
    @JsonNames("numFound")
    val numFound: Int,
    @JsonNames("numFoundExact")
    val numFoundExact: Boolean,
    val offset: Int? = null,
    val q: String? = null,
    val start: Int,
) {
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Author(
        val alternateNames: List<String> = emptyList(),
        @JsonNames("birth_date")
        @Serializable(with = LocalDateSerializer::class)
        val dateOfBirth: LocalDate? = null,
        val date: String? = null,
        @JsonNames("death_date")
        @Serializable(with = LocalDateSerializer::class)
        val dateOfDeath: LocalDate? = null,
        val key: String,
        val name: String,
        val topSubjects: List<String> = emptyList(),
        val topWork: String? = null,
        val type: String,
        val workCount: Int,
        @JsonNames("_version_")
        val version: Long,
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Work(
        val alreadyReadCount: Int? = null,
        val authorAlternativeName: List<String> = emptyList(),
        val authorFacet: List<String> = emptyList(),
        val authorKey: List<String> = emptyList(),
        val authorName: List<String> = emptyList(),
        val contributor: List<String> = emptyList(),
        val coverEditionKey: String? = null,
        @JsonNames("cover_i")
        val cover: Long? = null,
        val currentlyReadingCount: Int? = null,
        val ddc: List<String> = emptyList(),
        val ddcSort: String? = null,
        val ebookAccess: String,
        @JsonNames("ebook_count_i")
        val ebookCount: Long,
        val editionCount: Int,
        val editionKey: List<String> = emptyList(),
        val firstPublishYear: Int? = null,
        val firstSentence: List<String> = emptyList(),
        val format: List<String> = emptyList(),
        val hasFulltext: Boolean,
        val ia: List<String> = emptyList(),
        val iaBoxId: List<String> = emptyList(),
        val iaCollection: List<String> = emptyList(),
        @JsonNames("ia_collection_s")
        val iaCollectionString: String? = null,
        val iaLoadedId: List<String> = emptyList(),
        val idAmazon: List<String> = emptyList(),
        val idBetterWorldBooks: List<String> = emptyList(),
        @JsonNames("id_depósito_legal")
        val idDepositoLegal: List<String> = emptyList(),
        val idGoodreads: List<String> = emptyList(),
        val idGoogle: List<String> = emptyList(),
        val idIsfdb: List<String> = emptyList(),
        val idLibrarything: List<String> = emptyList(),
        val idOverdrive: List<String> = emptyList(),
        val idProjectGutenberg: List<String> = emptyList(),
        val idWikidata: List<String> = emptyList(),
        val isbn: List<String> = emptyList(),
        val key: String,
        val language: List<String> = emptyList(),
        @JsonNames("last_modified_i")
        val lastModified: Long,
        val lcc: List<String> = emptyList(),
        val lccn: List<String> = emptyList(),
        val lccSort: String? = null,
        @JsonNames("lending_edition_s")
        val lendingEdition: String? = null,
        @JsonNames("lending_identifier_s")
        val lendingIdentifier: String? = null,
        val numberOfPagesMedian: Int? = null,
        val oclc: List<String> = emptyList(),
        val ospCount: Int? = null,
        val person: List<String> = emptyList(),
        val personFacet: List<String> = emptyList(),
        val personKey: List<String> = emptyList(),
        val place: List<String> = emptyList(),
        val placeFacet: List<String> = emptyList(),
        val placeKey: List<String> = emptyList(),
        @JsonNames("printdisabled_s")
        val printDisabled: String? = null,
        @JsonNames("public_scan_b")
        val publicScan: Boolean,
        val publishDate: List<String> = emptyList(),
        val publishPlace: List<String> = emptyList(),
        val publishYear: List<Int> = emptyList(),
        val publisher: List<String> = emptyList(),
        val publisherFacet: List<String> = emptyList(),
        val ratingsAverage: Double? = null,
        val ratingsCount: Int? = null,
        @JsonNames("ratings_count_1")
        val oneStarRatings: Int? = null,
        @JsonNames("ratings_count_2")
        val twoStarRatings: Int? = null,
        @JsonNames("ratings_count_3")
        val threeStarRatings: Int? = null,
        @JsonNames("ratings_count_4")
        val fourStarRatings: Int? = null,
        @JsonNames("ratings_count_5")
        val fiveStarRatings: Int? = null,
        val ratingsSortable: Double? = null,
        val readinglogCount: Int? = null,
        val seed: List<String> = emptyList(),
        val subject: List<String> = emptyList(),
        val subjectFacet: List<String> = emptyList(),
        val subjectKey: List<String> = emptyList(),
        val subtitle: String? = null,
        val time: List<String> = emptyList(),
        val timeFacet: List<String> = emptyList(),
        val timeKey: List<String> = emptyList(),
        val title: String,
        val titleSort: String,
        val titleSuggest: String,
        val type: String,
        @JsonNames("_version_")
        val version: Long,
        val wantToReadCount: Int? = null,
    )
}
