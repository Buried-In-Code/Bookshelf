package github.buriedincode.openlibrary.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
object DescriptionSerializer : KSerializer<String?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Description", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): String? {
    return when (val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())) {
      is JsonObject -> jsonElement["value"]?.jsonPrimitive?.content
      is JsonPrimitive -> jsonElement.content
      else -> null
    }
  }

  override fun serialize(encoder: Encoder, value: String?) {
    if (value != null) {
      encoder.encodeString(value)
    } else {
      encoder.encodeNull()
    }
  }
}
