package com.anifichadia.figstract.type

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

fun <T> List<T>.replaceOrAdd(predicate: (T) -> Boolean, replacement: () -> T): List<T> {
    val index = this.indexOfFirst(predicate)
    return this.toMutableList()
        .apply {
            if (index >= 0) {
                removeAt(index)
                add(index, replacement())
            } else {
                add(replacement())
            }
        }
        .toList()
}
