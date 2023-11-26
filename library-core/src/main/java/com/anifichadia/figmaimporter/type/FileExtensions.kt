package com.anifichadia.figmaimporter.type

import java.io.File

fun File.fold(pathElements: List<String>, child: String): File {
    return File(this.fold(pathElements), child)
}

fun File.fold(vararg pathElements: String): File {
    return this.fold(pathElements.toList())
}

fun File.fold(pathElements: List<String>): File {
    return pathElements.fold(this) { acc, pathElement ->
        File(acc, pathElement)
    }
}
