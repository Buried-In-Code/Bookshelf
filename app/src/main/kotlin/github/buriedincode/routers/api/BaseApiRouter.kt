package github.buriedincode.routers.api

import github.buriedincode.Utils.transaction
import github.buriedincode.models.IJson
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

abstract class BaseApiRouter<T>(protected val entity: LongEntityClass<T>) where T : LongEntity, T : IJson {
  protected val name: String = entity::class.java.declaringClass.simpleName.lowercase()
  protected val paramName: String = "$name-id"
  protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

  protected fun Context.getResource(): T =
    this.pathParam(paramName).toLongOrNull()?.let { entity.findById(it) ?: throw NotFoundResponse("$title not found") }
      ?: throw BadRequestResponse("Invalid $title Id")

  protected inline fun <reified I> Context.processInput(crossinline block: (I) -> Unit) =
    block(bodyAsClass(I::class.java))

  protected inline fun <reified I> manage(ctx: Context, crossinline block: (I, T) -> Unit) =
    ctx.processInput<I> { body ->
      transaction {
        val resource = ctx.getResource()
        block(body, resource)
        ctx.json(resource.toJson(showAll = true))
      }
    }

  abstract fun list(ctx: Context)

  abstract fun create(ctx: Context)

  open fun read(ctx: Context) = transaction { ctx.json(ctx.getResource().toJson(showAll = true)) }

  abstract fun update(ctx: Context)

  open fun delete(ctx: Context) = transaction {
    ctx.getResource().delete()
    ctx.status(status = HttpStatus.NO_CONTENT)
  }
}
