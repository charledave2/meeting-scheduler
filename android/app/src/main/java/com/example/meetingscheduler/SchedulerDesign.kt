package com.example.meetingscheduler

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
