package `in`.obvious.mobiusgraphs.dto

import `in`.obvious.mobiusgraphs.fieldsMap
import `in`.obvious.mobiusgraphs.isNullOrType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

@JsonDeserialize(using = EffectDto.Deserializer::class)
data class EffectDto(
    val name: String,
    val events: List<EventToStateDto>
) {
    class Deserializer : StdDeserializer<EffectDto>(EffectDto::class.java) {

        private val eventListType = object : TypeReference<List<EventToStateDto>>() {}

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EffectDto {
            return with(p.codec.readTree<ObjectNode>(p)) {
                val fields = fieldsMap()

                require(fields.size == 1) {
                    val joinedKeys = fields.keys.joinToString()
                    "Exactly one key required per effect! Found keys: [$joinedKeys]"
                }

                val key = fields.entries.first().key

                val eventsNode = get(key).get("events")

                require(eventsNode.isNullOrType<ArrayNode>()) {
                    "Expected 'events' to be an array!"
                }

                val events = if (eventsNode != null && !eventsNode.isNull)
                    eventsNode.traverse(p.codec).readValueAs<List<EventToStateDto>>(eventListType)
                else
                    emptyList()

                EffectDto(key, events)
            }
        }
    }
}