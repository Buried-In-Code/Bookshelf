package duckpond.buriedincode.controllers

import duckpond.buriedincode.database.User
import duckpond.buriedincode.database.UserTable
import duckpond.buriedincode.services.USER_LIST
import duckpond.buriedincode.services.getSessionOrNull
import duckpond.buriedincode.transaction
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.v1.core.like

object UserController {
  private val LOGGER: KLogger = KotlinLogging.logger {}

  private fun Context.getResourceOrNull(): User? =
    this.pathParam("user-id").toLongOrNull()?.let { User.findById(id = it) }

  private fun Context.getResource(): User = this.getResourceOrNull() ?: throw NotFoundResponse("User not found.")

  private fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    ctx.render(
      "templates/user/${ template }.kte",
      mapOf("session" to ctx.getSessionOrNull(), "users" to USER_LIST) + model,
    )
  }

  fun listPage(ctx: Context): Unit = transaction {
    val query = ctx.queryParam("q")
    val results = query?.let { User.find { UserTable.usernameCol like "%$query%" } } ?: User.all()
    render(ctx = ctx, template = "list", model = mapOf("query" to query, "results" to results.sorted().toList()))
  }

  fun viewPage(ctx: Context): Unit = transaction {
    val resource = ctx.getResource()
    val nextBooks =
      resource.readBooks
        .flatMap { it.book.series.map { it.series } }
        .distinct()
        .sorted()
        .mapNotNull {
          it.books.map { it.book }.sorted().firstOrNull { book -> resource.readBooks.none { it.book == book } }
        }
    render(ctx = ctx, template = "view", model = mapOf("resource" to resource, "nextBooks" to nextBooks))
  }
}
