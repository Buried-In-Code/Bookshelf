package duckpond.buriedincode.bookcatalogue.schemas

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
sealed class ResponseData {
  abstract val status: String

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  @SerialName("BCDB")
  data class BookCatalogue(@JsonNames("data") val book: Book? = null, override val status: String) : ResponseData() {
    @Serializable
    data class Book(
      val anthologies: List<Byte>, // TODO: Figure out actual typing
      val anthology: Int?,
      val authors: List<Author>,
      val datePublished: Instant?,
      val defaultThumbnail: String,
      val description: String?, // Can contain HTML
      val format: String?,
      val genre: String?,
      val isbn: String,
      val listPrice: Byte?, // TODO: Figure out actual typing
      val pages: Int?,
      val publisher: String?,
      val series: List<Series>,
      val title: String,
    ) {
      @Serializable data class Author(val authorPosition: Int, val familyName: String, val givenNames: String)
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  @SerialName("Google")
  data class Google(@JsonNames("data") val book: Book? = null, override val status: String) : ResponseData() {
    @Serializable
    data class Book(
      val anthologies: List<Byte>, // TODO: Figure out actual typing
      val anthology: Int?,
      val authors: List<Author>,
      val datePublished: LocalDate,
      val defaultThumbnail: String,
      val description: String?,
      val format: String,
      val genre: String,
      val isbn: String,
      val listPrice: Byte?, // TODO: Figure out actual typing
      val pages: Int,
      val publisher: String,
      val series: List<Series>,
      val thumbnails: List<Thumbnail>,
      val title: String,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  @SerialName("Goodreads")
  data class Goodreads(@JsonNames("data") val book: Book? = null, override val status: String) : ResponseData() {
    @Serializable
    data class Book(
      val anthologies: List<Byte>, // TODO: Figure out actual typing
      val anthology: Int?,
      val authors: List<Author>,
      val datePublished: Byte?,
      val defaultThumbnail: String,
      val description: String,
      val format: String?,
      val genre: String?,
      val isbn: String,
      val listPrice: Byte?, // TODO: Figure out actual typing
      val pages: Int?,
      val publisher: String?,
      val series: List<Series>,
      val thumbnails: List<Thumbnail>,
      val title: String,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  @SerialName("OpenLibrary")
  data class OpenLibrary(@JsonNames("data") val book: Book? = null, override val status: String) : ResponseData() {
    @Serializable
    data class Book(
      val anthologies: List<Byte>, // TODO: Figure out actual typing
      val anthology: Int?,
      val authors: List<Author>,
      val datePublished: LocalDate, // TODO: Parse OL date values
      val defaultThumbnail: String,
      val description: String?,
      val format: String?,
      val genre: String?,
      val isbn: String, // TODO: Need formatting to strip '-'
      val listPrice: Byte?, // TODO: Figure out actual typing
      val pages: Int?,
      val publisher: String,
      val series: List<Series>,
      val thumbnails: List<Thumbnail>,
      val title: String,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Serializable
  @SerialName("Amazon")
  data class Amazon(@JsonNames("data") val book: Book? = null, override val status: String) : ResponseData() {
    @Serializable
    data class Book(
      val anthologies: List<Byte> // TODO: Figure out actual typing
    )
  }
}

@Serializable data class Author(val authorPosition: Int, val fullName: String)

@Serializable data class Series(val seriesPosition: Int, val seriesName: String, val seriesNum: String)

@Serializable data class Thumbnail(val type: String, val url: String)
