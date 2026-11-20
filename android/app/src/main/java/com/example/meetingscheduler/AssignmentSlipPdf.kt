package com.example.meetingscheduler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

internal data class AssignmentSlip(
    val name: String,
    val assistant: String,
    val date: String,
    val partNumber: String,
    val hall: String
)

internal fun createAssignmentSlipsPdf(context: Context, slips: List<AssignmentSlip>, weekStart: String): File {
    require(slips.isNotEmpty()) { "Walang nakatalagang participant para sa assignment slips." }
    val document = PdfDocument()
    val regular = runCatching { Typeface.createFromAsset(context.assets, "fonts/GoogleSansFlex.ttf") }
        .getOrDefault(Typeface.create("sans-serif", Typeface.NORMAL))
    val bold = Typeface.create(regular, Typeface.BOLD)
    val ink = Color.rgb(27, 35, 29)
    val forest = Color.rgb(45, 95, 61)
    val pale = Color.rgb(239, 246, 240)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 40f
    val right = 555f
    val note = "The source material and study point for your assignment can be found in the Life and Ministry Meeting Workbook. Please review the instructions for the part as outlined in Instructions for Our Christian Life and Ministry Meeting (S-38)."

    fun drawText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, strong: Boolean = false, color: Int = ink, maxWidth: Float = 515f) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.typeface = if (strong) bold else regular
        paint.textSize = size
        val width = paint.measureText(value)
        if (width > maxWidth && width > 0f) paint.textSize = (size * maxWidth / width).coerceAtLeast(8f)
        canvas.drawText(value, x, y, paint)
    }

    fun drawField(canvas: Canvas, label: String, value: String, y: Float) {
        drawText(canvas, label, left + 8f, y + 15f, 12f, strong = true)
        paint.color = pale
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(left + 92f, y, right - 8f, y + 23f, 3f, 3f, paint)
        if (value.isNotBlank()) drawText(canvas, value, left + 99f, y + 16f, 12f, maxWidth = right - left - 113f)
        paint.color = forest
        paint.strokeWidth = 0.7f
        paint.style = Paint.Style.STROKE
        paint.pathEffect = DashPathEffect(floatArrayOf(2f, 2f), 0f)
        canvas.drawLine(left + 92f, y + 23f, right - 8f, y + 23f, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL
    }

    fun drawCheckbox(canvas: Canvas, label: String, checked: Boolean, y: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.1f
        paint.color = forest
        canvas.drawRect(left + 24f, y - 10f, left + 36f, y + 2f, paint)
        paint.style = Paint.Style.FILL
        if (checked) {
            drawText(canvas, "✓", left + 25f, y + 1f, 12f, strong = true, color = forest)
        }
        drawText(canvas, label, left + 45f, y, 11f)
    }

    fun drawSlip(canvas: Canvas, slip: AssignmentSlip, top: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRect(left, top, right, top + 376f, paint)
        drawText(canvas, "OUR CHRISTIAN LIFE AND MINISTRY", left + 258f, top + 29f, 14f, strong = true, maxWidth = 245f)
        drawText(canvas, "MEETING ASSIGNMENT", left + 318f, top + 47f, 14f, strong = true, maxWidth = 185f)
        drawField(canvas, "Name:", slip.name, top + 60f)
        // Keep the assistant value empty when this assignment has no partner.
        drawField(canvas, "Assistant:", slip.assistant, top + 90f)
        drawField(canvas, "Date:", slip.date, top + 120f)
        drawField(canvas, "Part no.:", slip.partNumber, top + 150f)
        drawText(canvas, "To be given in:", left + 8f, top + 204f, 12f, strong = true)
        drawCheckbox(canvas, "Main Hall", slip.hall == "Main Hall", top + 227f)
        drawCheckbox(canvas, "Second Hall", slip.hall == "Second Hall", top + 246f)
        drawText(canvas, "Note to student:", left + 8f, top + 281f, 10f, strong = true)
        var line = ""
        var lineY = top + 296f
        paint.typeface = regular
        paint.textSize = 9f
        for (word in note.split(" ")) {
            val candidate = if (line.isEmpty()) word else line + " " + word
            if (paint.measureText(candidate) > right - left - 18f) {
                drawText(canvas, line, left + 8f, lineY, 9f)
                lineY += 12f
                line = word
            } else line = candidate
        }
        if (line.isNotEmpty()) drawText(canvas, line, left + 8f, lineY, 9f)
        drawText(canvas, "S-89-E  ·  JW Meeting Scheduler", left + 8f, top + 367f, 8f, color = forest)
    }

    try {
        slips.chunked(2).forEachIndexed { index, pageSlips ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
            page.canvas.drawColor(Color.WHITE)
            pageSlips.forEachIndexed { slot, slip -> drawSlip(page.canvas, slip, if (slot == 0) 28f else 432f) }
            if (pageSlips.size == 2) {
                paint.color = Color.LTGRAY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.7f
                paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                page.canvas.drawLine(left, 419f, right, 419f, paint)
                paint.pathEffect = null
                paint.style = Paint.Style.FILL
            }
            document.finishPage(page)
        }
        val output = File(File(context.cacheDir, "exports").apply { mkdirs() }, "assignment-slips-" + weekStart + ".pdf")
        FileOutputStream(output).use { document.writeTo(it) }
        return output
    } finally {
        document.close()
    }
}
