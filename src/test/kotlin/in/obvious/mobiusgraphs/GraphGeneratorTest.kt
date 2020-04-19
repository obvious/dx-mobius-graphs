package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.mappers.LogicDtoToStateMachine
import org.approvaltests.Approvals.verify
import org.junit.jupiter.api.Test

class GraphGeneratorTest {

    private val testData = """
        name: ChangeLanguage
        graph:
          Fetching:
            effects:
              - FetchManifest:
                  events:
                    - FetchManifestFailed: FetchFailed
                    - FetchManifestSucceeded: Fetched
          FetchFailed:
            events:
              - Retry: Fetching
          Fetched:
            events:
              - SelectCountry: CountrySelected
          CountrySelected:
            events:
              - SelectCountry: CountrySelected
              - SaveCountry: _SaveCountry
          _SaveCountry:
            effects:
              - SaveCountry: 
                  events:
                    - CountrySaved: _GoToNext
          _GoToNext:
            effects:
              - GoToNextScreen:
                  events: []
    """

    @Test
    fun `graph should be generated correctly`() {
        val logicDto = LogicDtoParser().fromString(testData)
        val logicGraph = LogicDtoToStateMachine().map(logicDto)

        val generator = GraphGenerator()
        verify(generator.generate(logicGraph, logicDto.name))
    }
}