package duckpond.buriedincode.services

import duckpond.buriedincode.database.User
import duckpond.buriedincode.transaction
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context

private const val USER_COOKIE_NAME = "bookshelf-user"

val USER_LIST by lazy { transaction { User.all().sortedBy { it.username }.toList() } }

fun Context.getSessionOrNull(): User? = this.cookie(USER_COOKIE_NAME)?.toLongOrNull()?.let { User.findById(id = it) }

fun Context.getSession(): User = this.getSessionOrNull() ?: throw BadRequestResponse("Invalid user session.")

fun Context.setSession() = transaction {
  val body = this.bodyAsClass(SessionCookie::class.java)
  val userId = body.userId.toLongOrNull()
  if (userId == null) {
    this.removeCookie(USER_COOKIE_NAME)
  } else {
    this.cookie(USER_COOKIE_NAME, userId.toString(), maxAge = 60 * 60 * 24)
  }
}

private data class SessionCookie(val userId: String)
