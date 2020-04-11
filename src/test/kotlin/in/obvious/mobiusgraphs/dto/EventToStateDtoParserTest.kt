package `in`.obvious.mobiusgraphs.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EventToStateDtoParserTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    @Test
    fun `custom deserialization must work as expected`() {
        // given
        val json = """
            |[
            |   {
            |       "FetchManifestFailed": "FetchFailed"   
            |   },
            |   {
            |       "FetchManifestSucceeded": "Fetched"
            |   }
            |]
        """.trimMargin()

        // when
        val type = object : TypeReference<List<EventToStateDto>>() {}
        val parsed = objectMapper.readValue(json, type)

        // then
        assertThat(parsed)
            .containsExactly(
                EventToStateDto("FetchManifestFailed", "FetchFailed"),
                EventToStateDto("FetchManifestSucceeded", "Fetched")
            )
            .inOrder()
    }
}