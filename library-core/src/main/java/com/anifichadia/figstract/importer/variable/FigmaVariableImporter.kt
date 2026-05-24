package com.anifichadia.figstract.importer.variable

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.api.FigmaApi
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.figma.model.GetLocalVariablesResponse
import com.anifichadia.figstract.figma.model.Mode
import com.anifichadia.figstract.figma.model.Variable
import com.anifichadia.figstract.importer.getFileWithBranchName
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.anifichadia.figstract.importer.variable.model.VariableImportResult
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableTreeBuilder
import com.anifichadia.figstract.importer.variable.model.variabletree.toDebugString
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter
import com.anifichadia.figstract.importer.variable.reporting.VariableImportReport
import com.anifichadia.figstract.importer.variable.reporting.VariableImportReportRepository
import com.anifichadia.figstract.util.createLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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
    private val figmaApi: FigmaApi,
    private val importReportRepository: VariableImportReportRepository,
    private val defaultContext: CoroutineContext = Dispatchers.Default,
    private val networkContext: CoroutineContext = Dispatchers.IO,
    private val writerContext: CoroutineContext = Dispatchers.IO,
) {
    suspend fun importFromFigma(handlers: List<VariableFileHandler>) {
        val reports = handlers.associate { handler ->
            handler.figmaFile to VariableImportReport(handler.figmaFile)
        }

        val importFlow = handlers
            .map { handler -> createProcessingFlow(handler, reports.getValue(handler.figmaFile)) }
            .merge()

        coroutineScope {
            importFlow.launchIn(this)
        }

        val failedFiles = mutableListOf<String>()
        for (report in reports.values) {
            importReportRepository.save(report)
            logger.info { report.summary() }

            if (report.hasFailures()) {
                failedFiles += report.figmaFile
                logger.error { "Variable import failures for ${report.figmaFile}: ${report.failures().size} failure(s)" }
            }
        }

        if (failedFiles.isNotEmpty()) {
            throw VariableImportFailureException(failedFiles)
        }
    }

    private fun createProcessingFlow(
        handler: VariableFileHandler,
        report: VariableImportReport,
    ): Flow<Unit> {
        return flow {
            val resolvedHandler = variableFileHandlerForBranch(handler)
            emitAll(createResolvedProcessingFlow(resolvedHandler, report))
        }
    }

    private suspend fun variableFileHandlerForBranch(handler: VariableFileHandler): VariableFileHandler {
        val figmaFileBranchName = handler.figmaFileBranchName ?: return handler

        val branchKey = figmaApi.getFileWithBranchName(
            key = handler.figmaFile,
            branchName = figmaFileBranchName,
            logger = logger,
        )

        return handler.withResolvedBranchKey(branchKey)
    }

    private fun createResolvedProcessingFlow(
        handler: VariableFileHandler,
        report: VariableImportReport,
    ): Flow<Unit> {
        val handlersFlow = flowOf(handler)

        val fileFlow = createFigmaFileFlow(handlersFlow, report)

        val exportFlow = createExportFlow(fileFlow, report)

        val importFlow = createImportFlow(exportFlow, report)

        return importFlow
    }

    private fun createFigmaFileFlow(
        handlers: Flow<VariableFileHandler>,
        report: VariableImportReport,
    ): Flow<Pair<VariableFileHandler, ApiResponse<GetLocalVariablesResponse>>> {
        return handlers
            .flowOn(defaultContext)
            .map { handler ->
                logger.debug { "Fetching ${handler.figmaFile}: Start" }
                val getLocalVariablesResponse = figmaApi.getLocalVariables(
                    key = handler.figmaFile,
                    version = handler.figmaFileVersion,
                )
                logger.info { "Fetching ${handler.figmaFile}: Finish ${getLocalVariablesResponse.isSuccess()}" }
                getLocalVariablesResponse.logError { "Fetching ${handler.figmaFile}" }

                if (!getLocalVariablesResponse.isSuccess()) {
                    report.record(
                        VariableImportResult.Failure.GetVariablesFailed(
                            figmaFile = handler.figmaFile,
                            cause = (getLocalVariablesResponse as ApiResponse.Failure).asException(),
                        )
                    )
                }

                handler to getLocalVariablesResponse
            }
            .flowOn(networkContext)
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(defaultContext)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createExportFlow(
        fileFlow: Flow<Pair<VariableFileHandler, ApiResponse<GetLocalVariablesResponse>>>,
        report: VariableImportReport,
    ): Flow<ExportOutput> {
        return fileFlow
            .map { (handler, getLocalVariablesResponse) ->
                logger.debug { "Processing variables ${handler.figmaFile}: Start" }
                val response = getLocalVariablesResponse.successBodyOrThrow()

                val meta = response.meta
                if (meta == null) {
                    logger.error { "Processing variables ${handler.figmaFile}: No metadata in response" }
                    report.record(
                        VariableImportResult.Failure.NoMetadata(
                            figmaFile = handler.figmaFile,
                        )
                    )
                    return@map emptyList()
                }

                val variables = meta.variables
                val variableCollections = meta.variableCollections.values
                logger.info { "Processing variables ${handler.figmaFile}: Finish, ${variableCollections.size} collection(s)" }

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
                        val variableData = VariableData(
                            variableCollection = variableCollection,
                            variablesByMode = variablesByMode,
                            booleansProvided = filter.variableTypeFilter.includeBooleans,
                            numbersProvided = filter.variableTypeFilter.includeNumbers,
                            stringsProvided = filter.variableTypeFilter.includeStrings,
                            colorsProvided = filter.variableTypeFilter.includeColors,
                        )

                        handler to variableData
                    }
            }
            .flatMapConcat {
                flow {
                    it.forEach { (handler, variableData) ->
                        emit(
                            ExportOutput(
                                handler = handler,
                                variableData = variableData,
                            ),
                        )
                    }
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createImportFlow(
        exportFlow: Flow<ExportOutput>,
        report: VariableImportReport,
    ): Flow<Unit> {
        data class WriterInput(
            val handler: VariableFileHandler,
            val collectionName: String,
            val writer: VariableDataWriter,
            val variableData: VariableData,
            val themeVariantMapping: ThemeVariantMapping,
            val root: VariableGroup,
        )

        return exportFlow
            .flatMapConcat { (handler, variableData) ->
                val collectionName = variableData.variableCollection.name
                val themeVariantMapping = handler.themeVariantMappings.getOrElse(collectionName) {
                    ThemeVariantMapping.None
                }
                val organizationStrategy = handler.variableOrganizationStrategy

                val root = VariableTreeBuilder.build(
                    variableData = variableData,
                    themeVariantMapping = themeVariantMapping,
                    organizationStrategy = organizationStrategy,
                )
                logger.debug { "Built variable tree for ${handler.figmaFile} [$collectionName] using $organizationStrategy:\n${root.toDebugString()}" }

                flow {
                    handler.writers.forEach { writer ->
                        emit(
                            WriterInput(
                                handler = handler,
                                collectionName = collectionName,
                                writer = writer,
                                variableData = variableData,
                                themeVariantMapping = themeVariantMapping,
                                root = root,
                            ),
                        )
                    }
                }
            }
            .map { writerInput ->
                val handler = writerInput.handler
                val collectionName = writerInput.collectionName
                val writer = writerInput.writer
                val variableData = writerInput.variableData
                val themeVariantMapping = writerInput.themeVariantMapping
                val root = writerInput.root

                val writerName = writer::class.simpleName ?: writer.toString()
                val organizationStrategy = handler.variableOrganizationStrategy
                logger.debug { "Importing ${handler.figmaFile} [$collectionName] via $writerName: Started" }
                try {
                    writer.write(
                        variableData = variableData,
                        themeVariantMapping = themeVariantMapping,
                        organizationStrategy = organizationStrategy,
                        collectionName = collectionName,
                        root = root,
                    )

                    report.record(
                        VariableImportResult.Success(
                            figmaFile = handler.figmaFile,
                            variableCollectionName = collectionName,
                            writerName = writerName,
                        ),
                    )
                    logger.info { "Importing ${handler.figmaFile} [$collectionName] via $writerName: Finished true" }
                } catch (e: Throwable) {
                    logger.error(e) { "Importing ${handler.figmaFile} [$collectionName] via $writerName: Finished false" }
                    report.record(
                        VariableImportResult.Failure.WriteFailed(
                            figmaFile = handler.figmaFile,
                            variableCollectionName = collectionName,
                            writerName = writerName,
                            cause = e,
                        ),
                    )
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
