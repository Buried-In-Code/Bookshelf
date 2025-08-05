package github.buriedincode.models

enum class Format {
  BOX_SET,
  GRAPHIC_NOVEL,
  HARDBACK, // TODO: Remove
  HARDCOVER,
  MANGA,
  PAPERBACK,
  TRADEPAPERBACK;

  val displayName: String
    get() = name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
}
