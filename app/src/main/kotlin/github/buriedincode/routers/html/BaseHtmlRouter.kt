package github.buriedincode.routers.html

import github.buriedincode.Utils.transaction
import github.buriedincode.models.User
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

abstract class BaseHtmlRouter<T : LongEntity>(protected val entity: LongEntityClass<T>, protected val plural: String) {
  protected val name: String = entity::class.java.declaringClass.simpleName.lowercase()
  protected val paramName: String = "$name-id"
  protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

  protected fun Context.getResource(): T =
    this.pathParam(paramName).toLongOrNull()?.let { id ->
      entity.findById(id) ?: throw NotFoundResponse("$title not found")
    } ?: throw BadRequestResponse("Invalid $title Id")

  protected fun Context.getSession(): User? =
    this.cookie("bookshelf_session-id")?.toLongOrNull()?.let { User.findById(it) }

  protected fun render(
    ctx: Context,
    template: String,
    model: Map<String, Any?> = emptyMap(),
    redirect: Boolean = true,
  ) {
    val session = ctx.getSession()
    if (session == null && redirect) {
      ctx.redirect("/$plural")
    }
    ctx.render("templates/$name/$template.kte", mapOf("session" to session) + model)
  }

  protected fun renderResource(
    ctx: Context,
    template: String,
    model: Map<String, Any?> = emptyMap(),
    redirect: Boolean = true,
  ) {
    render(ctx, template, mapOf("resource" to ctx.getResource()) + model, redirect)
  }

  protected open fun filterResources(ctx: Context): List<T> = emptyList()

  protected open fun filters(ctx: Context): Map<String, Any?> = emptyMap()

  protected open fun createOptions(): Map<String, Any?> = emptyMap()

  protected open fun updateOptions(ctx: Context): Map<String, Any?> = emptyMap()

  open fun list(ctx: Context) = transaction {
    render(ctx, "list", mapOf("resources" to filterResources(ctx), "filters" to filters(ctx)), redirect = false)
  }

  open fun create(ctx: Context) = transaction { render(ctx, "create", createOptions()) }

  open fun view(ctx: Context) = transaction { renderResource(ctx, "view", redirect = false) }

  open fun update(ctx: Context) = transaction { renderResource(ctx, "update", createOptions() + updateOptions(ctx)) }
}
