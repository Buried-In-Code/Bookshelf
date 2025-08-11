package github.buriedincode.bookshelf.models

interface IJson {
  fun toJson(showAll: Boolean = false): Map<String, Any?>
}
