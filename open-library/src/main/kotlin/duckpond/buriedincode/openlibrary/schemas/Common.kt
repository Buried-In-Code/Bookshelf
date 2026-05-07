package duckpond.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable data class Link(val title: String, val type: Resource? = null, val url: String)

@Serializable data class Resource(val key: String)

@Serializable data class TypedResource<T>(val type: String, val value: T)

@Serializable
data class ListResponse<T>(val links: Links, val size: Int, val entries: List<T>) {
  @Serializable data class Links(val self: String, val work: String)
}
