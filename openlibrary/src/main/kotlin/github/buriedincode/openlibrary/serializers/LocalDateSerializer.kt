package github.buriedincode.openlibrary.serializers

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
object LocalDateSerializer : KSerializer<LocalDate?> {
  @JvmStatic private val LOGGER = KotlinLogging.logger {}
  private val patterns =
    setOf(
      "dd MMM yyyy",
      "yyyy-MM-dd",
      "MMMM d, yyyy",
      "yyyy-MMM-dd",
      "MMM dd, yyyy",
      "d MMMM yyyy",
      "dd/MM/yyyy",
      "yyyy.",
      "yyyy?",
      "yyyy",
      "MMMM yyyy",
      "MMM, yyyy",
    )

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): LocalDate? {
    val dateString =
      (decoder.decodeSerializableValue(JsonElement.serializer()) as? JsonPrimitive)?.content ?: return null
    patterns.forEach { pattern ->
      var builder = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
      if (!pattern.contains("d")) builder = builder.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
      if (!pattern.contains("M")) builder = builder.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
      val formatter = builder.toFormatter(Locale.ENGLISH)
      try {
        LOGGER.debug { "Parsing date string: '$dateString' with pattern: '$pattern'" }
        return java.time.LocalDate.parse(dateString, formatter).toKotlinLocalDate()
      } catch (_: java.time.format.DateTimeParseException) {}
    }
    LOGGER.warn { "Unable to parse date string: '$dateString'" }
    return null
  }

  override fun serialize(encoder: Encoder, value: LocalDate?) {
    encoder.encodeNullableSerializableValue(JsonElement.serializer(), value?.toString()?.let { JsonPrimitive(it) })
  }
}
