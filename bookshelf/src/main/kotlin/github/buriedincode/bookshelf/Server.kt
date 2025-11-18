package github.buriedincode.bookshelf

// import github.buriedincode.bookshelf.controllers.SeriesController
import gg.jte.ContentType as JteType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.Utils.log
import github.buriedincode.bookshelf.Utils.settings
import github.buriedincode.bookshelf.Utils.toHumanReadable
import github.buriedincode.bookshelf.Utils.transaction
import github.buriedincode.bookshelf.controllers.AuthenticationController
import github.buriedincode.bookshelf.controllers.AuthenticationController.getSession
import github.buriedincode.bookshelf.controllers.BookController
import github.buriedincode.bookshelf.controllers.UserController
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.ContentType
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Path
import kotlin.io.path.div
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler

object Server {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  private fun createTemplateEngine(environment: Settings.Environment): TemplateEngine {
    return if (environment == Settings.Environment.DEV) {
      val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
      TemplateEngine.create(codeResolver, JteType.Html)
    } else {
      TemplateEngine.createPrecompiled(Path.of("jte-classes"), JteType.Html)
    }
  }

  private fun createSessionHandler(): SessionHandler {
    return SessionHandler().apply {
      sessionCache =
        DefaultSessionCache(this).apply {
          sessionDataStore =
            FileSessionDataStore().apply { storeDir = (Utils.CACHE_ROOT / "session-store").toFile().apply { mkdirs() } }
        }
      httpOnly = true
      isSecureRequestOnly = true
      sameSite = HttpCookie.SameSite.STRICT
    }
  }

  private fun createJavalinApp(renderer: FileRenderer): Javalin {
    return Javalin.create {
      it.fileRenderer(fileRenderer = renderer)
      it.http.prefer405over404 = true
      it.http.defaultContentType = ContentType.JSON
      it.jetty.modifyServletContextHandler { it.sessionHandler = createSessionHandler() }
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
      it.router.apiBuilder {
        path("/") {
          get { ctx ->
            transaction {
              ctx.getSession()?.let { ctx.redirect("/users/${it.id.value}") } ?: ctx.render("templates/index.kte")
            }
          }
          post("register", AuthenticationController::register)
          post("login", AuthenticationController::login)
          delete("logout", AuthenticationController::logout)
          path("books") {
            get(BookController::listBooksPage)
            path("{book-id}") {
              get(BookController::viewBookPage)
              get("image", BookController::bookImage)
              path("tabs") {
                get("overview", BookController::overviewTabPartial)
                get("credits", BookController::creditsTabPartial)
                get("readers", BookController::readersTabPartial)
                get("wishers", BookController::wishersTabPartial)
              }
            }
          }
          // path("series") {
          //   get(SeriesController::listPage)
          //   get("{series-id}", SeriesController::readPage)
          // }
          path("users") {
            get(UserController::listUsersPage)
            get("{user-id}", UserController::viewUserPage)
          }
        }
        path("api") {
          path("books") {
            path("{book-id}") {
              post(BookController::refresh)
              post("collect", BookController::toggleCollected)
              post("read", BookController::toggleRead)
              post("wish", BookController::toggleWished)
            }
          }
          // path("series") { path("{series-id}") { post(SeriesController::refresh) } }
        }
      }
      it.staticFiles.add {
        it.hostedPath = "/static"
        it.directory = "/static"
      }
    }
  }

  fun start(settings: Settings) {
    val engine = createTemplateEngine(environment = settings.environment)
    engine.setTrimControlStructures(true)
    val renderer = JavalinJte(templateEngine = engine)

    val app = createJavalinApp(renderer = renderer)
    app.start(settings.website.host, settings.website.port)
  }
}

fun main(@Suppress("UNUSED_PARAMETER") vararg args: String) {
  println("Bookshelf v$VERSION")
  println("Kotlin v${KotlinVersion.CURRENT}; Java v${System.getProperty("java.version")}")
  println("${System.getProperty("os.name")} ${System.getProperty("os.arch")}")

  println(settings)
  Server.start(settings = settings)
}
