package `in`.obvious.mobiusgraphs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

fun ObjectNode.fieldsMap(): Map<String, JsonNode> {
    return fields()
        .asSequence()
        .associateBy({ (fieldName, _) -> fieldName }, { (_, fieldValue) -> fieldValue })
}

inline fun <reified T : JsonNode> JsonNode?.isNullOrType(): Boolean {
    return this == null || this.isNull || this is T
}