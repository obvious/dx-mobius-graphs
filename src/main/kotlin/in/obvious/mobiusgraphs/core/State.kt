package `in`.obvious.mobiusgraphs.core

sealed class State

data class Concrete(
    val name: String,
    val effects: Set<Effect> = emptySet()
): State()

data class Transient(
    val effects: Set<Effect>
): State() {
    init {
        if (effects.isEmpty()) throw IllegalArgumentException("Transient state must have at least one effect!")
    }
}