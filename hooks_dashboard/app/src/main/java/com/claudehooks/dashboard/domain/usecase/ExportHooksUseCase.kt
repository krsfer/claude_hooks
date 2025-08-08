package com.claudehooks.dashboard.domain.usecase

import android.content.Context
import com.claudehooks.dashboard.domain.model.FilterCriteria
import com.claudehooks.dashboard.domain.model.HookData
import com.claudehooks.dashboard.domain.repository.HookRepository
import com.google.gson.GsonBuilder
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportHooksUseCase @Inject constructor(
    private val repository: HookRepository
) {
    
    enum class ExportFormat {
        JSON, CSV
    }
    
    suspend fun exportHooks(
        context: Context,
        criteria: FilterCriteria,
        format: ExportFormat,
        fileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val hooks = repository.getFilteredHooks(criteria)
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val defaultFileName = "hooks_export_$timestamp"
            val actualFileName = fileName ?: defaultFileName
            
            val file = when (format) {
                ExportFormat.JSON -> exportToJson(context, hooks, "$actualFileName.json")
                ExportFormat.CSV -> exportToCsv(context, hooks, "$actualFileName.csv")
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export hooks")
            Result.failure(e)
        }
    }
    
    private fun exportToJson(context: Context, hooks: List<HookData>, fileName: String): File {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonContent = gson.toJson(hooks)
        
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(jsonContent)
        
        return file
    }
    
    private fun exportToCsv(context: Context, hooks: List<HookData>, fileName: String): File {
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileWriter(file).use { fileWriter ->
            CSVWriter(fileWriter).use { csvWriter ->
                // Write headers
                val headers = arrayOf(
                    "ID", "Hook Type", "Timestamp", "Session ID", "Sequence",
                    "Status", "Execution Time (ms)", "Platform", "Git Branch", "Git Status", "Project Type",
                    "Prompt", "Tool Name", "Tool Input", "Tool Response", "Message"
                )
                csvWriter.writeNext(headers)
                
                // Write data rows
                hooks.forEach { hook ->
                    val row = arrayOf(
                        hook.id,
                        hook.hook_type.name,
                        hook.timestamp,
                        hook.session_id,
                        hook.sequence.toString(),
                        hook.core.status.name,
                        hook.core.execution_time_ms?.toString() ?: "",
                        hook.context.platform.name,
                        hook.context.git_branch ?: "",
                        hook.context.git_status?.name ?: "",
                        hook.context.project_type?.name ?: "",
                        hook.payload.prompt ?: "",
                        hook.payload.tool_name ?: "",
                        hook.payload.tool_input ?: "",
                        hook.payload.tool_response ?: "",
                        hook.payload.message ?: ""
                    )
                    csvWriter.writeNext(row)
                }
            }
        }
        
        return file
    }
}