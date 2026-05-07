package duckpond.buriedincode

import duckpond.buriedincode.controllers.BookController
import duckpond.buriedincode.controllers.SeriesController
import duckpond.buriedincode.controllers.UserController
import duckpond.buriedincode.services.getSessionOrNull
import duckpond.buriedincode.services.setSession
import gg.jte.ContentType as JteType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.ContentType
import io.javalin.http.HttpStatus
import io.javalin.json.JavalinJackson3
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Path
import kotlin.io.path.div

object Server {
  private val LOGGER = KotlinLogging.logger {}

  private fun createTemplateEngine(): TemplateEngine {
    return if (SETTINGS.precompileJte) {
      TemplateEngine.createPrecompiled(Path.of("jte-classes"), JteType.Html)
    } else {
      val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
      TemplateEngine.create(codeResolver, JteType.Html)
    }
  }

  private fun createJavalinApp(renderer: FileRenderer): Javalin {
    return Javalin.create {
      it.fileRenderer(fileRenderer = renderer)
      it.http.prefer405over404 = true
      it.http.defaultContentType = ContentType.JSON
      it.jsonMapper(JavalinJackson3())
      it.requestLogger.http { ctx, ms ->
        val level =
          when {
            ctx.statusCode() in (100..<200) -> Level.WARN
            ctx.statusCode() in (200..<300) -> Level.INFO
            ctx.statusCode() in (300..<400) -> Level.INFO
            ctx.statusCode() in (400..<500) -> Level.WARN
            else -> Level.ERROR
          }
        LOGGER.log(level) { "${ctx.statusCode()}: ${ctx.method()} - ${ctx.path()} => ${ms.toHumanReadable()}" }
      }
      it.router.caseInsensitiveRoutes = true
      it.router.ignoreTrailingSlashes = true
      it.router.treatMultipleSlashesAsSingleSlash = true
      it.routes.apiBuilder {
        path("/") {
          get { ctx ->
            transaction {
              ctx.getSessionOrNull()?.let { ctx.redirect("/users/${ it.id.value }") } ?: ctx.redirect("/books")
            }
          }
          get("search", BookController::searchPage)
          path("books") {
            get(BookController::listPage)
            path("{book-id}") {
              get(BookController::viewPage)
              get("edit", BookController::editPage)
              get("image", BookController::bookImage)
            }
          }
          path("series") {
            get(SeriesController::listPage)
            get("{series-id}", SeriesController::viewPage)
          }
          path("users") {
            get(UserController::listPage)
            get("{user-id}", UserController::viewPage)
          }
          post("cookie") { ctx ->
            ctx.setSession()
            ctx.status(HttpStatus.NO_CONTENT)
          }
        }
        path("api") {
          path("books") {
            post(BookController::add)
            path("{book-id}") {
              patch(BookController::update)
              post(BookController::updateState)
            }
          }
        }
      }
      it.staticFiles.add {
        it.hostedPath = "/static"
        it.directory = "/static"
      }
    }
  }

  fun start() {
    val engine = createTemplateEngine()
    engine.setTrimControlStructures(true)
    val renderer = JavalinJte(templateEngine = engine)

    val app = createJavalinApp(renderer = renderer)
    app.start(SETTINGS.website.host, SETTINGS.website.port)
  }
}

fun main(@Suppress("UNUSED_PARAMETER") vararg args: String) {
  println("Bookshelf v${VERSION}")
  println("Kotlin v${KotlinVersion.CURRENT}; Java v${System.getProperty("java.version")}")
  println("${System.getProperty("os.name")} ${System.getProperty("os.arch")}")

  println(SETTINGS)
  Server.start()
}
