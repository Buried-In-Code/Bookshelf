package duckpond.buriedincode.openlibrary.schemas.serializers

import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Locale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime?> {
  private val formatter =
    DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("yyyy-MM-dd")
      .optionalStart()
      .appendPattern("'T'HH:mm:ss")
      .optionalEnd()
      .optionalStart()
      .appendPattern(" HH:mm:ss")
      .optionalEnd()
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd()
      .toFormatter(Locale.ENGLISH)

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): LocalDateTime? {
    val dateTimeString = decoder.decodeString()
    if (dateTimeString.isBlank()) {
      return null
    }
    return try {
      java.time.LocalDateTime.parse(dateTimeString, formatter).toKotlinLocalDateTime()
    } catch (dtpe: DateTimeParseException) {
      throw dtpe
    }
  }

  override fun serialize(encoder: Encoder, value: LocalDateTime?) {
    if (value != null) {
      encoder.encodeString(value.toString())
    } else {
      encoder.encodeNull()
    }
  }
}
