package `in`.obvious.mobiusgraphs.dto

import `in`.obvious.mobiusgraphs.isNullOrType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

@JsonDeserialize(using = StateDto.Deserializer::class)
data class StateDto(
    val events: List<EventToStateDto>,
    val effects: List<EffectDto>
) {

    class Deserializer : StdDeserializer<StateDto>(StateDto::class.java) {

        private val eventListType = object : TypeReference<List<EventToStateDto>>() {}
        private val effectsListType = object : TypeReference<List<EffectDto>>() {}

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): StateDto {
            return with(p.codec.readTree<ObjectNode>(p)) {
                val eventsNode = get("events")
                val effectsNode = get("effects")

                require(eventsNode.isNullOrType<ArrayNode>()) {
                    "Expected 'events' to be an array!"
                }

                require(effectsNode.isNullOrType<ArrayNode>()) {
                    "Expected 'effects' to be an array!"
                }

                val events = if (eventsNode != null && !eventsNode.isNull)
                    eventsNode.traverse(p.codec).readValueAs<List<EventToStateDto>>(eventListType)
                else
                    emptyList()

                val effects = if (effectsNode != null && !effectsNode.isNull)
                    effectsNode.traverse(p.codec).readValueAs<List<EffectDto>>(effectsListType)
                else
                    emptyList()

                StateDto(events, effects)
            }
        }
    }
}