package duckpond.buriedincode.controllers

import duckpond.buriedincode.database.Series
import duckpond.buriedincode.database.SeriesTable
import duckpond.buriedincode.services.USER_LIST
import duckpond.buriedincode.services.getSessionOrNull
import duckpond.buriedincode.transaction
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.v1.core.like

object SeriesController {
  private fun Context.getResourceOrNull(): Series? =
    this.pathParam("series-id").toLongOrNull()?.let { Series.findById(id = it) }

  private fun Context.getResource(): Series = this.getResourceOrNull() ?: throw NotFoundResponse("Series not found.")

  private fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    ctx.render(
      "templates/series/${ template }.kte",
      mapOf("session" to ctx.getSessionOrNull(), "users" to USER_LIST) + model,
    )
  }

  fun listPage(ctx: Context) = transaction {
    val query = ctx.queryParam("q")
    val results = query?.let { Series.find { SeriesTable.titleCol like "%${ query }%" } } ?: Series.all()
    render(ctx = ctx, template = "list", model = mapOf("query" to query, "results" to results.sorted().toList()))
  }

  fun viewPage(ctx: Context) = transaction {
    render(ctx = ctx, template = "view", model = mapOf("resource" to ctx.getResource()))
  }
}
