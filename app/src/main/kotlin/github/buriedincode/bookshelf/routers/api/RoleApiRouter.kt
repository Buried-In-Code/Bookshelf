package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.RoleInput
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object RoleApiRouter : BaseApiRouter<Role>(entity = Role) {
    override fun list(ctx: Context): Unit = Utils.query {
        val query = RoleTable.selectAll()
        ctx.queryParam("book-id")?.toLongOrNull()?.let {
            Book.findById(it)?.let { book -> query.andWhere { CreditTable.bookCol eq book.id } }
        }
        ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
        }
        ctx.queryParam("title")?.let { title ->
            query.andWhere { RoleTable.titleCol like "%$title%" }
        }
        ctx.json(Role.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
    }

    override fun create(ctx: Context) = ctx.processInput<RoleInput> { body ->
        Role.find(body.title)?.let {
            throw ConflictResponse("Role already exists")
        }
        val resource = Role.findOrCreate(body.title).apply {
            body.credits.forEach {
                Credit.new {
                    this.book = Book.findById(it.book) ?: throw NotFoundResponse("Book not found.")
                    this.creator = Creator.findById(it.creator) ?: throw NotFoundResponse("Creator not found.")
                    this.role = this@apply
                }
            }
            summary = body.summary
        }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
    }

    override fun update(ctx: Context) = manage<RoleInput>(ctx) { body, role ->
        Role.find(body.title)?.takeIf { it != role }?.let { throw ConflictResponse("Role already exists") }
        role.apply {
            credits.forEach { it.delete() }
            body.credits.forEach {
                Credit.findOrCreate(
                    Book.findById(it.book) ?: throw NotFoundResponse("Book not found."),
                    Creator.findById(it.creator) ?: throw NotFoundResponse("Creator not found."),
                    this,
                )
            }
            summary = body.summary
            title = body.title
        }
    }

    override fun delete(ctx: Context) = Utils.query {
        ctx.getResource().apply {
            credits.forEach { it.delete() }
            delete()
        }
        ctx.status(HttpStatus.NO_CONTENT)
    }

    fun addCredit(ctx: Context) = manage<RoleInput.Credit>(ctx) { body, role ->
        Credit.findOrCreate(
            Book.findById(body.book) ?: throw NotFoundResponse("Book not found."),
            Creator.findById(body.creator) ?: throw NotFoundResponse("Creator not found."),
            role,
        )
    }

    fun removeCredit(ctx: Context) = manage<RoleInput.Credit>(ctx) { body, role ->
        Credit
            .findOrCreate(
                Book.findById(body.book) ?: throw NotFoundResponse("Book not found."),
                Creator.findById(body.creator) ?: throw NotFoundResponse("Creator not found."),
                role,
            )?.delete()
    }
}
