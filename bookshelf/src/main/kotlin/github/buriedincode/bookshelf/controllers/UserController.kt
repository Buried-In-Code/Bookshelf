package github.buriedincode.bookshelf.controllers

import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.User
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context

object UserController : LongController<User>(entity = User) {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  private fun monitoredListPage(ctx: Context, title: String, results: (resource: User) -> List<Book>) = transaction {
    val session = ctx.getSession()
    if (session == null) {
      ctx.redirect("/")
      return@transaction
    }
    val resource = ctx.getResource()
    renderResource(ctx, "monitored_list", mapOf("title" to title, "results" to results(resource)))
  }

  fun collectionPage(ctx: Context) =
    monitoredListPage(ctx = ctx, title = "Collection") { it.collected.map { it.book }.distinct() }

  fun readListPage(ctx: Context) =
    monitoredListPage(ctx = ctx, title = "Read List") { it.read.map { it.book }.distinct() }

  fun wishListPage(ctx: Context) =
    monitoredListPage(ctx = ctx, title = "Wish List") { it.wished.map { it.book }.distinct() }
}
