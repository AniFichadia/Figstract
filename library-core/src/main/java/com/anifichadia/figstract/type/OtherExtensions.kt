@file:Suppress("NOTHING_TO_INLINE")

package com.anifichadia.figstract.type

inline fun noOp() = Unit

inline fun noOp(reason: String) = Unit

inline fun notImplemented(reason: String? = null): Nothing = if (reason != null) {
    throw NotImplementedError(reason)
} else {
    throw NotImplementedError()
}

fun Any?.toUnit() = Unit
