package `in`.obvious.mobiusgraphs.mappers

import `in`.obvious.mobiusgraphs.core.*
import `in`.obvious.mobiusgraphs.dto.EffectDto
import `in`.obvious.mobiusgraphs.dto.LogicDto
import com.google.common.graph.*

@Suppress("UnstableApiUsage")
class LogicDtoToStateMachine {

    fun map(logicDto: LogicDto): Network<State, Event> {
        val network = NetworkBuilder
            .directed()
            .allowsSelfLoops(true)
            .build<State, Event>()

        logicDto
            .graph
            .map { (name, stateDto) -> createStateFromName(name, stateDto.effects) }
            .forEach { network.addNode(it) }

        val states: Map<String, State> = network.nodes().associateBy { it.name }
        val effects: Map<String, Effect> = states.values.flatMap { it.effects }.associateBy { it.name }

        val edgesFromEffects = edgesFromEffects(logicDto, states, effects)
        val edgesFromEvents = edgesFromEvents(logicDto, states)

        (edgesFromEffects + edgesFromEvents).forEach(network::addEdge)

        return ImmutableNetwork.copyOf(network)
    }

    private fun edgesFromEvents(
        logicDto: LogicDto,
        states: Map<String, State>
    ): List<Pair<EndpointPair<State>, ExternalEvent>> {
        return logicDto
            .graph
            .flatMap { (name, stateDto) ->
                val from = states.getValue(name)

                stateDto
                    .events
                    .map { (eventName, toState) ->
                        val to = states.getValue(toState)
                        val event = ExternalEvent(eventName, from)

                        EndpointPair.ordered(from, to) to event
                    }
            }
    }

    private fun edgesFromEffects(
        logicDto: LogicDto,
        states: Map<String, State>,
        effects: Map<String, Effect>
    ): List<Pair<EndpointPair<State>, InternalEvent>> {
        return logicDto
            .graph
            .flatMap { (name, stateDto) ->
                val from = states.getValue(name)

                stateDto
                    .effects
                    .flatMap { (effectName, events) ->
                        events.map { effectName to it }
                    }
                    .map { (effectName, eventToStateDto) ->
                        val to = states.getValue(eventToStateDto.toState)
                        val event = InternalEvent(eventToStateDto.name, effects.getValue(effectName))

                        EndpointPair.ordered(from, to) to event
                    }
            }
    }

    private fun createStateFromName(
        name: String,
        effectDtos: List<EffectDto>
    ): State {
        val effects = effectDtos
            .map { Effect(it.name) }
            .toSet()

        return if (name.startsWith("_")) Intermediate(name, effects) else Concrete(name, effects)
    }
}

@Suppress("UnstableApiUsage")
private fun <N, E> MutableNetwork<N, E>.addEdge(edge: Pair<EndpointPair<N>, E>) {
    addEdge(edge.first, edge.second)
}