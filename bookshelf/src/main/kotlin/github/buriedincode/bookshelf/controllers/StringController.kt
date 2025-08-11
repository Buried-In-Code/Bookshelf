package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.models.IJson
import github.buriedincode.bookshelf.models.User
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import io.javalin.http.UnauthorizedResponse
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

abstract class StringController<T>(protected val entity: EntityClass<String, T>) where T : Entity<String>, T : IJson {
  protected val name: String = entity::class.java.declaringClass.simpleName.lowercase()
  protected val paramName: String = "$name-id"
  protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

  protected fun Context.getResource(): T =
    this.pathParam(paramName).let { entity.findById(it) ?: throw NotFoundResponse("$title not found") }

  protected fun Context.getSession(): User? =
    this.sessionAttribute<Long>("session-id")?.let { User.findById(it) }
      ?: this.basicAuthCredentials()?.let { (username, _) -> User.findOrNull(username) }

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

  protected open fun filteredResources(ctx: Context): List<T> = entity.all().toList()

  open fun list(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    ctx.json(filteredResources(ctx).map { it.toJson() })
  }

  open fun create(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    throw NotImplementedResponse()
  }

  open fun read(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    ctx.json(ctx.getResource().toJson(showAll = true))
  }

  open fun update(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    throw NotImplementedResponse()
  }

  open fun delete(ctx: Context): Unit = transaction {
    ctx.getSession() ?: throw UnauthorizedResponse()
    ctx.getResource().delete()
    ctx.status(status = HttpStatus.NO_CONTENT)
  }

  protected fun render(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
    } else {
      ctx.render("templates/$name/$template.kte", mapOf<String, Any?>("session" to session) + model)
    }
  }

  protected fun renderResource(ctx: Context, template: String, model: Map<String, Any?> = emptyMap()) {
    render(ctx, template, mapOf<String, Any?>("resource" to ctx.getResource()) + model)
  }

  open fun listPage(ctx: Context) = transaction { render(ctx, "list", mapOf("resources" to filteredResources(ctx))) }

  open fun createPage(ctx: Context) = transaction { render(ctx, "create") }

  open fun readPage(ctx: Context) = transaction { renderResource(ctx, "view") }

  open fun updatePage(ctx: Context) = transaction { renderResource(ctx, "update") }
}
