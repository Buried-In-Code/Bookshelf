package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.models.User
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse

object AuthController {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  private fun Context.getSession(): User? =
    this.sessionAttribute<Long>("session-id")?.let { User.findById(it) }
      ?: this.basicAuthCredentials()?.let { (username, _) -> User.findOrNull(username) }

  fun signUp(ctx: Context) = transaction {
    ctx.basicAuthCredentials()?.let { (username, password) ->
      val user =
        User.findOrNull(username)?.let { throw ConflictResponse("User already exists") }
          ?: User.new {
            this.password = password
            this.username = username
          }
      ctx.sessionAttribute("session-id", user.id.value)
      ctx.status(status = HttpStatus.NO_CONTENT)
    } ?: throw UnauthorizedResponse()
  }

  fun signIn(ctx: Context) = transaction {
    ctx.basicAuthCredentials()?.let { (username, password) ->
      User.findOrNull(username)?.let find@{
        if (it.password != password) {
          return@find null
        }
        ctx.sessionAttribute("session-id", it.id.value)
        ctx.status(status = HttpStatus.NO_CONTENT)
      } ?: throw NotFoundResponse("User not found.")
    } ?: throw UnauthorizedResponse()
  }

  fun signOut(ctx: Context) {
    ctx.req().session.invalidate()
    ctx.status(status = HttpStatus.NO_CONTENT)
  }

  fun homePage(ctx: Context) = transaction {
    ctx.getSession()?.let { ctx.redirect("/users/${it.id.value}") } ?: ctx.render("templates/index.kte")
  }
}
