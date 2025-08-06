package github.buriedincode.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate

class LocalDateDeserializer : JsonDeserializer<LocalDate?>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): LocalDate? {
    val mapper = parser.codec as ObjectMapper
    val root = mapper.readTree(parser) as JsonNode
    if (root.isNull || root.asText() == "null") {
      return null
    }
    return java.time.LocalDate.parse(root.asText(), DateTimeFormatter.ISO_DATE).toKotlinLocalDate()
  }
}
