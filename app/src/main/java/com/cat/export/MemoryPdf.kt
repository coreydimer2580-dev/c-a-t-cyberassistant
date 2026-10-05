package com.cat.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.cat.data.MemoryEntity
import com.cat.data.TruthTag
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v1.16: writes memory notes to a local PDF and opens the Android share sheet.
 * Nothing is uploaded by C@T. The user picks where it goes (Drive, email, Files…).
 */
object MemoryPdf {
    private val AU: Locale = Locale.forLanguageTag("en-AU")
    private val PERTH: ZoneId = ZoneId.of("Australia/Perth")
    private val STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AU)

    /** Pure text layout, testable on the JVM. */
    fun lines(notes: List<MemoryEntity>, nowMillis: Long): List<String> {
        val out = mutableListOf<String>()
        out += "C@T memory export"
        out += "Exported ${format(nowMillis)} (Perth) · ${notes.size} notes · on-device only"
        out += ""
        if (notes.isEmpty()) out += "No notes saved yet."
        notes.forEach { note ->
            out += "[${TruthTag.normalize(note.truthTag)}] ${note.category} · ${format(note.createdAt)}"
            out += note.content.replace(Regex("\\s+"), " ").trim()
            out += ""
        }
        return out
    }

    fun format(millis: Long): String = Instant.ofEpochMilli(millis).atZone(PERTH).format(STAMP)

    fun write(context: Context, notes: List<MemoryEntity>, nowMillis: Long = System.currentTimeMillis()): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "C-AT-memory.pdf")
        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 40f
        val body = Paint().apply { textSize = 11f; isAntiAlias = true; color = android.graphics.Color.BLACK }
        val head = Paint(body).apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD }
        val lineH = 15f
        val maxW = pageW - margin * 2
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        var y = margin + 10f
        lines(notes, nowMillis).forEachIndexed { i, raw ->
            val paint = if (i == 0) head else body
            wrap(raw, paint, maxW).forEach { line ->
                if (y > pageH - margin) {
                    doc.finishPage(page)
                    pageNo++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
                    y = margin + 10f
                }
                page.canvas.drawText(line, margin, y, paint)
                y += if (i == 0) 22f else lineH
            }
        }
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun wrap(text: String, paint: Paint, maxW: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var current = ""
        text.split(' ').forEach { word ->
            val trial = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(trial) <= maxW) {
                current = trial
            } else {
                if (current.isNotEmpty()) out += current
                var w = word
                while (paint.measureText(w) > maxW && w.length > 1) {
                    val n = paint.breakText(w, true, maxW, null).coerceAtLeast(1)
                    out += w.take(n)
                    w = w.drop(n)
                }
                current = w
            }
        }
        if (current.isNotEmpty()) out += current
        return out
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".exports", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "C@T memory export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share C@T memory PDF").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Text-only backup reminder. Never uploads anything. */
    fun backupReminder(lastExport: Long, nowMillis: Long): String {
        if (lastExport <= 0L) {
            return "Backup reminder: you haven't exported memory yet. Tap Export PDF, then pick Drive in the share sheet if you want a copy there. C@T never uploads on its own."
        }
        val days = ((nowMillis - lastExport) / 86_400_000L).coerceAtLeast(0)
        return if (days >= 7) {
            "Backup reminder: last export was $days days ago. Export PDF and save it to Drive yourself if you want a fresh copy. No auto upload."
        } else {
            "Last export ${format(lastExport)} (Perth). C@T never uploads on its own."
        }
    }
}
