package github.buriedincode.routers.html

import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.Creator
import github.buriedincode.models.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.NotImplementedResponse

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role, plural = "roles") {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun list(ctx: Context) = throw NotImplementedResponse()

  override fun create(ctx: Context) = throw NotImplementedResponse()

  override fun view(ctx: Context) = transaction {
    renderResource(
      ctx,
      "view",
      mapOf("credits" to ctx.getResource().credits.groupBy({ it.creator }, { it.book })),
      redirect = false,
    )
  }

  override fun updateOptions(ctx: Context): Map<String, Any?> =
    mapOf("books" to Book.all().toList(), "creators" to Creator.all().toList())
}
