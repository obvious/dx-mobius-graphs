package `in`.obvious.mobiusgraphs.mappers

import `in`.obvious.mobiusgraphs.core.*
import `in`.obvious.mobiusgraphs.dto.EffectDto
import `in`.obvious.mobiusgraphs.dto.EventToStateDto
import `in`.obvious.mobiusgraphs.dto.LogicDto
import `in`.obvious.mobiusgraphs.dto.StateDto
import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class StateMachineGeneratorTest {

    private val generator = LogicDtoToStateMachine()

    @Test
    fun `a node should be created in the graph for every state`() {
        // given
        val logic = logicDto(
            "",
            "First" to stateDto(),
            "Second" to stateDto(),
            "Third" to stateDto()
        )

        // when
        val actual = generator.map(logic)

        // then
        val expected = mutableNetwork().apply {
            addNode(Concrete("First"))
            addNode(Concrete("Second"))
            addNode(Concrete("Third"))
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `a node name starting with an underscore should be added as a transient node`() {
        // given
        val logic = logicDto(
            "",
            "First" to stateDto(),
            "_Second" to stateDto(effects = listOf(EffectDto("FirstEffect"))),
            "Third" to stateDto()
        )

        // when
        val actual = generator.map(logic)

        // then
        val expected = mutableNetwork().apply {
            addNode(Concrete("First"))
            addNode(Intermediate("_Second", setOf(Effect("FirstEffect"))))
            addNode(Concrete("Third"))
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `effects should be added for all the nodes`() {
        // given
        val logic = logicDto(
            "",
            "First" to stateDto(),
            "_Second" to stateDto(effects = listOf(EffectDto("FirstEffect"), EffectDto("ThirdEffect"))),
            "Third" to stateDto(effects = listOf(EffectDto("SecondEffect"))),
            "Fourth" to stateDto(effects = listOf(EffectDto("FourthEffect"), EffectDto("FifthEffect")))
        )

        // when
        val actual = generator.map(logic)

        // then
        val expected = mutableNetwork().apply {
            addNode(Concrete("First"))
            addNode(Intermediate("_Second", setOf(Effect("FirstEffect"), Effect("ThirdEffect"))))
            addNode(Concrete("Third", setOf(Effect("SecondEffect"))))
            addNode(Concrete("Fourth", setOf(Effect("FourthEffect"), Effect("FifthEffect"))))
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `an edge should be added for every event to a state from every effect in a node`() {
        // given
        val logic = logicDto(
            "",
            "First" to stateDto(
                effects = listOf(
                    EffectDto(
                        name = "FirstEffect", events = listOf(
                            EventToStateDto(name = "FirstEvent", toState = "_Second"),
                            EventToStateDto(name = "SecondEvent", toState = "First")
                        )
                    ),
                    EffectDto(
                        name = "SecondEffect", events = listOf(
                            EventToStateDto(name = "ThirdEvent", toState = "Third")
                        )
                    )
                )
            ),
            "_Second" to stateDto(
                effects = listOf(
                    EffectDto(
                        name = "FourthEffect", events = listOf(
                            EventToStateDto(name = "FourthEvent", toState = "Fourth")
                        )
                    )
                )
            ),
            "Third" to stateDto(
                effects = listOf(
                    EffectDto(
                        name = "ThirdEffect", events = listOf(
                            EventToStateDto(name = "FirstEvent", toState = "_Second")
                        )
                    )
                )
            ),
            "Fourth" to stateDto()
        )

        // when
        val actual = generator.map(logic)

        // then
        val firstEffect = Effect("FirstEffect")
        val secondEffect = Effect("SecondEffect")
        val thirdEffect = Effect("ThirdEffect")
        val fourthEffect = Effect("FourthEffect")

        val expected = mutableNetwork().apply {
            val first = Concrete(_name = "First", _effects = setOf(firstEffect, secondEffect))
            val second = Intermediate(_name = "_Second", _effects = setOf(fourthEffect))
            val third = Concrete("Third", _effects = setOf(thirdEffect))
            val fourth = Concrete("Fourth")

            addNode(first)
            addNode(second)
            addNode(third)
            addNode(fourth)

            addEdge(first, second, InternalEvent(_name = "FirstEvent", source = firstEffect))
            addEdge(first, first, InternalEvent(_name = "SecondEvent", source = firstEffect))
            addEdge(first, third, InternalEvent(_name = "ThirdEvent", source = secondEffect))
            addEdge(second, fourth, InternalEvent(_name = "FourthEvent", source = fourthEffect))
            addEdge(third, second, InternalEvent(_name = "FirstEvent", source = thirdEffect))
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `an edge should be added for every event of a state to another state`() {
        // given
        val logic = logicDto(
            "",
            "First" to stateDto(
                events = listOf(
                    EventToStateDto(name = "FirstEvent", toState = "_Second"),
                    EventToStateDto(name = "SecondEvent", toState = "First"),
                    EventToStateDto(name = "ThirdEvent", toState = "Third")
                )
            ),
            "_Second" to stateDto(
                effects = listOf(EffectDto("FirstEffect")),
                events = listOf(EventToStateDto(name = "FourthEvent", toState = "Fourth"))
            ),
            "Third" to stateDto(
                events = listOf(EventToStateDto(name = "FirstEvent", toState = "_Second"))
            ),
            "Fourth" to stateDto()
        )

        // when
        val actual = generator.map(logic)

        // then
        val expected = mutableNetwork().apply {
            val first = Concrete(_name = "First")
            val second = Intermediate(_name = "_Second", _effects = setOf(Effect("FirstEffect")))
            val third = Concrete("Third")
            val fourth = Concrete("Fourth")

            addNode(first)
            addNode(second)
            addNode(third)
            addNode(fourth)

            addEdge(first, second, ExternalEvent(_name = "FirstEvent", source = first))
            addEdge(first, first, ExternalEvent(_name = "SecondEvent", source = first))
            addEdge(first, third, ExternalEvent(_name = "ThirdEvent", source = first))
            addEdge(second, fourth, ExternalEvent(_name = "FourthEvent", source = second))
            addEdge(third, second, ExternalEvent(_name = "FirstEvent", source = third))
        }
        assertThat(actual).isEqualTo(expected)
    }

    private fun mutableNetwork(): MutableNetwork<State, Event> {
        return NetworkBuilder
            .directed()
            .allowsSelfLoops(true)
            .build()
    }

    private fun stateDto(
        events: List<EventToStateDto> = emptyList(),
        effects: List<EffectDto> = emptyList()
    ): StateDto = StateDto(events, effects)

    private fun logicDto(
        name: String,
        vararg states: Pair<String, StateDto>
    ): LogicDto = LogicDto(name, states.toMap())
}