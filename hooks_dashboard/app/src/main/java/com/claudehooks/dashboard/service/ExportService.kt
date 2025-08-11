package com.claudehooks.dashboard.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.PerformanceMetrics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service for exporting dashboard data in various formats
 */
class ExportService(private val context: Context) {
    
    companion object {
        const val EXPORT_DIR = "ClaudeHooks"
        const val CSV_MIME_TYPE = "text/csv"
        const val JSON_MIME_TYPE = "application/json"
        const val TEXT_MIME_TYPE = "text/plain"
    }
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toString())
        })
        .create()
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        .withZone(ZoneId.systemDefault())
    
    /**
     * Export formats supported
     */
    enum class ExportFormat {
        CSV,
        JSON,
        TEXT
    }
    
    /**
     * Export events to specified format
     */
    suspend fun exportEvents(
        events: List<HookEvent>,
        format: ExportFormat,
        filename: String? = null
    ): Result<Uri> {
        return try {
            val timestamp = dateFormatter.format(Instant.now())
            val baseFilename = filename ?: "claude_hooks_${timestamp}"
            
            val (content, extension, mimeType) = when (format) {
                ExportFormat.CSV -> Triple(
                    generateCsvContent(events),
                    ".csv",
                    CSV_MIME_TYPE
                )
                ExportFormat.JSON -> Triple(
                    generateJsonContent(events),
                    ".json",
                    JSON_MIME_TYPE
                )
                ExportFormat.TEXT -> Triple(
                    generateTextContent(events),
                    ".txt",
                    TEXT_MIME_TYPE
                )
            }
            
            val uri = saveToFile("$baseFilename$extension", content, mimeType)
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export events")
            Result.failure(e)
        }
    }
    
    /**
     * Export dashboard statistics report
     */
    suspend fun exportStatisticsReport(
        stats: DashboardStats,
        events: List<HookEvent>,
        performanceMetrics: PerformanceMetrics? = null
    ): Result<Uri> {
        return try {
            val timestamp = dateFormatter.format(Instant.now())
            val filename = "dashboard_report_${timestamp}.txt"
            val content = generateStatisticsReport(stats, events, performanceMetrics)
            
            val uri = saveToFile(filename, content, TEXT_MIME_TYPE)
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export statistics report")
            Result.failure(e)
        }
    }
    
    /**
     * Generate CSV content from events
     */
    private fun generateCsvContent(events: List<HookEvent>): String {
        val csvBuilder = StringBuilder()
        
        // Header
        csvBuilder.appendLine("Timestamp,Type,Severity,Title,Message,Source,Session ID,Metadata")
        
        // Data rows
        events.forEach { event ->
            val row = listOf(
                event.timestamp.toString(),
                event.type.name,
                event.severity.name,
                escapeCsvField(event.title),
                escapeCsvField(event.message),
                escapeCsvField(event.source),
                event.metadata["session_id"] ?: "",
                escapeCsvField(event.metadata.toString())
            ).joinToString(",")
            
            csvBuilder.appendLine(row)
        }
        
        return csvBuilder.toString()
    }
    
    /**
     * Generate JSON content from events
     */
    private fun generateJsonContent(events: List<HookEvent>): String {
        val exportData = mapOf(
            "exportTime" to Instant.now().toString(),
            "totalEvents" to events.size,
            "events" to events.map { event ->
                mapOf(
                    "id" to event.id,
                    "timestamp" to event.timestamp.toString(),
                    "type" to event.type.name,
                    "severity" to event.severity.name,
                    "title" to event.title,
                    "message" to event.message,
                    "source" to event.source,
                    "metadata" to event.metadata
                )
            }
        )
        
        return gson.toJson(exportData)
    }
    
    /**
     * Generate human-readable text content from events
     */
    private fun generateTextContent(events: List<HookEvent>): String {
        val textBuilder = StringBuilder()
        
        textBuilder.appendLine("=== Claude Hooks Dashboard Export ===")
        textBuilder.appendLine("Export Time: ${Instant.now()}")
        textBuilder.appendLine("Total Events: ${events.size}")
        textBuilder.appendLine()
        textBuilder.appendLine("=" * 50)
        textBuilder.appendLine()
        
        events.forEach { event ->
            textBuilder.appendLine("[${event.timestamp}] ${event.severity.name}")
            textBuilder.appendLine("Type: ${event.type.name}")
            textBuilder.appendLine("Title: ${event.title}")
            textBuilder.appendLine("Message: ${event.message}")
            textBuilder.appendLine("Source: ${event.source}")
            
            if (event.metadata.isNotEmpty()) {
                textBuilder.appendLine("Metadata:")
                event.metadata.forEach { (key, value) ->
                    textBuilder.appendLine("  $key: $value")
                }
            }
            
            textBuilder.appendLine("-" * 30)
            textBuilder.appendLine()
        }
        
        return textBuilder.toString()
    }
    
    /**
     * Generate comprehensive statistics report
     */
    private fun generateStatisticsReport(
        stats: DashboardStats,
        events: List<HookEvent>,
        performanceMetrics: PerformanceMetrics?
    ): String {
        val reportBuilder = StringBuilder()
        
        reportBuilder.appendLine("═══ CLAUDE HOOKS DASHBOARD REPORT ═══")
        reportBuilder.appendLine()
        reportBuilder.appendLine("Generated: ${Instant.now()}")
        reportBuilder.appendLine()
        
        // Dashboard Statistics
        reportBuilder.appendLine("📊 DASHBOARD STATISTICS")
        reportBuilder.appendLine("━" * 30)
        reportBuilder.appendLine("Total Events: ${stats.totalEvents}")
        reportBuilder.appendLine("Critical Events: ${stats.criticalCount}")
        reportBuilder.appendLine("Warnings: ${stats.warningCount}")
        reportBuilder.appendLine("Success Rate: ${String.format("%.1f%%", stats.successRate)}")
        reportBuilder.appendLine("Active Hooks: ${stats.activeHooks}")
        reportBuilder.appendLine()
        
        // Performance Metrics
        if (performanceMetrics != null) {
            reportBuilder.appendLine("⚡ PERFORMANCE METRICS")
            reportBuilder.appendLine("━" * 30)
            reportBuilder.appendLine("Health Score: ${performanceMetrics.healthScore.toInt()}%")
            reportBuilder.appendLine("Memory Usage: ${performanceMetrics.memoryUsageMB}MB / ${performanceMetrics.maxMemoryMB}MB (${String.format("%.1f%%", performanceMetrics.memoryUsagePercent)})")
            reportBuilder.appendLine("Events/sec: ${String.format("%.1f", performanceMetrics.eventsPerSecond)}")
            reportBuilder.appendLine("Connection Latency: ${performanceMetrics.connectionLatencyMs}ms")
            reportBuilder.appendLine("Cache Hit Rate: ${String.format("%.1f%%", performanceMetrics.cacheHitRate * 100)}")
            reportBuilder.appendLine()
        }
        
        // Event Type Distribution
        reportBuilder.appendLine("📈 EVENT TYPE DISTRIBUTION")
        reportBuilder.appendLine("━" * 30)
        val typeDistribution = events.groupBy { it.type }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        typeDistribution.forEach { (type, count) ->
            val percentage = (count.toFloat() / events.size * 100)
            reportBuilder.appendLine("${type.name}: $count (${String.format("%.1f%%", percentage)})")
        }
        reportBuilder.appendLine()
        
        // Severity Distribution
        reportBuilder.appendLine("🚨 SEVERITY DISTRIBUTION")
        reportBuilder.appendLine("━" * 30)
        val severityDistribution = events.groupBy { it.severity }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        severityDistribution.forEach { (severity, count) ->
            val percentage = (count.toFloat() / events.size * 100)
            reportBuilder.appendLine("${severity.name}: $count (${String.format("%.1f%%", percentage)})")
        }
        reportBuilder.appendLine()
        
        // Top Sources
        reportBuilder.appendLine("📍 TOP EVENT SOURCES")
        reportBuilder.appendLine("━" * 30)
        val topSources = events.groupBy { it.source }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
        
        topSources.forEach { (source, count) ->
            reportBuilder.appendLine("$source: $count events")
        }
        reportBuilder.appendLine()
        
        // Time-based Analysis
        reportBuilder.appendLine("⏰ TIME-BASED ANALYSIS")
        reportBuilder.appendLine("━" * 30)
        if (events.isNotEmpty()) {
            val oldestEvent = events.minByOrNull { it.timestamp }
            val newestEvent = events.maxByOrNull { it.timestamp }
            
            reportBuilder.appendLine("Oldest Event: ${oldestEvent?.timestamp}")
            reportBuilder.appendLine("Newest Event: ${newestEvent?.timestamp}")
            
            // Events per hour (last 24 hours)
            val now = Instant.now()
            val last24Hours = events.filter { 
                it.timestamp.isAfter(now.minusSeconds(86400))
            }
            
            if (last24Hours.isNotEmpty()) {
                val eventsPerHour = last24Hours.size / 24.0
                reportBuilder.appendLine("Events/hour (last 24h): ${String.format("%.1f", eventsPerHour)}")
            }
        }
        reportBuilder.appendLine()
        
        reportBuilder.appendLine("═" * 40)
        reportBuilder.appendLine("End of Report")
        
        return reportBuilder.toString()
    }
    
    /**
     * Save content to file and return URI
     */
    private fun saveToFile(filename: String, content: String, mimeType: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android Q and above
            saveToMediaStore(filename, content, mimeType)
        } else {
            // Use external storage for older versions
            saveToExternalStorage(filename, content)
        }
    }
    
    /**
     * Save to MediaStore (Android Q+)
     */
    private fun saveToMediaStore(filename: String, content: String, mimeType: String): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$EXPORT_DIR")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw Exception("Failed to create file in MediaStore")
        
        resolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(content)
            }
        }
        
        return uri
    }
    
    /**
     * Save to external storage (pre-Android Q)
     */
    private fun saveToExternalStorage(filename: String, content: String): Uri {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val exportDir = File(documentsDir, EXPORT_DIR)
        
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val file = File(exportDir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(content)
            }
        }
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Escape CSV field to handle special characters
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
    
    /**
     * Create share intent for exported file
     */
    fun createShareIntent(uri: Uri, mimeType: String, title: String = "Share Export"): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Claude Hooks Dashboard Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, title) }
    }
}

// Extension function for string multiplication
private operator fun String.times(count: Int): String = repeat(count)