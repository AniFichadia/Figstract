package com.anifichadia.figstract.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <Subject, ValueT : Any?> Subject.ifNotNull(
    value: ValueT?,
    block: Subject.(ValueT) -> Subject,
): Subject {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (value != null) {
        block(value)
    } else {
        this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <Subject> Subject.ifC(
    predicate: Boolean,
    block: Subject.() -> Subject,
): Subject {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (predicate) {
        block()
    } else {
        this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <Subject> Subject.ifC(
    predicate: () -> Boolean,
    block: Subject.() -> Subject,
): Subject {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (predicate()) {
        block()
    } else {
        this
    }
}
