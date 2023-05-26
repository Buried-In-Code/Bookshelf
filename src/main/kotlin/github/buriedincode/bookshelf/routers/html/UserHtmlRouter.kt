package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.*
import org.jetbrains.exposed.dao.load

object UserHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val users = User.all().toList()
        ctx.render(filePath = "templates/user/list_users.kte", mapOf("users" to users))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val userId = ctx.pathParam("user-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "User not found")
        val user = User.findById(id = userId)
            ?.load(User::readBooks, User::wishedBooks)
            ?: throw NotFoundResponse(message = "User not found")
        ctx.render(filePath = "templates/user/view_user.kte", mapOf("user" to user))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val userId = ctx.pathParam("user-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "User not found")
        val user = User.findById(id = userId)
            ?.load(User::readBooks, User::wishedBooks)
            ?: throw NotFoundResponse(message = "User not found")
        val readBooks = Book.all().toList().filterNot { it in user.readBooks }
        val wishedBooks = Book.all().toList().filterNot { it in user.wishedBooks }
        ctx.render(filePath = "templates/user/edit_user.kte", mapOf(
            "user" to user,
            "readBooks" to readBooks,
            "wishedBooks" to wishedBooks
        ))
    }
}