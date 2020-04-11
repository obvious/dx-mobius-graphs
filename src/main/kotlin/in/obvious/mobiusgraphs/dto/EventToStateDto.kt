package `in`.obvious.mobiusgraphs.dto

import `in`.obvious.mobiusgraphs.fieldsMap
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

@JsonDeserialize(using = EventToStateDto.Deserializer::class)
data class EventToStateDto(
    val name: String,
    val toState: String
) {

    class Deserializer : StdDeserializer<EventToStateDto>(EventToStateDto::class.java) {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EventToStateDto {
            return with(p.codec.readTree<ObjectNode>(p)) {
                val fields = fieldsMap()

                require(fields.size == 1) {
                    val joinedKeys = fields.keys.joinToString()
                    "Exactly one key required per event! Found keys: [$joinedKeys]"
                }

                val (key, value) = fields.entries.first()

                require(value is TextNode) { "Expected value for '$key' to be text, but was ${value.nodeType}!" }
                require(!value.isNull) { "An event must always lead to a state, found <null>!" }

                EventToStateDto(key, value.asText())
            }
        }
    }
}