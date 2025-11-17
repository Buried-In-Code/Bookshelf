package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.controllers.AuthenticationController.getSession
import github.buriedincode.bookshelf.database.User
import github.buriedincode.bookshelf.database.UserTable
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.v1.core.like

object UserController {
  @JvmStatic private val LOGGER: KLogger = KotlinLogging.logger {}

  private fun Context.getResource(): User {
    return this.pathParam("user-id").toLongOrNull()?.let {
      User.findById(id = it) ?: throw NotFoundResponse("User not found.")
    } ?: throw BadRequestResponse("Bad User id")
  }

  private fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
    } else {
      ctx.render("templates/user/$template.kte", mapOf<String, Any?>("session" to session) + model)
    }
  }

  fun listUsersPage(ctx: Context) = transaction {
    val query = ctx.queryParam("q")
    if (query.isNullOrBlank()) {
      render(ctx = ctx, template = "list", model = mapOf<String, Any?>("results" to User.all().toList()))
    } else {
      render(
        ctx = ctx,
        template = "list",
        model = mapOf<String, Any?>("results" to User.find { UserTable.usernameCol like "%$query%" }.toList()),
      )
    }
  }

  fun viewUserPage(ctx: Context) = transaction {
    render(ctx = ctx, template = "view", model = mapOf<String, Any?>("resource" to ctx.getResource()))
  }
}
