package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.database.User
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

object AuthenticationController {
  @JvmStatic private val LOGGER: KLogger = KotlinLogging.logger {}
  @JvmStatic private val ENCODER = Argon2PasswordEncoder(128 / 8, 256 / 8, 1, 10 * 1024, 10)

  @JvmStatic
  internal fun authenticateUser(username: String, password: String?): User? {
    return User.findOrNull(username = username)?.let {
      if (ENCODER.matches(password, it.password)) {
        it
      } else {
        null
      }
    }
  }

  @JvmStatic
  internal fun Context.getSession(): User? {
    return this.sessionAttribute<Long>("session-id")?.let { User.findById(id = it) }
      ?: this.basicAuthCredentials()?.let { (username, password) ->
        authenticateUser(username = username, password = password)
      }
  }

  fun register(ctx: Context) {
    ctx.basicAuthCredentials()?.let { (username, password) ->
      transaction {
        val user =
          User.findOrNull(username = username)?.let { throw ConflictResponse("User already exists") }
            ?: User.new {
              this.username = username
              this.password = ENCODER.encode(password) ?: throw IllegalStateException("Unable to encode passw")
            }
        ctx.sessionAttribute("session-id", user.id.value)
      }
      ctx.status(status = HttpStatus.NO_CONTENT)
    } ?: throw UnauthorizedResponse()
  }

  fun login(ctx: Context) {
    ctx.basicAuthCredentials()?.let { (username, password) ->
      transaction {
        val user =
          authenticateUser(username = username, password = password)
            ?: throw UnauthorizedResponse("Invalid Username/Password.")
        ctx.sessionAttribute("session-id", user.id.value)
      }
      ctx.status(status = HttpStatus.NO_CONTENT)
    } ?: throw UnauthorizedResponse()
  }

  fun logout(ctx: Context) {
    ctx.req().session.invalidate()
    ctx.status(status = HttpStatus.NO_CONTENT)
  }
}
