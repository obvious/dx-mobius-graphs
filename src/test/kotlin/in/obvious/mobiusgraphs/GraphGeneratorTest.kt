package `in`.obvious.mobiusgraphs

import org.approvaltests.Approvals.verify
import org.junit.jupiter.api.Test

class GraphGeneratorTest {

    @Test
    fun `graph should be generated correctly`() {
        val generator = GraphGenerator()
        verify(generator.generateFromFile("sample.yaml"))
    }
}