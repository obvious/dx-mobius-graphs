package `in`.obvious.mobiusgraphs

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> classLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun logger(): Logger = LoggerFactory.getLogger("Global")