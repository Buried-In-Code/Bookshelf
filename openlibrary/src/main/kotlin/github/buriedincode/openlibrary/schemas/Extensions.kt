package github.buriedincode.openlibrary.schemas

val Edition.id: String
  get() = this.key.split("/").last()

val Resource.id: String
  get() = this.key.split("/").last()

val SearchResponse.Work.id: String
  get() = this.key.split("/").last()

val Work.id: String
  get() = this.key.split("/").last()
