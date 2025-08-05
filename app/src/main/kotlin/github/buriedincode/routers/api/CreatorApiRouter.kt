package github.buriedincode.routers.api

import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.Creator
import github.buriedincode.models.CreatorInput
import github.buriedincode.models.Credit
import github.buriedincode.models.Role
import github.buriedincode.tables.CreatorTable
import github.buriedincode.tables.CreditTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object CreatorApiRouter : BaseApiRouter<Creator>(entity = Creator) {
  override fun list(ctx: Context): Unit = transaction {
    val query = CreatorTable.selectAll()
    ctx.queryParam("book-id")?.toLongOrNull()?.let {
      Book.findById(it)?.let { book -> query.andWhere { CreditTable.bookCol eq book.id } }
    }
    ctx.queryParam("name")?.let { name -> query.andWhere { CreatorTable.nameCol like "%$name%" } }
    ctx.queryParam("role-id")?.toLongOrNull()?.let {
      Role.findById(it)?.let { role -> query.andWhere { CreditTable.roleCol eq role.id } }
    }
    ctx.json(Creator.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
  }

  override fun create(ctx: Context) =
    ctx.processInput<CreatorInput> { body ->
      transaction {
        Creator.find(body.name)?.let { throw ConflictResponse("Creator already exists") }
        val resource =
          Creator.findOrCreate(body.name).apply {
            body.credits.forEach {
              Credit.new {
                this.book = Book.findById(it.book) ?: throw NotFoundResponse("Book not found.")
                this.creator = this@apply
                this.role = Role.findById(it.role) ?: throw NotFoundResponse("Role not found.")
              }
            }
            imageUrl = body.imageUrl
          }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
      }
    }

  override fun update(ctx: Context) =
    manage<CreatorInput>(ctx) { body, creator ->
      Creator.find(body.name)?.takeIf { it != creator }?.let { throw ConflictResponse("Creator already exists") }
      creator.apply {
        credits.forEach { it.delete() }
        body.credits.forEach {
          Credit.findOrCreate(
            Book.findById(it.book) ?: throw NotFoundResponse("Book not found."),
            this,
            Role.findById(it.role) ?: throw NotFoundResponse("Role not found."),
          )
        }
        imageUrl = body.imageUrl
        name = body.name
      }
    }

  override fun delete(ctx: Context) = transaction {
    ctx.getResource().apply {
      credits.forEach { it.delete() }
      delete()
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  fun addCredit(ctx: Context) =
    manage<CreatorInput.Credit>(ctx) { body, creator ->
      Credit.findOrCreate(
        Book.findById(body.book) ?: throw NotFoundResponse("Book not found."),
        creator,
        Role.findById(body.role) ?: throw NotFoundResponse("Role not found."),
      )
    }

  fun removeCredit(ctx: Context) =
    manage<CreatorInput.Credit>(ctx) { body, creator ->
      Credit.find(
          Book.findById(body.book) ?: throw NotFoundResponse("Book not found."),
          creator,
          Role.findById(body.role) ?: throw NotFoundResponse("Role not found."),
        )
        ?.delete()
    }
}
