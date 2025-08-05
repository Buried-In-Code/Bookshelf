package github.buriedincode.routers.html

import github.buriedincode.Utils.asEnumOrNull
import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.Creator
import github.buriedincode.models.Format
import github.buriedincode.models.Publisher
import github.buriedincode.models.Role
import github.buriedincode.models.Series
import io.javalin.http.Context

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book, plural = "books") {
  override fun list(ctx: Context) = transaction {
    val extras = if (ctx.getSession() == null) mapOf("read" to true, "wished" to true) else emptyMap()
    render(
      ctx,
      "list",
      mapOf("resources" to filterResources(ctx), "filters" to filters(ctx), "extras" to extras),
      redirect = false,
    )
  }

  override fun view(ctx: Context) = transaction {
    renderResource(
      ctx,
      "view",
      mapOf("credits" to ctx.getResource().credits.groupBy({ it.role }, { it.creator })),
      redirect = false,
    )
  }

  fun import(ctx: Context) = transaction { render(ctx, "import") }

  fun search(ctx: Context) = transaction { render(ctx, "search") }

  override fun filterResources(ctx: Context): List<Book> {
    val isCollected: Boolean? = ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull()
    var resources = Book.all().toList()
    ctx.queryParam("creator-id")?.toLongOrNull()?.let {
      Creator.findById(it)?.let { creator -> resources = resources.filter { it.credits.any { it.creator == creator } } }
    }
    ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
      resources = resources.filter { format == it.format }
    }
    ctx.getSession()?.let { session ->
      if (isCollected == true || isCollected == null) {
        ctx.queryParam("has-read")?.lowercase()?.toBooleanStrictOrNull()?.let { hasRead ->
          resources = resources.filter { session.readBooks.any { it.book == it } == hasRead }
        }
      }
      if (isCollected == false || isCollected == null) {
        ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull()?.let { hasWished ->
          resources = resources.filter { (it in session.wishedBooks) == hasWished }
        }
      }
    }
    isCollected?.let { resources = resources.filter { it.isCollected == isCollected!! } }
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
    return resources
  }

  override fun filters(ctx: Context): Map<String, Any?> =
    mapOf(
      "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
      "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
      "has-read" to ctx.queryParam("has-read")?.lowercase()?.toBooleanStrictOrNull(),
      "has-wished" to ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull(),
      "is-collected" to ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull(),
      "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
      "role" to ctx.queryParam("role-id")?.toLongOrNull()?.let { Role.findById(it) },
      "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
      "title" to ctx.queryParam("title"),
    )

  override fun createOptions(): Map<String, Any?> =
    mapOf(
      "formats" to Format.entries.toList(),
      "has-read" to listOf(true, false),
      "has-wished" to listOf(true, false),
      "is-collected" to listOf(true, false),
      "publishers" to Publisher.all().toList(),
    )

  override fun updateOptions(ctx: Context): Map<String, Any?> =
    mapOf(
      "creators" to Creator.all().toList(),
      "roles" to Role.all().toList(),
      "series" to Series.all().filterNot { series -> ctx.getResource().series.any { it.series == series } }.toList(),
    )
}
