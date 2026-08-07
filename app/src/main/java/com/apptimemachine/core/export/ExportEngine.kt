package com.apptimemachine.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.ExportFormat
import com.apptimemachine.data.entities.ExportHistoryEntity
import com.apptimemachine.data.entities.ExportStatus
import com.apptimemachine.data.entities.TimelineEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Part 3.1 Export Engine. Every format is generated fully offline into
 * app-private storage, then handed back as a File the caller can share via
 * Android's Share Sheet (Part 3.1: "The application should not
 * automatically upload reports anywhere").
 *
 * File naming follows the spec's suggested pattern:
 * AppTimeMachine_<ReportType>_<yyyy-MM-dd_HH-mm>.<ext>
 */
@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

    private fun exportDir(): File =
        File(context.filesDir, "exports").apply { mkdirs() }

    private fun fileName(reportType: String, extension: String): String =
        "AppTimeMachine_${reportType}_${fileNameFormat.format(Date())}.$extension"

    /** Part 3.1 CSV Export: one row per timeline event with the spec's listed columns. */
    fun exportTimelineCsv(events: List<TimelineEventEntity>, reportType: String = "Timeline"): File {
        val file = File(exportDir(), fileName(reportType, "csv"))
        file.bufferedWriter().use { writer ->
            writer.write("Date,Time,Application,Package,Event Type,Old Value,New Value,Difference,Severity\n")
            events.forEach { event ->
                val date = Formatters.dateTime(event.createdTimestamp)
                writer.write(
                    listOf(
                        csvEscape(date),
                        csvEscape(Formatters.time(event.createdTimestamp)),
                        csvEscape(event.appName),
                        csvEscape(event.packageName),
                        csvEscape(event.eventType),
                        csvEscape(event.oldValue ?: ""),
                        csvEscape(event.newValue ?: ""),
                        csvEscape(event.difference ?: ""),
                        csvEscape(event.severity.name)
                    ).joinToString(",") + "\n"
                )
            }
        }
        return file
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    /** Part 3.1 JSON Export: human-readable, includes metadata + monitoring period. */
    fun exportTimelineJson(
        events: List<TimelineEventEntity>,
        periodStart: Long,
        periodEnd: Long,
        reportType: String = "Timeline"
    ): File {
        val file = File(exportDir(), fileName(reportType, "json"))
        val root = buildJsonObject {
            put("metadata", buildJsonObject {
                put("generatedAt", Formatters.dateTime(System.currentTimeMillis()))
                put("reportType", reportType)
                put("appVersion", "1.0")
            })
            put("monitoringPeriod", buildJsonObject {
                put("start", Formatters.dateTime(periodStart))
                put("end", Formatters.dateTime(periodEnd))
            })
            put("timeline", buildJsonArray {
                events.forEach { event ->
                    add(buildJsonObject {
                        put("date", Formatters.dateTime(event.createdTimestamp))
                        put("application", event.appName)
                        put("package", event.packageName)
                        put("eventType", event.eventType)
                        put("oldValue", event.oldValue)
                        put("newValue", event.newValue)
                        put("difference", event.difference)
                        put("severity", event.severity.name)
                    })
                }
            })
        }
        file.writeText(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), root))
        return file
    }

    /**
     * Part 3.1 PDF Report: cover page + timeline table, built with Android's
     * built-in PdfDocument (no third-party PDF library needed — keeps the
     * app offline-first and dependency-light per Part 4.0).
     */
    fun exportTimelinePdf(
        events: List<TimelineEventEntity>,
        periodStart: Long,
        periodEnd: Long,
        reportType: String = "Timeline"
    ): File {
        val file = File(exportDir(), fileName(reportType, "pdf"))
        val document = PdfDocument()
        val pageWidth = 595 // A4 @ 72dpi
        val pageHeight = 842

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { textSize = 11f; color = 0xFF666666.toInt() }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 9f }
        val footerPaint = Paint().apply { textSize = 8f; color = 0xFF999999.toInt() }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 60f

        // Cover / header
        canvas.drawText("App Time Machine", 40f, y, titlePaint)
        y += 26f
        canvas.drawText("$reportType Report", 40f, y, subtitlePaint)
        y += 16f
        canvas.drawText(
            "Monitoring Period: ${Formatters.dateTime(periodStart)} – ${Formatters.dateTime(periodEnd)}",
            40f, y, subtitlePaint
        )
        y += 30f

        canvas.drawText("Date", 40f, y, headerPaint)
        canvas.drawText("App", 140f, y, headerPaint)
        canvas.drawText("Event", 320f, y, headerPaint)
        canvas.drawText("Change", 460f, y, headerPaint)
        y += 14f
        canvas.drawLine(40f, y, 555f, y, subtitlePaint)
        y += 12f

        fun finishPage() {
            canvas.drawText("Generated by App Time Machine — Page $pageNumber", 40f, pageHeight - 24f, footerPaint)
            document.finishPage(page)
        }

        for (event in events) {
            if (y > pageHeight - 60f) {
                finishPage()
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 50f
            }
            canvas.drawText(Formatters.dateTime(event.createdTimestamp), 40f, y, bodyPaint)
            canvas.drawText(event.appName.take(22), 140f, y, bodyPaint)
            canvas.drawText(event.eventType.replace('_', ' ').take(18), 320f, y, bodyPaint)
            canvas.drawText((event.difference ?: event.newValue ?: "—").take(14), 460f, y, bodyPaint)
            y += 14f
        }
        finishPage()

        document.writeTo(file.outputStream())
        document.close()
        return file
    }

    fun buildExportHistoryEntry(file: File, reportType: String, format: ExportFormat, recordCount: Int) =
        ExportHistoryEntity(
            reportType = reportType,
            format = format,
            filePath = file.absolutePath,
            fileSizeBytes = file.length(),
            recordCount = recordCount,
            status = ExportStatus.SUCCESS,
            createdAt = System.currentTimeMillis()
        )
}
