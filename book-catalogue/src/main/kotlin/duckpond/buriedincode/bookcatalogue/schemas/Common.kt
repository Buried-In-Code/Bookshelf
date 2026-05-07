package duckpond.buriedincode.bookcatalogue.schemas

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ListResponse(@JsonNames("Results") val results: ListResult) {
  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  data class ListResult(@JsonNames("data") val results: List<ResponseData>, val status: String)
}
