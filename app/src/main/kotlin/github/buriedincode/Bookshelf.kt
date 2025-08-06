package github.buriedincode

import gg.jte.ContentType as JteType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.Utils.log
import github.buriedincode.Utils.toHumanReadable
import github.buriedincode.Utils.transaction
import github.buriedincode.models.Creator
import github.buriedincode.models.Publisher
import github.buriedincode.models.Role
import github.buriedincode.models.Series
import github.buriedincode.models.User
import github.buriedincode.routers.api.BookApiRouter
import github.buriedincode.routers.api.CreatorApiRouter
import github.buriedincode.routers.api.PublisherApiRouter
import github.buriedincode.routers.api.RoleApiRouter
import github.buriedincode.routers.api.SeriesApiRouter
import github.buriedincode.routers.api.UserApiRouter
import github.buriedincode.routers.html.BookHtmlRouter
import github.buriedincode.routers.html.CreatorHtmlRouter
import github.buriedincode.routers.html.PublisherHtmlRouter
import github.buriedincode.routers.html.RoleHtmlRouter
import github.buriedincode.routers.html.SeriesHtmlRouter
import github.buriedincode.routers.html.UserHtmlRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.ContentType
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Path
import kotlin.io.path.div

object Bookshelf {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  private fun createTemplateEngine(): TemplateEngine {
    val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
    return TemplateEngine.create(codeResolver, JteType.Html)
    // return TemplateEngine.createPrecompiled(Path.of("jte-classes"), JteType.Html)
  }

