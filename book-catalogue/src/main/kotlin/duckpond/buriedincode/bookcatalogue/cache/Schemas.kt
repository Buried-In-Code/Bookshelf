package duckpond.buriedincode.bookcatalogue.cache

import kotlin.time.Instant

data class CacheData(val url: String, val response: String, val createdAt: Instant)
