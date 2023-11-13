package com.anifichadia.figmaimporter.apiclient

import com.anifichadia.figmaimporter.type.mapIf

typealias PathSegment = String

fun pathSegments(vararg segments: PathSegment?) = segments.toList()

fun List<PathSegment?>.sanitise(): List<PathSegment> =
    this.filterNotNull()
        .filter { it.isNotBlank() }
        .mapIf({ it.startsWith("/") }) { it.substring(1) }
        .mapIf({ it.endsWith("/") }) { it.substring(0, it.length - 1) }
        .toList()

fun List<PathSegment?>.join(): String =
    this.sanitise()
        .joinToString("/")
