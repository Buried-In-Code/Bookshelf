package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable data class Link(val title: String, val type: Resource? = null, val url: String)

@Serializable data class Resource(val key: String)

@Serializable data class TypedResource(val type: String, val value: String)
