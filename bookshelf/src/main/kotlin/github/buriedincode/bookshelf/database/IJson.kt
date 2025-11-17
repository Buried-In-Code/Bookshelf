package github.buriedincode.bookshelf.database

interface IJson {
  fun toJson(showAll: Boolean = false): Map<String, Any?>
}
