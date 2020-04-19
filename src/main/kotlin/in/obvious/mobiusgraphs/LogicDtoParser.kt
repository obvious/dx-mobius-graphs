package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.dto.LogicDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

class LogicDtoParser {

    private val objectMapper = ObjectMapper(YAMLFactory())
        .apply {
        registerModule(KotlinModule())
    }

    fun fromFile(file: File): LogicDto {
        return objectMapper.readValue<LogicDto>(file, LogicDto::class.java)
    }

    fun fromString(data: String): LogicDto {
        return objectMapper.readValue<LogicDto>(data, LogicDto::class.java)
    }
}