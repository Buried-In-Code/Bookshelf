package github.buriedincode.routers.api

import github.buriedincode.Utils.transaction
import github.buriedincode.models.Book
import github.buriedincode.models.IdInput
import github.buriedincode.models.ReadBook
import github.buriedincode.models.User
import github.buriedincode.models.UserInput
import github.buriedincode.tables.UserTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object UserApiRouter : BaseApiRouter<User>(entity = User) {
  override fun list(ctx: Context): Unit = transaction {
    val query = UserTable.selectAll()
    ctx.queryParam("username")?.let { username -> query.andWhere { UserTable.usernameCol like "%$username%" } }
    ctx.json(User.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
  }

  override fun create(ctx: Context) =
    ctx.processInput<UserInput> { body ->
      transaction {
        User.findOrNull(body.username)?.let { throw ConflictResponse("User already exists") }
        val resource =
          User.new User@{
            body.readBooks.forEach {
              ReadBook.new {
                this.book = Book.findById(it.book) ?: throw NotFoundResponse("Book not found.")
                this.user = this@User
                this.readDate = it.readDate
              }
            }
            this.imageUrl = body.imageUrl
            this.username = body.username
            this.wishedBooks =
              SizedCollection(body.wishedBooks.map { Book.findById(it) ?: throw NotFoundResponse("Book not found.") })
          }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
      }
    }

  override fun update(ctx: Context) =
    manage<UserInput>(ctx) { body, user ->
      User.findOrNull(body.username)?.takeIf { it != user }?.let { throw ConflictResponse("User already exists") }
      user.apply {
        imageUrl = body.imageUrl
        readBooks.forEach { it.delete() }
        body.readBooks.forEach {
          ReadBook.findOrCreate(Book.findById(it.book) ?: throw NotFoundResponse("Book not found."), user).apply {
            readDate = it.readDate
          }
        }
        username = body.username
        wishedBooks =
          SizedCollection(body.wishedBooks.map { Book.findById(it) ?: throw NotFoundResponse("Book not found.") })
      }
    }

  override fun delete(ctx: Context) = transaction {
    ctx.getResource().apply {
      readBooks.forEach { it.delete() }
      delete()
    }
    ctx.status(HttpStatus.NO_CONTENT)
  }

  fun addReadBook(ctx: Context) =
    manage<UserInput.ReadBook>(ctx) { body, user ->
      val book = Book.findById(body.book) ?: throw NotFoundResponse("No Book found.")
      if (!book.isCollected) {
        throw BadRequestResponse("Book hasn't been collected")
      }
      ReadBook.findOrCreate(book, user).apply { readDate = body.readDate }
    }

  fun removeReadBook(ctx: Context) =
    manage<IdInput>(ctx) { body, user ->
      val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
      if (!book.isCollected) {
        throw BadRequestResponse("Book hasn't been collected")
      }
      ReadBook.find(book, user)?.delete()
    }

  fun addWishedBook(ctx: Context) =
    manage<IdInput>(ctx) { body, user ->
      val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
      if (book.isCollected) {
        throw BadRequestResponse("Book has been collected")
      }
      user.wishedBooks = SizedCollection(user.wishedBooks + book)
    }

  fun removeWishedBook(ctx: Context) =
    manage<IdInput>(ctx) { body, user ->
      val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
      if (book.isCollected) {
        throw BadRequestResponse("Book has been collected")
      }
      user.wishedBooks = SizedCollection(user.wishedBooks - book)
    }
}
