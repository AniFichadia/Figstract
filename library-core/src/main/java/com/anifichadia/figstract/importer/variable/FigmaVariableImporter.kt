package com.anifichadia.figstract.importer.variable

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.api.FigmaApi
import com.anifichadia.figstract.figma.api.FigmaApiProxyWithFlowControl
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.figma.model.GetLocalVariablesResponse
import com.anifichadia.figstract.figma.model.Mode
import com.anifichadia.figstract.figma.model.Variable
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.anifichadia.figstract.util.createLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlin.coroutines.CoroutineContext

class FigmaVariableImporter(
    figmaApi: FigmaApi,
    figmaApiConcurrencyLimit: Int = FigmaApiProxyWithFlowControl.DEFAULT_CONCURRENCY_LIMIT,
    private val defaultContext: CoroutineContext = Dispatchers.Default,
    private val networkContext: CoroutineContext = Dispatchers.IO,
    private val writerContext: CoroutineContext = Dispatchers.IO,
) {
    private val figmaApi = FigmaApiProxyWithFlowControl(figmaApi, figmaApiConcurrencyLimit)

    suspend fun importFromFigma(handlers: List<VariableFileHandler>) {
        val importFlow = handlers
            .map { handler -> createProcessingFlow(handler) }
            .merge()

        coroutineScope {
            importFlow.launchIn(this)
        }
    }

    private fun createProcessingFlow(handler: VariableFileHandler): Flow<Unit> {
        val handlersFlow = flowOf(handler)

        val fileFlow = createFigmaFileFlow(handlersFlow)

        val exportFlow = createExportFlow(fileFlow)

        val importFlow = createImportFlow(exportFlow)

        return importFlow
    }

    private fun createFigmaFileFlow(handlers: Flow<VariableFileHandler>): Flow<Pair<VariableFileHandler, ApiResponse<GetLocalVariablesResponse>>> {
        return handlers
            .flowOn(defaultContext)
            .map { handler ->
                logger.debug { "Fetching ${handler.figmaFile}: Start" }
                val getLocalVariablesResponse = figmaApi.getLocalVariables(
                    key = handler.figmaFile,
                )
                logger.info { "Fetching ${handler.figmaFile}: Finish ${getLocalVariablesResponse.isSuccess()}" }
                getLocalVariablesResponse.logError { "Fetching ${handler.figmaFile}" }

                handler to getLocalVariablesResponse
            }
            .flowOn(networkContext)
            // TODO: error handling
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(defaultContext)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createExportFlow(fileFlow: Flow<Pair<VariableFileHandler, ApiResponse<GetLocalVariablesResponse>>>): Flow<ExportOutput> {
        // TODO: log processing
        return fileFlow
            .map { (handler, getLocalVariablesResponse) ->
                val response = getLocalVariablesResponse.successBodyOrThrow()

                // TODO: error
                val meta = response.meta ?: error("no data????")
                val variables = meta.variables
                val variableCollections = meta.variableCollections.values

                val filter = handler.filter
                variableCollections
                    .filter { filter.variableCollectionFilter.accept(it) }
                    .map { variableCollection ->
                        val variablesForCollection = variables
                            .filter { it.key in variableCollection.variableIds }

                        val modes = variableCollection
                            .modes
                            .filter { filter.modeNameFilter.accept(it) }

                        val variablesByMode = modes.map { mode ->
                            VariableData.VariablesByMode(
                                mode = mode,
                                booleanVariables = variablesForCollection.extractVariables<Variable.BooleanVariable, Boolean>(
                                    mode = mode,
                                    typeEnabled = filter.variableTypeFilter.includeBooleans,
                                ),
                                numberVariables = variablesForCollection.extractVariables<Variable.NumberVariable, Number>(
                                    mode = mode,
                                    typeEnabled = filter.variableTypeFilter.includeNumbers,
                                ),
                                stringVariables = variablesForCollection.extractVariables<Variable.StringVariable, String>(
                                    mode = mode,
                                    typeEnabled = filter.variableTypeFilter.includeStrings,
                                ),
                                colorVariables = variablesForCollection.extractVariables<Variable.ColorVariable, Color>(
                                    mode = mode,
                                    typeEnabled = filter.variableTypeFilter.includeColors,
                                ),
                            )
                        }
                        handler to VariableData(
                            variableCollection = variableCollection,
                            variablesByMode = variablesByMode,
                        )
                    }
            }
            .flatMapConcat {
                flow {
                    it.forEach {
                        emit(
                            ExportOutput(
                                handler = it.first,
                                variableData = it.second,
                            ),
                        )
                    }
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createImportFlow(exportFlow: Flow<ExportOutput>): Flow<Unit> {
        return exportFlow
            .flatMapConcat { exportOutput ->
                val handler = exportOutput.handler
                val variableData = exportOutput.variableData
                flow {
                    handler.writers.forEach { writer ->
                        emit(Triple(handler, variableData, writer))
                    }
                }
            }
            .map { (handler, variableData, writer) ->
                logger.debug { "Importing ${handler.figmaFile}: Started" }
                try {
                    writer.write(variableData)
                    logger.info { "Importing ${handler.figmaFile}: Finished true" }
                } catch (e: Throwable) {
                    logger.error(e) { "Importing ${handler.figmaFile}: Finished false" }
                    // TODO: error handling
                }
            }
            .flowOn(writerContext)
    }

    private fun Variable.getValue(modeId: String) = when (this) {
        is Variable.BooleanVariable -> this.valuesByMode[modeId]
        is Variable.NumberVariable -> this.valuesByMode[modeId]
        is Variable.StringVariable -> this.valuesByMode[modeId]
        is Variable.ColorVariable -> this.valuesByMode[modeId]
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified V : Variable, T : Any> Map<String, Variable>.extractVariables(
        mode: Mode,
        typeEnabled: Boolean,
    ): Map<String, T>? {
        if (!typeEnabled) return null

        val variablesForCollection = this
        val modeId = mode.modeId
        return variablesForCollection
            .values
            .filterIsInstance<V>()
            .mapNotNull { variable ->
                variable
                    .getValue(modeId)
                    ?.let { variableForMode ->
                        val value = variableForMode.value as T?
                        val variableAlias = variableForMode.variableAlias
                        when {
                            value != null -> value
                            variableAlias != null -> variablesForCollection[variableAlias.id]
                                ?.let { it as? V }
                                ?.let { it.getValue(modeId)?.value as T? }

                            else -> error("unresolved")
                        }
                    }
                    ?.let { variable.name to it }
            }
            .toMap()
    }

    private data class ExportOutput(
        val handler: VariableFileHandler,
        val variableData: VariableData,
    )

    private companion object {
        private val logger = createLogger("FigmaVariableImporter")

        private fun <R> ApiResponse<R>.logError(message: () -> String) {
            if (this is ApiResponse.Failure) {
                logger.error(this.asException(), message)
            }
        }
    }
}
