package `in`.obvious.mobiusgraphs.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EffectDtoParserTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    @Test
    fun `custom deserialization must work as expected`() {
        // given
        val json = """
            |{
            |  "FetchManifest": {
            |    "events": [
            |      {
            |        "FetchManifestFailed": "FetchFailed"
            |      },
            |      {
            |        "FetchManifestSucceeded": "Fetched"
            |      }
            |    ]
            |  }
            |}
        """.trimMargin()

        // when
        val parsed = objectMapper.readValue(json, EffectDto::class.java)

        // then
        val expected = EffectDto(
            name = "FetchManifest",
            events = listOf(
                EventToStateDto("FetchManifestFailed", "FetchFailed"),
                EventToStateDto("FetchManifestSucceeded", "Fetched")
            )
        )
        assertThat(parsed).isEqualTo(expected)
    }
}