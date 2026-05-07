package duckpond.buriedincode.openlibrary.schemas

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SearchResponse<T>(
  val docs: List<T> = listOf(),
  @JsonNames("documentationUrl") val documentationUrl: String? = null,
  @JsonNames("numFound") val numFound: Int,
  @JsonNames("numFoundExact") val numFoundExact: Boolean,
  val offset: Int? = null,
  val q: String? = null,
  val start: Int,
) {
  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  data class Work(
    @JsonNames("author_key") val authorIds: List<String> = emptyList(),
    val editions: SearchResponse<Edition>,
    val key: String,
    val subtitle: String? = null,
    val title: String,
  ) {
    val id: String
      get() = this.key.split("/").last()
  }

  @Serializable
  data class Edition(val key: String, val subtitle: String? = null, val title: String) {
    val id: String
      get() = this.key.split("/").last()
  }
}
