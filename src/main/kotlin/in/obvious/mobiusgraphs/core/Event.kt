package `in`.obvious.mobiusgraphs.core

sealed class Event(val name: String)

data class InternalEvent(
    private val _name: String,
    val source: Effect
): Event(_name)

data class ExternalEvent(
    private val _name: String,
    val source: State
): Event(_name)