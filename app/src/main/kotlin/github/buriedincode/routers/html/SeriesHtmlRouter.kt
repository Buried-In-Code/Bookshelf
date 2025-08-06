package github.buriedincode.routers.html

import github.buriedincode.models.Book
import github.buriedincode.models.Series
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context

object SeriesHtmlRouter : BaseHtmlRouter<Series>(entity = Series, plural = "series") {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun filterResources(ctx: Context): List<Series> {
    var resources = Series.all().toList()
    ctx.queryParam("title")?.let { title ->
      resources = resources.filter { it.title.contains(title, ignoreCase = true) }
    }
    return resources
  }

  override fun filters(ctx: Context): Map<String, Any?> = mapOf("title" to ctx.queryParam("title"))

  override fun updateOptions(ctx: Context): Map<String, Any?> =
    mapOf("books" to Book.all().filter { book -> ctx.getResource().books.none { it.book == book } }.toList())
}
