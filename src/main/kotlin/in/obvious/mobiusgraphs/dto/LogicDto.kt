package `in`.obvious.mobiusgraphs.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class LogicDto(

    @JsonProperty("name")
    val name: String,

    @JsonProperty("graph")
    val graph: Map<String, StateDto>
)