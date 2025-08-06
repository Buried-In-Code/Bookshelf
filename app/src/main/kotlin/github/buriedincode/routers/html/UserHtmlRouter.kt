package github.buriedincode.routers.html

import github.buriedincode.Utils.asEnumOrNull
import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.Creator
import github.buriedincode.models.Format
import github.buriedincode.models.Publisher
import github.buriedincode.models.Role
import github.buriedincode.models.Series
import github.buriedincode.models.User
import github.buriedincode.tables.BookTable
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User, plural = "users") {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun create(ctx: Context) = transaction { render(ctx, "create", createOptions(), redirect = false) }

  override fun view(ctx: Context) = transaction {
    val resource = ctx.getResource()
    val nextBooks =
      resource.readBooks
        .flatMap { it.book.series.map { it.series } }
        .distinct()
        .sorted()
        .mapNotNull {
          it.books.map { it.book }.sorted().firstOrNull { book -> resource.readBooks.none { it.book == book } }
        }
    val model =
      mapOf(
        "stats" to
          mapOf<String, Int>(
            "wishlist" to resource.wishedBooks.count().toInt(),
            "shared" to Book.all().count { !it.isCollected && it.wishers.empty() },
            "unread" to (Book.find { BookTable.isCollectedCol eq true }.count() - resource.readBooks.count()).toInt(),
            "read" to resource.readBooks.count().toInt(),
          ),
        "nextBooks" to nextBooks,
      )
    renderResource(ctx, "view", model, redirect = false)
  }

  fun wishlist(ctx: Context) = transaction {
    val resource = ctx.getResource()
    ctx.render(
      "templates/book/list.kte",
      mapOf(
        "session" to ctx.getSession(),
        "resources" to filterWishlist(ctx),
        "filters" to wishlistFilters(ctx),
        "extras" to
          mapOf(
            "collected" to true,
            "read" to true,
            "resetUrl" to "/users/${resource.id.value}/wishlist",
            "title" to "${resource.username}'s Wishlist",
          ),
      ),
    )
  }

  fun readlist(ctx: Context) = transaction {
    val resource = ctx.getResource()
    ctx.render(
      "templates/book/list.kte",
      mapOf(
        "session" to ctx.getSession(),
        "resources" to filterReadlist(ctx),
        "filters" to readlistFilters(ctx),
        "extras" to
          mapOf(
            "collected" to true,
            "read" to true,
            "resetUrl" to "/users/${resource.id.value}/readlist",
            "title" to "${resource.username}'s Readlist",
            "wished" to true,
          ),
      ),
    )
  }

  override fun filterResources(ctx: Context): List<User> {
    var resources = User.all().toList()
    ctx.queryParam("username")?.let { username ->
      resources = resources.filter { it.username.contains(username, ignoreCase = true) }
    }
    return resources
  }

  override fun filters(ctx: Context): Map<String, Any?> = mapOf("username" to ctx.queryParam("username"))

  override fun updateOptions(ctx: Context): Map<String, Any?> =
    mapOf(
      "readBooks" to
        Book.all().filter { it.isCollected }.filterNot { it in ctx.getResource().readBooks.map { it.book } }.toList(),
      "wishedBooks" to
        Book.all().filterNot { it.isCollected }.filterNot { it in ctx.getResource().wishedBooks }.toList(),
    )

  private fun filterWishlist(ctx: Context): List<Book> {
    val resource = ctx.getResource()
    var resources =
      Book.all().filter { resource.wishedBooks.any { book -> it == book } || (!it.isCollected && it.wishers.empty()) }
    ctx.queryParam("creator-id")?.toLongOrNull()?.let {
      Creator.findById(it)?.let { creator -> resources = resources.filter { it.credits.any { it.creator == creator } } }
    }
    ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
      resources = resources.filter { format == it.format }
    }
    ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull()?.let { hasWished ->
      resources = resources.filter { (it in resource.wishedBooks) == hasWished }
    }
    ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
      Publisher.findById(it)?.let { publisher -> resources = resources.filter { publisher == it.publisher } }
    }
    ctx.queryParam("role-id")?.toLongOrNull()?.let {
      Role.findById(it)?.let { role -> resources = resources.filter { it.credits.any { it.role == role } } }
    }
    ctx.queryParam("series-id")?.toLongOrNull()?.let {
      Series.findById(it)?.let { series -> resources = resources.filter { it.series.any { it.series == series } } }
    }
    ctx.queryParam("title")?.let { title ->
      resources =
        resources.filter {
          it.title.contains(title, ignoreCase = true) || (it.subtitle?.contains(title, ignoreCase = true) == true)
        }
    }
    return resources.toList()
  }

  private fun wishlistFilters(ctx: Context): Map<String, Any?> =
    mapOf(
      "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
      "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
      "has-read" to false,
      "has-wished" to ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull(),
      "is-collected" to false,
      "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
      "role" to ctx.queryParam("role-id")?.toLongOrNull()?.let { Role.findById(it) },
      "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
      "title" to ctx.queryParam("title"),
    )

  private fun filterReadlist(ctx: Context): List<Book> {
    var resources = ctx.getResource().readBooks.map { it.book }
    ctx.queryParam("creator-id")?.toLongOrNull()?.let {
      Creator.findById(it)?.let { creator -> resources = resources.filter { it.credits.any { it.creator == creator } } }
    }
    ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
      resources = resources.filter { format == it.format }
    }
    ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
      Publisher.findById(it)?.let { publisher -> resources = resources.filter { publisher == it.publisher } }
    }
    ctx.queryParam("role-id")?.toLongOrNull()?.let {
      Role.findById(it)?.let { role -> resources = resources.filter { it.credits.any { it.role == role } } }
    }
    ctx.queryParam("series-id")?.toLongOrNull()?.let {
      Series.findById(it)?.let { series -> resources = resources.filter { it.series.any { it.series == series } } }
    }
    ctx.queryParam("title")?.let { title ->
      resources =
        resources.filter {
          it.title.contains(title, ignoreCase = true) || (it.subtitle?.contains(title, ignoreCase = true) == true)
        }
    }
    return resources.toList()
  }

  private fun readlistFilters(ctx: Context): Map<String, Any?> =
    mapOf(
      "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
      "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
      "has-read" to true,
      "has-wished" to null,
      "is-collected" to true,
      "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
      "role" to ctx.queryParam("role-id")?.toLongOrNull()?.let { Role.findById(it) },
      "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
      "title" to ctx.queryParam("title"),
    )
}
