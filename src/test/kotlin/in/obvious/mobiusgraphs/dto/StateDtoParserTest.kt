package `in`.obvious.mobiusgraphs.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StateDtoParserTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    @Test
    fun `custom deserialization must work as expected`() {
        // given
        val json = """
            |{
            |  "effects": [
            |    {
            |      "FetchManifest": {
            |         "events": [
            |           {
            |               "FetchManifestFailed": "FetchFailed"
            |           },
            |           {
            |               "FetchManifestSucceeded": "Fetched"
            |           }
            |         ]
            |      }
            |    }
            |  ],
            |  "events": [
            |       {
            |           "Failed": "Succeeded"
            |       },
            |       {
            |           "Retry": "Failed"
            |       }
            |  ]
            |}
        """.trimMargin()

        // when
        val parsed = objectMapper.readValue(json, StateDto::class.java)

        // then
        val expected = StateDto(
            events = listOf(
                EventToStateDto("Failed", "Succeeded"),
                EventToStateDto("Retry", "Failed")
            ),
            effects = listOf(
                EffectDto(
                    name = "FetchManifest",
                    events = listOf(
                        EventToStateDto("FetchManifestFailed", "FetchFailed"),
                        EventToStateDto("FetchManifestSucceeded", "Fetched")
                    )
                )
            )
        )
        assertThat(parsed).isEqualTo(expected)
    }
}