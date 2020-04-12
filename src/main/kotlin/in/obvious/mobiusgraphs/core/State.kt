package `in`.obvious.mobiusgraphs.core

sealed class State(
    val name: String,
    val effects: Set<Effect>
)

data class Concrete(
    private val _name: String,
    private val _effects: Set<Effect> = emptySet()
) : State(_name, _effects)

data class Intermediate(
    private val _name: String,
    private val _effects: Set<Effect>
) : State(_name, _effects) {
    init {
        if (_effects.isEmpty()) throw IllegalArgumentException("Intermediate state must have at least one effect!")
    }
}