  private fun createJavalinApp(renderer: FileRenderer): Javalin {
    return Javalin.create {
      it.fileRenderer(fileRenderer = renderer)
      it.http.prefer405over404 = true
      it.http.defaultContentType = ContentType.JSON
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
      it.router.ignoreTrailingSlashes = true
      it.router.treatMultipleSlashesAsSingleSlash = true
      it.router.caseInsensitiveRoutes = true
      it.router.apiBuilder {
        path("/") {
          get { ctx ->
            transaction {
              ctx.render(
                filePath = "templates/index.kte",
                model =
                  mapOf(
                    "session" to ctx.cookie("bookshelf_session-id")?.toLongOrNull()?.let { User.findById(it) },
                    "users" to User.all().sorted().toList(),
                  ),
              )
            }
          }
          path("books") {
            get(BookHtmlRouter::list)
            get("create", BookHtmlRouter::create)
            get("import", BookHtmlRouter::import)
            get("search", BookHtmlRouter::search)
            path("{book-id}") {
              get(BookHtmlRouter::view)
              get("update", BookHtmlRouter::update)
            }
          }
          path("creators") {
            path("{creator-id}") {
              get(CreatorHtmlRouter::view)
              get("update", CreatorHtmlRouter::update)
            }
          }
          path("publishers") {
            path("{publisher-id}") {
              get(PublisherHtmlRouter::view)
              get("update", PublisherHtmlRouter::update)
            }
          }
          path("roles") {
            path("{role-id}") {
              get(RoleHtmlRouter::view)
              get("update", RoleHtmlRouter::update)
            }
          }
          path("series") {
            get(SeriesHtmlRouter::list)
            get("create", SeriesHtmlRouter::create)
            path("{series-id}") {
              get(SeriesHtmlRouter::view)
              get("update", SeriesHtmlRouter::update)
            }
          }
          path("users") {
            get(UserHtmlRouter::list)
            get("create", UserHtmlRouter::create)
            path("{user-id}") {
              get(UserHtmlRouter::view)
              get("readlist", UserHtmlRouter::readlist)
              get("update", UserHtmlRouter::update)
              get("wishlist", UserHtmlRouter::wishlist)
            }
          }
        }
        path("api") {
          delete("clean") { ctx ->
            transaction {
              Creator.all().filter { it.credits.empty() }.forEach { it.delete() }
              Publisher.all().filter { it.books.empty() }.forEach { it.delete() }
              Role.all().filter { it.credits.empty() }.forEach { it.delete() }
              Series.all().filter { it.books.empty() }.forEach { it.delete() }
            }
          }
          path("books") {
            post("search", BookApiRouter::search)
            get(BookApiRouter::list)
            post(BookApiRouter::create)
            post("import", BookApiRouter::import)
            path("{book-id}") {
              get(BookApiRouter::read)
              put(BookApiRouter::update)
              delete(BookApiRouter::delete)
              post("import", BookApiRouter::reimport)
              path("collect") {
                post(BookApiRouter::collectBook)
                delete(BookApiRouter::discardBook)
              }
              path("wish") {
                post(BookApiRouter::addWisher)
                delete(BookApiRouter::removeWisher)
              }
              path("read") {
                post(BookApiRouter::addReader)
                delete(BookApiRouter::removeReader)
              }
              path("credits") {
                post(BookApiRouter::addCredit)
                delete(BookApiRouter::removeCredit)
              }
              path("series") {
                post(BookApiRouter::addSeries)
                delete(BookApiRouter::removeSeries)
              }
            }
          }
          path("creators") {
            get(CreatorApiRouter::list)
            post(CreatorApiRouter::create)
            path("{creator-id}") {
              get(CreatorApiRouter::read)
              put(CreatorApiRouter::update)
              delete(CreatorApiRouter::delete)
              path("credits") {
                post(CreatorApiRouter::addCredit)
                delete(CreatorApiRouter::removeCredit)
              }
            }
          }
          path("publishers") {
            get(PublisherApiRouter::list)
            post(PublisherApiRouter::create)
            path("{publisher-id}") {
              get(PublisherApiRouter::read)
              put(PublisherApiRouter::update)
              delete(PublisherApiRouter::delete)
              path("books") {
                post(PublisherApiRouter::addBook)
                delete(PublisherApiRouter::removeBook)
              }
            }
          }
          path("roles") {
            get(RoleApiRouter::list)
            post(RoleApiRouter::create)
            path("{role-id}") {
              get(RoleApiRouter::read)
              put(RoleApiRouter::update)
              delete(RoleApiRouter::delete)
              path("credits") {
                post(RoleApiRouter::addCredit)
                delete(RoleApiRouter::removeCredit)
              }
            }
          }
          path("series") {
            get(SeriesApiRouter::list)
            post(SeriesApiRouter::create)
            path("{series-id}") {
              get(SeriesApiRouter::read)
              put(SeriesApiRouter::update)
              delete(SeriesApiRouter::delete)
              path("books") {
                post(SeriesApiRouter::addBook)
                delete(SeriesApiRouter::removeBook)
              }
            }
          }
          path("users") {
            get(UserApiRouter::list)
            post(UserApiRouter::create)
            path("{user-id}") {
              get(UserApiRouter::read)
              put(UserApiRouter::update)
              delete(UserApiRouter::delete)
              path("read") {
                post(UserApiRouter::addReadBook)
                delete(UserApiRouter::removeReadBook)
              }
              path("wished") {
                post(UserApiRouter::addWishedBook)
                delete(UserApiRouter::removeWishedBook)
              }
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

  fun start(settings: Settings) {
    val engine = createTemplateEngine()
    engine.setTrimControlStructures(true)
    val renderer = JavalinJte(templateEngine = engine)

    val app = createJavalinApp(renderer = renderer)
    app.start(settings.website.host, settings.website.port)
  }
}

fun main(@Suppress("UNUSED_PARAMETER") vararg args: String) {
  println("$PROJECT_NAME v$VERSION")
  println("Kotlin v${KotlinVersion.CURRENT}")
  println("Java v${System.getProperty("java.version")}")
  println("Arch: ${System.getProperty("os.arch")}")

  val settings = Settings.load()
  println(settings)

  Bookshelf.start(settings = settings)
}
