package com.example.meetingscheduler

import android.content.Context
import android.graphics.Bitmap
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

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val IMAGE_WIDTH = 2480
private const val IMAGE_HEIGHT = 3508

private fun renderSlipPage(context: Context, canvas: Canvas, slips: List<AssignmentSlip>) {
    val regular = runCatching { Typeface.createFromAsset(context.assets, "fonts/GoogleSansFlex.ttf") }
        .getOrDefault(Typeface.create("sans-serif", Typeface.NORMAL))
    val bold = Typeface.create(regular, Typeface.BOLD)
    val ink = Color.rgb(27, 35, 29)
    val forest = Color.rgb(45, 95, 61)
    val pale = Color.rgb(239, 246, 240)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val note = "The source material and study point for your assignment can be found in the Life and Ministry Meeting Workbook. Please review the instructions for the part as outlined in Instructions for Our Christian Life and Ministry Meeting (S-38)."
    canvas.drawColor(Color.WHITE)

    fun text(value: String, x: Float, y: Float, size: Float, width: Float, strong: Boolean = false, color: Int = ink) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.typeface = if (strong) bold else regular
        paint.textSize = size
        val measured = paint.measureText(value)
        if (measured > width && measured > 0f) paint.textSize = (size * width / measured).coerceAtLeast(6f)
        canvas.drawText(value, x, y, paint)
    }

    fun field(label: String, value: String, x: Float, y: Float, w: Float, wrap: Boolean = false) {
        text(label, x + 7f, y + 14f, 9.4f, 60f, strong = true)
        val height = if (wrap) 32f else 21f
        paint.color = pale
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x + 68f, y, x + w - 8f, y + height, 2f, 2f, paint)
        if (value.isNotBlank()) {
            if (wrap) {
                paint.typeface = regular
                paint.textSize = 8.2f
                val available = w - 86f
                val words = value.split(" ")
                var first = ""
                var next = ""
                for (word in words) {
                    val candidate = if (first.isEmpty()) word else "$first $word"
                    if (next.isEmpty() && paint.measureText(candidate) <= available) first = candidate
                    else next = if (next.isEmpty()) word else "$next $word"
                }
                text(first, x + 73f, y + 12f, 8.2f, available)
                if (next.isNotBlank()) text(next, x + 73f, y + 23f, 8.2f, available)
            } else text(value, x + 73f, y + 14f, 9.4f, w - 86f)
        }
        paint.color = forest
        paint.strokeWidth = 0.6f
        paint.style = Paint.Style.STROKE
        paint.pathEffect = DashPathEffect(floatArrayOf(2f, 2f), 0f)
        canvas.drawLine(x + 68f, y + height, x + w - 8f, y + height, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL
    }

    fun check(label: String, active: Boolean, x: Float, y: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = forest
        paint.strokeWidth = 0.9f
        canvas.drawRect(x, y - 9f, x + 10f, y + 1f, paint)
        paint.style = Paint.Style.FILL
        if (active) text("X", x + 2f, y, 9f, 8f, strong = true, color = forest)
        text(label, x + 17f, y, 9f, 110f)
    }

    fun slip(slip: AssignmentSlip, x: Float, y: Float) {
        val w = 266f
        text("OUR CHRISTIAN LIFE AND MINISTRY", x + 10f, y + 23f, 11f, w - 20f, strong = true)
        text("MEETING ASSIGNMENT", x + 50f, y + 39f, 11f, w - 60f, strong = true)
        field("Name:", slip.name, x, y + 51f, w)
        // The Assistant line intentionally stays empty when no partner is assigned.
        field("Assistant:", slip.assistant, x, y + 80f, w)
        field("Date:", slip.date, x, y + 109f, w)
        field("Part no.:", slip.partNumber, x, y + 138f, w, wrap = true)
        text("To be given in:", x + 7f, y + 188f, 10f, w - 15f, strong = true)
        check("Main Hall", slip.hall == "Main Hall", x + 20f, y + 210f)
        check("Second Hall", slip.hall == "Second Hall", x + 20f, y + 229f)
        text("Note to student:", x + 7f, y + 263f, 8.5f, w - 14f, strong = true)
        var line = ""
        var lineY = y + 277f
        paint.typeface = regular
        paint.textSize = 7.6f
        for (word in note.split(" ")) {
            val candidate = if (line.isEmpty()) word else line + " " + word
            if (paint.measureText(candidate) > w - 15f) {
                text(line, x + 7f, lineY, 7.6f, w - 14f)
                lineY += 10.2f
                line = word
                paint.typeface = regular
                paint.textSize = 7.6f
            } else line = candidate
        }
        if (line.isNotEmpty()) text(line, x + 7f, lineY, 7.6f, w - 14f)
        text("S-89-E  ·  JW Meeting Scheduler", x + 7f, y + 365f, 7f, w - 14f, color = forest)
    }

    val cells = arrayOf(24f to 25f, 305f to 25f, 24f to 430f, 305f to 430f)
    slips.forEachIndexed { index, slip -> slip(slip, cells[index].first, cells[index].second) }
    paint.color = Color.LTGRAY
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.6f
    paint.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    canvas.drawLine(297f, 18f, 297f, 822f, paint)
    canvas.drawLine(18f, 421f, 577f, 421f, paint)
    paint.pathEffect = null
}

internal fun createAssignmentSlipsPdf(context: Context, slips: List<AssignmentSlip>, weekStart: String): File {
    require(slips.isNotEmpty()) { "Walang nakatalagang participant para sa assignment slips." }
    val document = PdfDocument()
    try {
        slips.chunked(4).forEachIndexed { index, pageSlips ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
            renderSlipPage(context, page.canvas, pageSlips)
            document.finishPage(page)
        }
        val output = File(File(context.cacheDir, "exports").apply { mkdirs() }, "assignment-slips-" + weekStart + ".pdf")
        FileOutputStream(output).use { document.writeTo(it) }
        return output
    } finally {
        document.close()
    }
}

internal fun createAssignmentSlipsImages(context: Context, slips: List<AssignmentSlip>, weekStart: String): List<File> {
    require(slips.isNotEmpty()) { "Walang nakatalagang participant para sa assignment slips." }
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    return slips.chunked(4).mapIndexed { index, pageSlips ->
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(IMAGE_WIDTH.toFloat() / PAGE_WIDTH, IMAGE_HEIGHT.toFloat() / PAGE_HEIGHT)
            renderSlipPage(context, canvas, pageSlips)
            val output = File(directory, "assignment-slips-" + weekStart + "-page-" + (index + 1) + ".png")
            FileOutputStream(output).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            output
        } finally {
            bitmap.recycle()
        }
    }
}
