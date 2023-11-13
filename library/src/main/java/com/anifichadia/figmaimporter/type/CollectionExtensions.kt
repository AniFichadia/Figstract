package com.anifichadia.figmaimporter.type

fun Collection<String>.contains(other: String, ignoreCase: Boolean = false): Boolean {
    return find { it.equals(other, ignoreCase = ignoreCase) } != null
}

fun <T> MutableCollection<T>.removeIf(predicate: (T) -> Boolean) {
    this.filter(predicate).forEach(::remove)
}

fun <T> Collection<T>.mapIf(predicate: (T) -> Boolean, block: (T) -> T): Collection<T> {
    return this.map {
        if (predicate(it)) {
            block(it)
        } else {
            it
        }
    }
}
