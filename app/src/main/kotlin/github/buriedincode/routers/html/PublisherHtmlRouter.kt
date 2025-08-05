package github.buriedincode.routers.html

import github.buriedincode.models.Publisher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.NotImplementedResponse

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher, plural = "publishers") {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}

  override fun list(ctx: Context) = throw NotImplementedResponse()

  override fun create(ctx: Context) = throw NotImplementedResponse()
}
