package com.example.meetingscheduler

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MeetingSchedulerBrandIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.onSurface.luminance() > 0.5f
    val tile = if (dark) colors.primaryContainer else colors.primary
    val paper = Color(0xFFF6F0D8)
    val cell = colors.tertiary
    val checkTile = colors.primary
    val checkInk = colors.onPrimary

    Canvas(modifier.semantics { contentDescription?.let { this.contentDescription = it } }) {
        val scale = size.minDimension / 512f
        val left = (size.width - 512f * scale) / 2f
        val top = (size.height - 512f * scale) / 2f
        fun point(x: Float, y: Float) = Offset(left + x * scale, top + y * scale)
        fun rect(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float) {
            drawRoundRect(
                color = color,
                topLeft = point(x, y),
                size = Size(width * scale, height * scale),
                cornerRadius = CornerRadius(radius * scale)
            )
        }

        rect(0f, 0f, 512f, 512f, tile, 76f)
        rect(128f, 92f, 28f, 53f, paper, 14f)
        rect(356f, 92f, 28f, 53f, paper, 14f)
        rect(132f, 85f, 18f, 60f, paper, 9f)
        rect(362f, 85f, 18f, 60f, paper, 9f)
        rect(60f, 142f, 392f, 248f, paper, 40f)

        val cells = listOf(92f to 228f, 198f to 228f, 304f to 228f, 92f to 310f, 198f to 310f, 304f to 310f)
        cells.forEachIndexed { index, (x, y) ->
            rect(x, y, 76f, 64f, if (index == 2) checkTile else cell, 12f)
        }
        val tick = Path().apply {
            moveTo(point(323f, 250f).x, point(323f, 250f).y)
            lineTo(point(331f, 242f).x, point(331f, 242f).y)
            lineTo(point(344f, 255f).x, point(344f, 255f).y)
            lineTo(point(369f, 228f).x, point(369f, 228f).y)
            lineTo(point(378f, 237f).x, point(378f, 237f).y)
            lineTo(point(344f, 279f).x, point(344f, 279f).y)
            close()
        }
        drawPath(tick, checkInk)
    }
}

private val SchedulerFont = FontFamily(
    Font(R.font.scheduler_regular, FontWeight.Normal),
    Font(R.font.scheduler_medium, FontWeight.Medium),
    Font(R.font.scheduler_semibold, FontWeight.SemiBold),
    Font(R.font.scheduler_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()
internal val SchedulerTypography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = SchedulerFont),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = SchedulerFont),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = SchedulerFont),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = SchedulerFont),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = SchedulerFont),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = SchedulerFont),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = SchedulerFont),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = SchedulerFont),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = SchedulerFont),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = SchedulerFont),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = SchedulerFont),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = SchedulerFont),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = SchedulerFont),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = SchedulerFont, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = SchedulerFont, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
internal fun PageFrame(modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val gutter = if (maxWidth >= 600.dp) 24.dp else 16.dp
        content(Modifier.widthIn(max = 880.dp).fillMaxWidth().padding(horizontal = gutter, vertical = 12.dp))
    }
}

@Composable
internal fun AdaptivePair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minimumWidth = 340.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        if (maxWidth < minimumWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}


// WebView previews and PNG exports use the same font files as Compose.
@Volatile private var cachedScheduleFontCss: String? = null

internal fun scheduleFontCss(context: Context): String = cachedScheduleFontCss ?: buildString {
    for ((weight, resource) in listOf(
        400 to R.font.scheduler_regular,
        600 to R.font.scheduler_semibold,
        700 to R.font.scheduler_bold
    )) {
        val data = context.resources.openRawResource(resource).use {
            Base64.encodeToString(it.readBytes(), Base64.NO_WRAP)
        }
        append("@font-face{font-family:'Google Sans';src:url(data:font/ttf;base64,")
        append(data).append(") format('truetype');font-weight:").append(weight)
        append(";font-style:normal;font-display:block}")
    }
}.also { cachedScheduleFontCss = it }
