package com.anifichadia.figstract.importer.asset.model.importing.dsl

/**
 * Thrown when a pipeline DSL string cannot be parsed or a referenced step name is not found
 */
class ImportPipelineDslException(message: String, cause: Throwable? = null) : Exception(message, cause)
