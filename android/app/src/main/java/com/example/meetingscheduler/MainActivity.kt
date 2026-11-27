package com.example.meetingscheduler

import android.Manifest

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.BroadcastReceiver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Bundle
import android.os.Build
import android.view.View
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SHARE_RESULT_PREFS = "schedule_share_result"
private const val SHARE_CHOSEN_ACTION = "com.example.meetingscheduler.SHARE_TARGET_CHOSEN"
private const val LEGACY_WORKBOOK_WEEK_START = "2026-11-09"

class ShareTargetChosenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("share_type") ?: return
        context.getSharedPreferences(SHARE_RESULT_PREFS, Context.MODE_PRIVATE)
            .edit().putString("selected_type", type).apply()
    }
}


private fun fitScheduleWebView(
    view: WebView,
    targetWidthPx: Int,
    targetHeightPx: Int,
    density: Float,
    onReady: () -> Unit,
    onFailure: () -> Unit
) {
    var widthExpanded = false
    var layoutWidthCss = SCHEDULE_EXPORT_CSS_WIDTH.toFloat()
    fun measureContent(attempt: Int) {
        val measureScript = "(document.fonts && document.fonts.status === 'loading') ? -1 : document.body.scrollHeight"
        view.evaluateJavascript(measureScript) { rawHeight ->
            val contentHeight = rawHeight?.trim()?.toFloatOrNull() ?: -1f
            if (contentHeight <= 0f) {
                if (attempt < 30) view.postDelayed({ measureContent(attempt + 1) }, 100L) else onFailure()
                return@evaluateJavascript
            }
            if (!widthExpanded) {
                widthExpanded = true
                // A tall schedule used to shrink the fixed-width page, leaving a blank
                // strip on the right. Give its columns that space before fitting to A4.
                val pageWidthCss = (targetWidthPx * contentHeight / targetHeightPx)
                    .coerceIn(SCHEDULE_EXPORT_CSS_WIDTH.toFloat(), SCHEDULE_EXPORT_CSS_WIDTH * 1.8f)
                if (pageWidthCss > SCHEDULE_EXPORT_CSS_WIDTH + 1f) {
                    layoutWidthCss = pageWidthCss
                    view.evaluateJavascript("document.body.style.width='${pageWidthCss}px'; true") {
                        measureContent(attempt + 1)
                    }
                    return@evaluateJavascript
                }
            }
            val widthScale = targetWidthPx / (layoutWidthCss * density)
            val heightScale = targetHeightPx / (contentHeight * density)
            val fitScale = minOf(widthScale, heightScale).coerceAtLeast(0.01f)
            view.evaluateJavascript("document.body.style.zoom='$fitScale'; true") {
                view.postVisualStateCallback(System.nanoTime(), object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) { view.postDelayed(onReady, 100L) }
                })
            }
        }
    }
    measureContent(0)
}

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFA8C3A4), onPrimary = Color(0xFF17301F),
    primaryContainer = Color(0xFF304536), onPrimaryContainer = Color(0xFFDDE9D9),
    secondary = Color(0xFFB6C7B4), onSecondary = Color(0xFF273329),
    secondaryContainer = Color(0xFF304536), onSecondaryContainer = Color(0xFFDDE9D9),
    tertiary = Color(0xFFC3C7A0), onTertiary = Color(0xFF2D3019),
    tertiaryContainer = Color(0xFF3C4227), onTertiaryContainer = Color(0xFFE3E7C7),
    background = Color(0xFF151812), surface = Color(0xFF151812),
    surfaceVariant = Color(0xFF20241E), onSurface = Color(0xFFE5E9E0),
    onSurfaceVariant = Color(0xFFB7BEB3), outline = Color(0xFF687064),
    outlineVariant = Color(0xFF3A4139), error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    surfaceTint = Color(0xFFA8C3A4),
    surfaceDim = Color(0xFF10150F), surfaceBright = Color(0xFF353B33),
    surfaceContainerLowest = Color(0xFF10150F), surfaceContainerLow = Color(0xFF191F18),
    surfaceContainer = Color(0xFF20271F), surfaceContainerHigh = Color(0xFF293127),
    surfaceContainerHighest = Color(0xFF343C32),
    inverseSurface = Color(0xFFE5E9E0), inverseOnSurface = Color(0xFF20241E), inversePrimary = Color(0xFF28634D)
)
private val LightPalette = lightColorScheme(
    primary = Color(0xFF28634D), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9DD), onPrimaryContainer = Color(0xFF1B3828),
    secondary = Color(0xFF566858), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9DD), onSecondaryContainer = Color(0xFF1B3828),
    tertiary = Color(0xFF5C6542), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E9D1), onTertiaryContainer = Color(0xFF252B12),
    background = Color(0xFFF7F8F2), surface = Color(0xFFF7F8F2),
    surfaceVariant = Color(0xFFEBEFE8), onSurface = Color(0xFF191D1A),
    onSurfaceVariant = Color(0xFF535D53), outline = Color(0xFF7A837B),
    outlineVariant = Color(0xFFC1C9C0),
    surfaceTint = Color(0xFF28634D),
    surfaceDim = Color(0xFFD8DED5), surfaceBright = Color(0xFFF7F8F2),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F4EC),
    surfaceContainer = Color(0xFFEBEFE8), surfaceContainerHigh = Color(0xFFE5EAE1),
    surfaceContainerHighest = Color(0xFFDDE4D9),
    inverseSurface = Color(0xFF293127), inverseOnSurface = Color(0xFFF1F4EC), inversePrimary = Color(0xFFA8C3A4)
)

private enum class AppPaletteStyle(
    val label: String,
    val primaryLight: Color, val primaryDark: Color,
    val containerLight: Color, val containerDark: Color,
    val secondaryLight: Color, val secondaryDark: Color,
    val tertiaryLight: Color, val tertiaryDark: Color,
    val swatches: List<Color>
) {
    TONAL_SPOT("Tonal Spot", Color(0xFF28634D), Color(0xFFA8C3A4), Color(0xFFDCE9DD), Color(0xFF304536), Color(0xFF566858), Color(0xFFB6C7B4), Color(0xFF5C6542), Color(0xFFC3C7A0), listOf(Color(0xFF80CBC4), Color(0xFFB7D0C8), Color(0xFF9EC4E1), Color(0xFF38586B), Color(0xFF00574B))),
    NEUTRAL("Neutral", Color(0xFF52615D), Color(0xFFBBCAC4), Color(0xFFDCE5E0), Color(0xFF394541), Color(0xFF56635F), Color(0xFFC0CCC7), Color(0xFF58666B), Color(0xFFC1CED0), listOf(Color(0xFFDCE5E0), Color(0xFFC0CCC7), Color(0xFFB7D0C8), Color(0xFF53625E), Color(0xFF394541))),
    VIBRANT("Vibrant", Color(0xFF006B60), Color(0xFF59DCC8), Color(0xFFB5F1E8), Color(0xFF005047), Color(0xFF386964), Color(0xFFA8D8D1), Color(0xFF365F68), Color(0xFFA6D7E0), listOf(Color(0xFF00D3B5), Color(0xFF9ED9D3), Color(0xFF86D1DA), Color(0xFF00545A), Color(0xFF006B60))),
    EXPRESSIVE("Expressive", Color(0xFF8A4D00), Color(0xFFFFB870), Color(0xFFFFDDB9), Color(0xFF653900), Color(0xFF725A42), Color(0xFFE7C7A1), Color(0xFF35636A), Color(0xFFA6DDE3), listOf(Color(0xFFFFB870), Color(0xFFA9CFE0), Color(0xFF8AD1D3), Color(0xFF00545A), Color(0xFF8A4D00)));

    val launcherAlias: String get() = when (this) {
        TONAL_SPOT -> "LauncherTonalSpot"
        NEUTRAL -> "LauncherNeutral"
        VIBRANT -> "LauncherVibrant"
        EXPRESSIVE -> "LauncherExpressive"
    }

    fun launcherAlias(darkMode: Boolean): String = if (darkMode) "${launcherAlias}Dark" else launcherAlias

    fun colorScheme(dark: Boolean): ColorScheme {
        val base = if (dark) DarkPalette else LightPalette
        if (this == TONAL_SPOT) return base
        val primary = if (dark) primaryDark else primaryLight
        val container = if (dark) containerDark else containerLight
        val secondary = if (dark) secondaryDark else secondaryLight
        val tertiary = if (dark) tertiaryDark else tertiaryLight
        val onPrimary = if (dark) Color(0xFF00372F) else Color.White
        val onContainer = if (dark) Color(0xFFD1F5E9) else Color(0xFF002117)
        fun tint(color: Color, alpha: Float, behind: Color) = color.copy(alpha = alpha).compositeOver(behind)
        return base.copy(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = secondary,
            secondaryContainer = tint(secondary, if (dark) 0.22f else 0.15f, base.surfaceContainerHigh),
            tertiary = tertiary,
            tertiaryContainer = tint(tertiary, if (dark) 0.20f else 0.16f, base.surfaceContainerHigh),
            background = tint(primary, if (dark) 0.055f else 0.035f, base.background),
            surface = tint(primary, if (dark) 0.07f else 0.025f, base.surface),
            surfaceVariant = tint(primary, if (dark) 0.11f else 0.065f, base.surfaceVariant),
            surfaceContainerLowest = tint(primary, if (dark) 0.035f else 0.015f, base.surfaceContainerLowest),
            surfaceContainerLow = tint(primary, if (dark) 0.045f else 0.025f, base.surfaceContainerLow),
            surfaceContainer = tint(primary, if (dark) 0.06f else 0.035f, base.surfaceContainer),
            surfaceContainerHigh = tint(primary, if (dark) 0.075f else 0.045f, base.surfaceContainerHigh),
            surfaceContainerHighest = tint(primary, if (dark) 0.09f else 0.055f, base.surfaceContainerHighest),
            surfaceTint = primary, inversePrimary = if (dark) primaryLight else primaryDark
        )
    }
}

private enum class AppThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

/**
 * Apply launcher icon changes only after the activity has left the foreground.
 * Some OEM launchers stop the task when a currently running launcher alias is disabled.
 */
private fun syncLauncherPalette(context: Context, selected: AppPaletteStyle, darkMode: Boolean) {
    val preferenceKey = "${selected.name}:${if (darkMode) "dark" else "light"}"
    val iconPrefs = context.getSharedPreferences("meeting_scheduler_launcher", Context.MODE_PRIVATE)
    if (iconPrefs.getString("applied_palette", null) == preferenceKey) return

    val packageManager = context.packageManager
    val activeAlias = selected.launcherAlias(darkMode)
    val aliases = AppPaletteStyle.entries.flatMap { listOf(it.launcherAlias, it.launcherAlias(darkMode = true)) }
    val changes = aliases.map { alias ->
        val component = ComponentName(context, "${context.packageName}.$alias")
        val state = if (alias == activeAlias) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        Triple(alias, component, state)
    }

    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.setComponentEnabledSettings(changes.map { (_, component, state) ->
                PackageManager.ComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            })
        } else {
            // Apply while the activity is stopped, enable the new entry before disabling the old one.
            changes.firstOrNull { it.first == activeAlias }?.let { (_, component, state) ->
                packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
            changes.filterNot { it.first == activeAlias }.forEach { (_, component, state) ->
                packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
        iconPrefs.edit().putString("applied_palette", preferenceKey).apply()
    }
}

private val Forest @Composable get() = MaterialTheme.colorScheme.primary
private val ForestContainer @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val AppSurface @Composable get() = MaterialTheme.colorScheme.background
private val SurfaceContainer @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val Ink @Composable get() = MaterialTheme.colorScheme.onSurface
private val Muted @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val IsDarkPalette @Composable get() = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

class MainActivity : FragmentActivity() {
    internal var pendingSchedulePreviewId by mutableIntStateOf(0)
        private set

    private fun scheduleIdFromIntent(openIntent: Intent?): Int {
        val raw = runCatching { openIntent?.getStringExtra("scheduleId")?.toIntOrNull() }.getOrNull()
            ?: runCatching { openIntent?.getIntExtra("scheduleId", 0) ?: 0 }.getOrDefault(0)
        return raw.takeIf { it > 0 } ?: 0
    }

    internal fun consumeSchedulePreview(id: Int) {
        if (pendingSchedulePreviewId == id) pendingSchedulePreviewId = 0
    }

    internal lateinit var appLock: AppLockController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingSchedulePreviewId = scheduleIdFromIntent(intent)
        MeetingPushNotifications.createChannel(this)
        appLock = AppLockController(this)
        appLock.updatePrivacy()
        setContent { MeetingSchedulerRoot(window) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSchedulePreviewId = scheduleIdFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::appLock.isInitialized) appLock.onForeground()
    }

    override fun onPause() {
        if (::appLock.isInitialized) appLock.onBackground()
        super.onPause()
    }

    override fun onStop() {
        if (::appLock.isInitialized) appLock.onBackground()
        runCatching {
            val store = LocalWorkspaceStore(this)
            val darkMode = when (store.themeMode) {
                AppThemeMode.SYSTEM -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            syncLauncherPalette(this, store.paletteStyle, darkMode)
        }
        super.onStop()
    }
}

private data class Participant(
    val name: String,
    val appointment: String = "Publisher",
    val service: String = "Publisher",
    val gender: String = "Brother",
    val enabled: Boolean = true,
    val id: Int = 0,
    val unbaptized: Boolean = false
)
private fun Participant.isBrother() = gender.equals("brother", true)
private fun Participant.isAppointedBrother() = isBrother() && appointment.lowercase().replace('_',' ').let { it=="elder" || it=="ministerial" || it=="ministerial servant" || it=="ministerialservant" }

private data class Assignment(
    val title: String, val detail: String = "", val person: String = "", val id: Int = 0,
    val time: String = "", val section: String = "", val duration: String = "",
    val assistant: String = "", val secondaryAssignee: String = "", val secondaryAssistant: String = "",
    val hasAssistant: Boolean = false, val hasSecondary: Boolean = false, val hasSecondaryAssistant: Boolean = false
)
private data class ScheduleAssignmentValue(val key: String, val label: String, val person: String)
private enum class ScheduleChangeKind { ADDED, REPLACED, REMOVED }
private data class ScheduleAssignmentChange(
    val kind: ScheduleChangeKind,
    val label: String,
    val previousPerson: String = "",
    val currentPerson: String = ""
)

private fun scheduleAssignmentValues(
    parts: List<Assignment>, chairman: String, openingPrayer: String, adviser: String, audienceGroup: String
): List<ScheduleAssignmentValue> = buildList {
    add(ScheduleAssignmentValue("role:chairman", "Chairman", chairman))
    add(ScheduleAssignmentValue("role:openingPrayer", "Opening prayer", openingPrayer))
    add(ScheduleAssignmentValue("role:adviser", "Second Hall adviser", adviser))
    add(ScheduleAssignmentValue("role:group", "Group", audienceGroup))
    parts.forEachIndexed { index, part ->
        val title = part.title.trim().replace(Regex("\\s+"), " ").ifBlank { "Part ${index + 1}" }
            .let { if (it.length > 54) it.take(51).trimEnd() + "…" else it }
        val partKey = part.id.takeIf { it > 0 }?.toString() ?: "index:$index"
        fun slot(name: String, suffix: String, person: String) {
            add(ScheduleAssignmentValue("part:$partKey:$suffix", "$title · $name", person))
        }
        val lowerTitle = part.title.lowercase(Locale.ROOT)
        val bibleStudy = lowerTitle.contains("pag-aaral ng kongregasyon sa bibliya")
        val bibleReading = lowerTitle.contains("pagbabasa ng bibliya")
        val what = lowerTitle.contains("ano ang sasabihin mo")
        val dualHall = (part.section == "ministry" && !what) || bibleReading
        val qualified = !what && (part.section == "living" || (part.section == "treasures" && !bibleReading))
        val mainRole = when {
            bibleStudy -> "Conductor"
            dualHall -> "Main Hall"
            what -> "Brother"
            qualified -> "Qualified brother"
            else -> "Participant"
        }
        slot(mainRole, "main", part.person)
        if (part.hasAssistant || part.assistant.isNotBlank()) {
            slot(if (bibleStudy) "Reader" else if (dualHall) "Main Hall assistant" else "Assistant", "assistant", part.assistant)
        }
        if (part.hasSecondary || part.secondaryAssignee.isNotBlank()) slot("Second Hall", "second", part.secondaryAssignee)
        if (part.hasSecondaryAssistant || part.secondaryAssistant.isNotBlank()) slot("Second Hall assistant", "secondAssistant", part.secondaryAssistant)
    }
}

private fun summarizeScheduleChanges(
    previous: List<ScheduleAssignmentValue>, current: List<ScheduleAssignmentValue>
): List<ScheduleAssignmentChange> {
    val previousByKey = previous.associateBy { it.key }
    val currentByKey = current.associateBy { it.key }
    val orderedKeys = (previous.map { it.key } + current.map { it.key }).distinct()
    return buildList {
        orderedKeys.forEach { key ->
            val before = previousByKey[key]
            val after = currentByKey[key]
            val oldPerson = before?.person.orEmpty().trim()
            val newPerson = after?.person.orEmpty().trim()
            val label = after?.label ?: before?.label.orEmpty()
            when {
                oldPerson.isBlank() && newPerson.isNotBlank() -> add(ScheduleAssignmentChange(ScheduleChangeKind.ADDED, label, currentPerson = newPerson))
                oldPerson.isNotBlank() && newPerson.isBlank() -> add(ScheduleAssignmentChange(ScheduleChangeKind.REMOVED, label, previousPerson = oldPerson))
                oldPerson.isNotBlank() && !oldPerson.equals(newPerson, ignoreCase = true) -> add(ScheduleAssignmentChange(ScheduleChangeKind.REPLACED, label, oldPerson, newPerson))
            }
        }
    }
}

private fun missingRequiredAssignmentSlots(
    parts: List<Assignment>, chairman: String, openingPrayer: String, adviser: String
): List<String> = buildList {
    if (chairman.isBlank()) add("Chairman")
    if (openingPrayer.isBlank()) add("Opening prayer")
    if (adviser.isBlank()) add("Second Hall adviser")
    parts.forEachIndexed { index, part ->
        val title = part.title.trim().replace(Regex("\\s+"), " ").ifBlank { "Part ${index + 1}" }
            .let { if (it.length > 54) it.take(51).trimEnd() + "…" else it }
        val lowerTitle = part.title.lowercase(Locale.ROOT)
        val bibleStudy = lowerTitle.contains("pag-aaral ng kongregasyon sa bibliya")
        val bibleReading = lowerTitle.contains("pagbabasa ng bibliya")
        val what = lowerTitle.contains("ano ang sasabihin mo")
        val dualHall = (part.section == "ministry" && !what) || bibleReading
        val qualified = !what && (part.section == "living" || (part.section == "treasures" && !bibleReading))
        val mainRole = when {
            bibleStudy -> "Conductor"
            dualHall -> "Main Hall"
            what -> "Brother"
            qualified -> "Qualified brother"
            else -> "Participant"
        }
        if (part.person.isBlank()) add("$title · $mainRole")
        if (part.hasAssistant && part.assistant.isBlank()) add("$title · ${if (bibleStudy) "Reader" else if (dualHall) "Main Hall assistant" else "Assistant"}")
        if (part.hasSecondary && part.secondaryAssignee.isBlank()) add("$title · Second Hall")
        if (part.hasSecondaryAssistant && part.secondaryAssistant.isBlank()) add("$title · Second Hall assistant")
    }
}

private fun deduplicateAssignmentParticipants(parts: List<Assignment>): List<Assignment> {
    val assigned = mutableSetOf<String>()
    fun keepFirst(name: String): String {
        val key = name.trim().lowercase(Locale.ROOT)
        if (key.isBlank()) return name
        return if (assigned.add(key)) name else ""
    }
    return parts.map { part ->
        part.copy(
            person = keepFirst(part.person),
            assistant = keepFirst(part.assistant),
            secondaryAssignee = keepFirst(part.secondaryAssignee),
            secondaryAssistant = keepFirst(part.secondaryAssistant)
        )
    }
}

private fun assignParticipantToOneSlot(parts: MutableList<Assignment>, partIndex: Int, slot: String, name: String) {
    if (partIndex !in parts.indices) return
    if (name.isNotBlank()) {
        parts.indices.forEach { index ->
            val part = parts[index]
            fun keep(field: String): Boolean = index == partIndex && slot == field
            fun clearDuplicate(value: String, field: String): String =
                if (!keep(field) && value.trim().equals(name.trim(), ignoreCase = true)) "" else value
            parts[index] = part.copy(
                person = clearDuplicate(part.person, "main"),
                assistant = clearDuplicate(part.assistant, "assistant"),
                secondaryAssignee = clearDuplicate(part.secondaryAssignee, "secondary"),
                secondaryAssistant = clearDuplicate(part.secondaryAssistant, "secondaryAssistant")
            )
        }
    }
    val part = parts[partIndex]
    parts[partIndex] = when (slot) {
        "assistant" -> part.copy(assistant = name)
        "secondary" -> part.copy(secondaryAssignee = name)
        "secondaryAssistant" -> part.copy(secondaryAssistant = name)
        else -> part.copy(person = name)
    }
}

private fun Assignment.withoutParticipant(name: String): Assignment {
    if (name.isBlank()) return this
    fun clear(value: String): String =
        if (value.trim().equals(name.trim(), ignoreCase = true)) "" else value
    return copy(
        person = clear(person),
        assistant = clear(assistant),
        secondaryAssignee = clear(secondaryAssignee),
        secondaryAssistant = clear(secondaryAssistant)
    )
}


private data class SavedSchedule(
    val week: String,
    val reading: String,
    val saved: String,
    val weekStart: String = "",
    val openingSong: String = "27",
    val closingSong: String = "132",
    val assignments: List<Assignment> = emptyList(),
    val chairman: String = "", val openingPrayer: String = "", val adviser: String = "", val audienceGroup: String = ""
)
internal enum class Screen(val title: String, val section: String) {
    HOME("Home", "OVERVIEW"),
    DETAILS("Workbook", "SCHEDULE TOOLS"),
    ASSIGNMENTS("Assignments", "SCHEDULE TOOLS"),
    PREVIEW("Preview & Export", "SCHEDULE TOOLS"),
    PARTICIPANTS("Participants", "CONGREGATION"),
    HISTORY("History", "CONGREGATION"),
    ACCOUNTS("Account & Settings", "ACCOUNT")
}

private class LocalWorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("meeting_scheduler_workspace", Context.MODE_PRIVATE)
    private fun profileKey(kind: String, username: String) = "${kind}_${username.trim().lowercase(Locale.ROOT).ifBlank { "account" }}"
    fun profileName(username: String, default: String) = prefs.getString(profileKey("profile_name", username), null)?.takeIf { it.isNotBlank() } ?: default
    fun saveProfileName(username: String, value: String) { prefs.edit().putString(profileKey("profile_name", username), value).apply() }
    fun profilePhoto(username: String) = prefs.getString(profileKey("profile_photo_path", username), null)
    fun saveProfilePhoto(username: String, path: String) { prefs.edit().putString(profileKey("profile_photo_path", username), path).apply() }
    var themeMode: AppThemeMode
        get() = prefs.getString("theme_mode", null)?.let { saved ->
            runCatching { AppThemeMode.valueOf(saved) }.getOrNull()
        } ?: if (prefs.contains("dark_mode")) {
            if (prefs.getBoolean("dark_mode", true)) AppThemeMode.DARK else AppThemeMode.LIGHT
        } else AppThemeMode.SYSTEM
        set(value) { prefs.edit().putString("theme_mode", value.name).apply() }
    var paletteStyle: AppPaletteStyle
        get() = runCatching { AppPaletteStyle.valueOf(prefs.getString("palette_style", AppPaletteStyle.TONAL_SPOT.name) ?: AppPaletteStyle.TONAL_SPOT.name) }
            .getOrDefault(AppPaletteStyle.TONAL_SPOT)
        set(value) { prefs.edit().putString("palette_style", value.name).apply() }
    var appFontScale: Float
        get() = prefs.getFloat("app_font_scale", 1f).coerceIn(0.85f, 1.35f)
        set(value) { prefs.edit().putFloat("app_font_scale", value.coerceIn(0.85f, 1.35f)).apply() }
    fun detail(key: String, fallback: String) = prefs.getString("detail_$key", fallback) ?: fallback
    fun setDetail(key: String, value: String) { prefs.edit().putString("detail_$key", value).apply() }
    fun workbookWeekStart(fallback: String): String {
        val saved = prefs.getString("detail_week", null) ?: return fallback
        if (saved == LEGACY_WORKBOOK_WEEK_START && loadHistory().none { it.weekStart == LEGACY_WORKBOOK_WEEK_START }) {
            setDetail("week", fallback)
            return fallback
        }
        return saved
    }
    fun saveDetails(week: String, bible: String, opening: String, closing: String, workbook: String) {
        prefs.edit().putString("detail_week", week).putString("detail_bible", bible)
            .putString("detail_opening", opening).putString("detail_closing", closing)
            .putString("detail_workbook", workbook).apply()
    }
    fun loadParticipants(): List<Participant> = readList("participants") { o ->
        Participant(o.optString("name"), o.optString("appointment", "publisher"), o.optString("service", "none"), o.optString("gender", "brother"), o.optBoolean("enabled", true),o.optInt("id"),o.optBoolean("unbaptized",false))
    } ?: sampleParticipants()
    fun saveParticipants(items: List<Participant>) = writeList("participants", items.map { o -> JSONObject().put("name",o.name).put("appointment",o.appointment).put("service",o.service).put("gender",o.gender).put("enabled",o.enabled).put("id",o.id).put("unbaptized",o.unbaptized) })
    fun loadAssignments(): List<Assignment> = readList("assignments") { o -> o.toAssignment() } ?: sampleAssignments()
    fun saveAssignments(items: List<Assignment>) = writeList("assignments", items.map { it.toJson() })
    fun loadHistory(): List<SavedSchedule> = readList("history") { o ->
        val a = o.optJSONArray("assignments") ?: JSONArray()
        val assignments = buildList { for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(it.toAssignment()) } }
        SavedSchedule(o.optString("week"),o.optString("reading"),o.optString("saved"),o.optString("weekStart"),o.optString("openingSong","27"),o.optString("closingSong","132"),assignments,o.optString("chairman"),o.optString("openingPrayer"),o.optString("adviser"),o.optString("audienceGroup"))
    } ?: sampleHistory()
    fun saveHistory(items: List<SavedSchedule>) = writeList("history", items.map { item ->
        val assignments = JSONArray().apply { item.assignments.forEach { put(it.toJson()) } }
        JSONObject().put("week",item.week).put("reading",item.reading).put("saved",item.saved).put("weekStart",item.weekStart).put("openingSong",item.openingSong).put("closingSong",item.closingSong).put("assignments",assignments).put("chairman",item.chairman).put("openingPrayer",item.openingPrayer).put("adviser",item.adviser).put("audienceGroup",item.audienceGroup)
    })
    private fun <T> readList(key: String, map: (JSONObject) -> T): List<T>? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { val a=JSONArray(raw); buildList { for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(map(it)) } } }.getOrNull()
    }
    private fun writeList(key: String, values: List<JSONObject>) { prefs.edit().putString(key,JSONArray(values).toString()).apply() }
}

private fun JSONObject.toAssignment() = Assignment(
    title=optString("title"), detail=optString("detail"), person=optString("person", optString("assignee")),
    id=optInt("id"), time=optString("time"), section=optString("section"), duration=optString("duration"),
    assistant=optString("assistant"), secondaryAssignee=optString("secondaryAssignee"), secondaryAssistant=optString("secondaryAssistant"),
    hasAssistant=optBoolean("hasAssistant", has("assistant")), hasSecondary=optBoolean("hasSecondary", has("secondaryAssignee")),
    hasSecondaryAssistant=optBoolean("hasSecondaryAssistant", has("secondaryAssistant"))
)
private fun Assignment.toJson() = JSONObject().put("title",title).put("detail",detail).put("person",person).put("assignee",person).put("id",id)
    .put("time",time).put("section",section).put("duration",duration).put("assistant",assistant)
    .put("secondaryAssignee",secondaryAssignee).put("secondaryAssistant",secondaryAssistant)
    .put("hasAssistant",hasAssistant).put("hasSecondary",hasSecondary).put("hasSecondaryAssistant",hasSecondaryAssistant)

internal data class AvailableAppUpdate(val version: String, val apkUrl: String, val notes: String)

@Composable
private fun AppUpdatePrompt() {
    val context = LocalContext.current
    var update by remember { mutableStateOf<AvailableAppUpdate?>(null) }
    var downloadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadProgress by remember { mutableStateOf<AppUpdateDownloadProgress?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var installPermissionMissing by remember { mutableStateOf(false) }

    val updateCheckScope = rememberCoroutineScope()
    val activity = context as? MainActivity
    DisposableEffect(activity) {
        var checkJob: kotlinx.coroutines.Job? = null
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                checkJob?.cancel()
                checkJob = updateCheckScope.launch {
                    val latest = runCatching {
                        withContext(Dispatchers.IO) { AppUpdateClient.checkForUpdate(BuildConfig.VERSION_NAME) }
                    }.getOrNull()
                    if (latest != null) update = latest
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            checkJob?.cancel()
            activity?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        while (true) {
            val state = runCatching {
                withContext(Dispatchers.IO) { AppUpdateClient.downloadProgress(context, id) }
            }.getOrNull()
            downloadProgress = state
            if (state == null || state.complete || state.failed) break
            delay(500)
        }
        if (downloadProgress?.failed == true) downloadError = "Hindi natuloy ang download. Subukan ulit."
    }

    update?.let { available ->
        val downloading = downloadId != null && downloadProgress?.complete != true && downloadProgress?.failed != true
        val complete = downloadProgress?.complete == true
        val progressValue = (downloadProgress?.percent ?: 0).coerceIn(0, 100) / 100f
        val notes = available.notes.lineSequence()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .take(3)
            .toList()

        AlertDialog(
            onDismissRequest = { if (!downloading) update = null },
            icon = {
                Surface(shape = CircleShape, color = ForestContainer, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(SproutIcons.SystemUpdateAlt, contentDescription = null, tint = Forest, modifier = Modifier.size(29.dp))
                    }
                }
            },
            title = { Text("May bagong update", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.48f).dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("JW Meeting Manager ${available.version} ay handa nang i-update.", color = Ink)
                    if (notes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Mga bagong pagbabago", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            notes.forEach { Text("• $it", color = Muted, fontSize = 12.sp, lineHeight = 17.sp) }
                        }
                    }
                    if (downloadId != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                when {
                                    complete -> "Handa nang i-install"
                                    downloadProgress?.failed == true -> "Hindi natapos ang download"
                                    else -> "Dina-download ang update…"
                                },
                                color = if (downloadProgress?.failed == true) MaterialTheme.colorScheme.error else Forest,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            LinearProgressIndicator(
                                progress = { if (complete) 1f else progressValue },
                                modifier = Modifier.fillMaxWidth(),
                                color = Forest,
                                trackColor = ForestContainer
                            )
                            Text(
                                if (complete) "100%" else downloadProgress?.percent?.let { "$it%" } ?: "Inihahanda ang download…",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (installPermissionMissing) {
                        Text("Payagan ang pag-install mula sa app na ito sa Android settings, pagkatapos ay pindutin ulit ang Install update.", color = Muted, fontSize = 12.sp)
                    }
                    downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    if (complete) Text("Ilulunsad ng Android ang confirmation bago i-install.", color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = !downloading,
                    onClick = {
                        val activity = context as? MainActivity ?: return@Button
                        downloadError = null
                        if (complete && downloadId != null) {
                            if (AppUpdateClient.canInstallPackages(activity)) {
                                runCatching { AppUpdateClient.install(activity, downloadId!!) }
                                    .onFailure { downloadError = it.message ?: "Hindi mabuksan ang installer." }
                            } else {
                                installPermissionMissing = true
                                runCatching { AppUpdateClient.openInstallSettings(activity) }
                                    .onFailure { downloadError = it.message ?: "Hindi mabuksan ang Android settings." }
                            }
                        } else if (downloadProgress?.failed == true || downloadId == null) {
                            runCatching {
                                downloadId = AppUpdateClient.download(activity, available.apkUrl)
                                downloadProgress = null
                                installPermissionMissing = false
                            }.onFailure { downloadError = it.message ?: "Hindi masimulan ang download." }
                        }
                    },
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Icon(SproutIcons.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            downloading -> "Downloading…"
                            complete -> "Install update"
                            downloadProgress?.failed == true -> "Try again"
                            else -> "Download update"
                        }
                    )
                }
            },
            dismissButton = {
                if (!downloading) TextButton(onClick = { update = null }) { Text("Mamaya", color = Forest) }
            },
            containerColor = AppSurface
        )
    }
}

@Composable
private fun MeetingSchedulerRoot(window: android.view.Window) {
    val context = LocalContext.current
    val activity = androidx.activity.compose.LocalActivity.current as MainActivity
    val appLock = activity.appLock
    val workspaceState = rememberSaveableStateHolder()
    val store = remember(context) { LocalWorkspaceStore(context) }
    val workspacePreferences = remember(context) { context.getSharedPreferences("meeting_scheduler_workspace_choice", Context.MODE_PRIVATE) }
    var deviceMode by remember { mutableStateOf(workspacePreferences.getBoolean("local", false)) }
    fun setWorkspace(local: Boolean) {
        workspacePreferences.edit().putBoolean("local", local).apply()
        deviceMode = local
    }
    LaunchedEffect(activity.pendingSchedulePreviewId) {
        if (activity.pendingSchedulePreviewId > 0 && deviceMode) setWorkspace(false)
    }
    var themeMode by rememberSaveable { mutableStateOf(store.themeMode) }
    val darkMode = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    var paletteStyle by rememberSaveable { mutableStateOf(store.paletteStyle) }
    var appFontScale by rememberSaveable { mutableFloatStateOf(store.appFontScale) }
    val scheme = paletteStyle.colorScheme(darkMode)
    val systemDensity = LocalDensity.current
    SideEffect {
        window.statusBarColor = scheme.background.toArgb()
        window.navigationBarColor = scheme.background.toArgb()
        val lightNavigation = if (android.os.Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        window.decorView.systemUiVisibility = if (darkMode) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or lightNavigation)
    }
    CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * appFontScale)) {
        MaterialTheme(colorScheme = scheme, shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ), typography = SchedulerTypography) {
            Box(Modifier.fillMaxSize()) {
                if (appLock.state.locked) {
                    AppLockScreen(appLock)
                } else if (deviceMode) {
                    workspaceState.SaveableStateProvider("deviceWorkspace") {
                        DeviceWorkspaceApp(
                            onOpenShared = { setWorkspace(false) },
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it; store.themeMode = it },
                            paletteStyle = paletteStyle,
                            onPaletteStyleChange = { paletteStyle = it; store.paletteStyle = it },
                            fontScale = appFontScale,
                            onFontScaleChange = { appFontScale = it.coerceIn(0.85f, 1.35f); store.appFontScale = appFontScale },
                            appLock = appLock
                        )
                    }
                } else {
                    workspaceState.SaveableStateProvider("workspace") {
                        MeetingSchedulerApp(
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it; store.themeMode = it },
                            paletteStyle = paletteStyle,
                            onPaletteStyleChange = { paletteStyle = it; store.paletteStyle = it },
                            store = store,
                            fontScale = appFontScale,
                            onFontScaleChange = { appFontScale = it.coerceIn(0.85f, 1.35f); store.appFontScale = appFontScale },
                            appLock = appLock,
                            onOpenLocal = { setWorkspace(true) }
                        )
                        AppUpdatePrompt()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingSchedulerApp(themeMode: AppThemeMode, onThemeModeChange: (AppThemeMode) -> Unit, paletteStyle: AppPaletteStyle, onPaletteStyleChange: (AppPaletteStyle) -> Unit, store: LocalWorkspaceStore, fontScale: Float, onFontScaleChange: (Float) -> Unit, appLock: AppLockController, onOpenLocal: () -> Unit) {
    val context = LocalContext.current
    val api = remember(context) { SchedulerApi(context) }
    val authScope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) MeetingPushNotifications.syncToken(context, api)
    }
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var authReady by rememberSaveable { mutableStateOf(false) }
    var loginBusy by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordChangeRequired by rememberSaveable { mutableStateOf(false) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1_050); splashFinished = true }
    var identityName by rememberSaveable { mutableStateOf("") }
    var identityUsername by rememberSaveable { mutableStateOf("") }
    var identityRole by rememberSaveable { mutableStateOf("") }
    var identityToolAccess by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var profilePhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var profilePhotoDataUrl by remember { mutableStateOf("") }
    var profileMenuOpen by rememberSaveable { mutableStateOf(false) }
    var workspacePickerOpen by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var workbookStatus by rememberSaveable { mutableStateOf("Naghahanap ng workbook para sa napiling linggo…") }
    var workbookBusy by remember { mutableStateOf(false) }
    var workbookCaptureLog by remember { mutableStateOf(listOf("READY · waiting for the selected meeting week")) }
    var cloudStatus by rememberSaveable { mutableStateOf("Konektado sa website") }
    var cloudSaveBusy by remember { mutableStateOf(false) }
    var cloudSaveFeedback by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var participantAdmin by rememberSaveable { mutableStateOf(false) }
    var participantCanEdit by rememberSaveable { mutableStateOf(false) }
    var showNotificationPermissionPrompt by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(signedIn, identityUsername, splashFinished) {
        if (!signedIn || identityUsername.isBlank() || !splashFinished) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val permissionPrefs = context.getSharedPreferences("meeting_notification_permission", Context.MODE_PRIVATE)
            if (!permissionPrefs.getBoolean("asked", false)) showNotificationPermissionPrompt = true
        } else {
            MeetingPushNotifications.syncToken(context, api)
        }
    }
    LaunchedEffect(api) {
        if (!api.hasStoredSession()) {
            authReady = true
            return@LaunchedEffect
        }
        runCatching { api.currentUser() }.onSuccess {
            identityName = store.profileName(it.username, it.name); identityUsername = it.username
            profilePhotoPath = store.profilePhoto(it.username); profilePhotoDataUrl = it.photoDataUrl; identityRole = it.role; identityToolAccess = it.toolAccess; identityToolAccess = it.toolAccess
            if (it.mustChangePassword) passwordChangeRequired = true else signedIn = true
        }
        authReady = true
    }
    if (showNotificationPermissionPrompt) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPermissionPrompt = false
                context.getSharedPreferences("meeting_notification_permission", Context.MODE_PRIVATE).edit().putBoolean("asked", true).apply()
            },
            title = { Text("Get assignment notifications") },
            text = { Text("We can notify you when a newly saved cloud schedule includes an assignment for you.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionPrompt = false
                    context.getSharedPreferences("meeting_notification_permission", Context.MODE_PRIVATE).edit().putBoolean("asked", true).apply()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationPermissionPrompt = false
                    context.getSharedPreferences("meeting_notification_permission", Context.MODE_PRIVATE).edit().putBoolean("asked", true).apply()
                }) { Text("Not now") }
            }
        )
    }
    if (!splashFinished) { MeetingSplashScreen(); return }
    if (!authReady) {
        PageColumn(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            PageHeading("JW Meeting Manager")
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text("Checking Shared Workspace…", color = Muted)
            OutlinedButton(onClick = onOpenLocal) { Text("Open Local Workspace") }
        }
        return
    }
    if (!signedIn) {
        if (!authReady) {
            Surface(color = AppSurface, modifier = Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) } }
        } else LoginScreen(
            busy = loginBusy, error = loginError, mustChangePassword = passwordChangeRequired,
            onOpenLocal = onOpenLocal,
            onRequestAccess = { name, username, note -> api.requestAccess(name, username, note); Unit },
            onChangePassword = { currentPassword, newPassword ->
                authScope.launch {
                    loginBusy = true; loginError = null
                    runCatching { api.changePassword(currentPassword, newPassword) }
                        .onSuccess { passwordChangeRequired = false; signedIn = true }
                        .onFailure { loginError = it.message ?: "Could not change your password." }
                    loginBusy = false
                }
            },
            onLogin = { username, password ->
                authScope.launch {
                    loginBusy = true; loginError = null
                    runCatching { api.login(username, password) }
                        .onSuccess {
                            identityName = store.profileName(it.username, it.name); identityUsername = it.username
                            profilePhotoPath = store.profilePhoto(it.username); profilePhotoDataUrl = it.photoDataUrl; identityRole = it.role
                            if (it.mustChangePassword) passwordChangeRequired = true else signedIn = true
                        }
                        .onFailure { loginError = it.message ?: "Could not sign in." }
                    loginBusy = false
                }
            }
        )
        return
    }

    var current by rememberSaveable { mutableStateOf(Screen.HOME) }
    val officialPreviewWebView = remember { mutableStateOf<WebView?>(null) }
    var toolsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(profileMenuOpen, toolsOpen) {
        if (profileMenuOpen || toolsOpen) runCatching { api.currentUser() }
            .onSuccess { profilePhotoDataUrl = it.photoDataUrl; identityToolAccess = it.toolAccess; identityRole = it.role }
    }
    val currentWorkbookWeekStart = remember {
        LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    }
    var weekStart by rememberSaveable {
        mutableStateOf(store.workbookWeekStart(currentWorkbookWeekStart.toString()))
    }
    var bible by rememberSaveable { mutableStateOf(store.detail("bible", "Jeremias 31")) }
    var openingSong by rememberSaveable { mutableStateOf(store.detail("opening", "27")) }
    var closingSong by rememberSaveable { mutableStateOf(store.detail("closing", "132")) }
    var workbook by rememberSaveable { mutableStateOf(store.detail("workbook", "https://www.jw.org/finder?srcid=jwlshare&wtlocale=TG&prefer=lang&docid=202026249")) }
    var datePicker by remember { mutableStateOf(false) }
    var chairman by rememberSaveable { mutableStateOf(store.detail("chairman", "")) }
    var openingPrayer by rememberSaveable { mutableStateOf(store.detail("opening_prayer", "")) }
    var adviser by rememberSaveable { mutableStateOf(store.detail("adviser", "")) }
    var audienceGroup by rememberSaveable { mutableStateOf(store.detail("audience_group", "")) }
    val participants = remember(store) { mutableStateListOf<Participant>().apply { addAll(store.loadParticipants()) } }
    val storedAssignments = remember(store) { store.loadAssignments() }
    val parts = remember(store) {
        mutableStateListOf<Assignment>().apply { addAll(deduplicateAssignmentParticipants(storedAssignments)) }
    }
    LaunchedEffect(storedAssignments) {
        val unique = deduplicateAssignmentParticipants(storedAssignments)
        if (unique != storedAssignments) store.saveAssignments(unique)
    }
    val history = remember(store) { mutableStateListOf<SavedSchedule>().apply { addAll(store.loadHistory()) } }
    var participantsLoaded by remember { mutableStateOf(false) }
    var participantsBusy by remember { mutableStateOf(false) }
    var participantsLoadError by remember { mutableStateOf<String?>(null) }
    var rotationStats by remember { mutableStateOf<Map<Int, ParticipantRotation>>(emptyMap()) }
    var rotationBusy by remember { mutableStateOf(false) }
    var rotationError by remember { mutableStateOf<String?>(null) }
    var remoteRows by remember { mutableStateOf<List<RemoteSchedule>>(emptyList()) }
    var homeSchedules by remember { mutableStateOf<List<RemoteSchedule>>(emptyList()) }
    var homeLoaded by remember { mutableStateOf(false) }
    var homeBusy by remember { mutableStateOf(false) }
    var homeError by remember { mutableStateOf<String?>(null) }
    var homePreviewHtml by remember { mutableStateOf<String?>(null) }
    var homePreviewRequest by remember { mutableIntStateOf(0) }
    var homePreviewWeek by remember { mutableStateOf<String?>(null) }
    var homePreviewLoading by remember { mutableStateOf(false) }
    var editingRemoteId by rememberSaveable { mutableIntStateOf(0) }
    var originalCloudUpdatedAt by rememberSaveable { mutableStateOf("") }
    var cloudSaveReviewVisible by remember { mutableStateOf(false) }
    var cloudConflictVisible by remember { mutableStateOf(false) }
    var originalCloudAssignments by remember { mutableStateOf<List<ScheduleAssignmentValue>>(emptyList()) }
    var suppressWeekLookup by remember { mutableStateOf(false) }
    var remoteScheduleLoadedWeek by rememberSaveable { mutableStateOf("") }
    var weekLoadNonce by remember { mutableIntStateOf(0) }
    var historyLoaded by remember { mutableStateOf(false) }
    var historyBusy by remember { mutableStateOf(false) }
    var historyLoadingMore by remember { mutableStateOf(false) }
    var historyHasMore by remember { mutableStateOf(false) }
    var historyNextOffset by remember { mutableIntStateOf(0) }
    var availableHistoryYears by remember { mutableStateOf<List<Int>>(emptyList()) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var historyReloadTick by remember { mutableIntStateOf(0) }
    var remoteAccounts by remember { mutableStateOf<List<RemoteAccount>>(emptyList()) }
    var accountLinkParticipants by remember { mutableStateOf<List<AccountLinkParticipant>>(emptyList()) }
    var accountsLoaded by remember { mutableStateOf(false) }
    var accountsBusy by remember { mutableStateOf(false) }
    var accountsError by remember { mutableStateOf<String?>(null) }
    var testNotificationBusy by remember { mutableStateOf(false) }
    var testNotificationFeedback by remember { mutableStateOf<NotificationFeedback?>(null) }
    var accountReloadTick by remember { mutableIntStateOf(0) }
    var accountPhotoTarget by remember { mutableStateOf<RemoteAccount?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var shareSuccessType by rememberSaveable { mutableStateOf<String?>(null) }
    var exportBusyType by remember { mutableStateOf<String?>(null) }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val prefs = context.getSharedPreferences(SHARE_RESULT_PREFS, Context.MODE_PRIVATE)
        val pending = prefs.getString("pending_type", null)
        val selected = prefs.getString("selected_type", null)
        if (pending != null && pending == selected) shareSuccessType = pending
        prefs.edit().remove("pending_type").remove("selected_type").apply()
    }
    fun launchScheduleShare(intent: Intent, type: String, chooserTitle: String) {
        val prefs = context.getSharedPreferences(SHARE_RESULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("pending_type", type).remove("selected_type").apply()
        val callback = Intent(context, ShareTargetChosenReceiver::class.java)
            .setAction(SHARE_CHOSEN_ACTION)
            .putExtra("share_type", type)
        val requestCode = when (type) { "png" -> 451; "slips_image" -> 454; else -> 452 }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            callback,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val chooser = Intent.createChooser(intent, chooserTitle, pendingIntent.intentSender)
        runCatching { shareLauncher.launch(chooser) }.onSuccess {
            exportBusyType = null
        }.onFailure {
            prefs.edit().remove("pending_type").remove("selected_type").apply()
            exportBusyType = null
            toastMessage = "Hindi mabuksan ang sharing menu: ${it.message ?: "subukan muli"}"
        }
    }
    var restrictedTapCount by rememberSaveable { mutableIntStateOf(0) }
    var accessNotice by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val week = remember(weekStart, currentWorkbookWeekStart) {
        runCatching { LocalDate.parse(weekStart) }.getOrDefault(currentWorkbookWeekStart)
    }
    val weekLabel = remember(week) { week.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + week.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy")) }
    fun toolKey(screen: Screen): String = when (screen) {
        Screen.HOME -> "home"
        Screen.DETAILS -> "workbook"
        Screen.ASSIGNMENTS -> "assignments"
        Screen.PREVIEW -> "preview"
        Screen.PARTICIPANTS -> "participants"
        Screen.HISTORY -> "history"
        Screen.ACCOUNTS -> "accounts"
    }
    fun toolMode(screen: Screen): String {
        if (identityRole.equals("admin", true) || identityRole.equals("administrator", true)) return "edit"
        val key = toolKey(screen)
        if (key == "accounts") return "hidden"
        val defaults = if (identityRole.equals("scheduler", true)) mapOf(
            "home" to "view", "workbook" to "edit", "assignments" to "edit",
            "preview" to "edit", "participants" to "hidden", "history" to "view"
        ) else mapOf("home" to "view", "workbook" to "hidden", "assignments" to "hidden", "preview" to "hidden", "participants" to "hidden", "history" to "hidden")
        return identityToolAccess[key] ?: defaults[key] ?: "hidden"
    }
    fun canAccess(screen: Screen): Boolean = if (screen == Screen.ACCOUNTS) signedIn else toolMode(screen) != "hidden"
    fun canEdit(screen: Screen): Boolean = identityRole.equals("admin", true) || (identityRole.equals("scheduler", true) && toolMode(screen) == "edit")
    fun navigateTo(screen: Screen) {
        if (canAccess(screen)) current = screen
        else {
            restrictedTapCount += 1
            accessNotice = if (restrictedTapCount >= 7)
                "Please Don't Spam, Just Message the Administrator for Access." to true
            else "Your Account Doesn't Have Access to This Tool." to false
        }
    }

    LaunchedEffect(signedIn, identityToolAccess, identityRole) {
        if (signedIn && !canAccess(current)) current = Screen.values().firstOrNull { canAccess(it) } ?: Screen.HOME
    }

    LaunchedEffect(current, signedIn, homeLoaded) {
        if (current == Screen.HOME && signedIn && !homeLoaded && !homeBusy) {
            homeBusy = true
            homeError = null
            runCatching { api.schedules() }
                .onSuccess { homeSchedules = it; homeLoaded = true }
                .onFailure { homeError = it.message ?: "Hindi makuha ang upcoming meetings."; homeLoaded = true }
            homeBusy = false
        }
    }

    LaunchedEffect(weekStart, signedIn, participantsLoaded) {
        if (signedIn && participantsLoaded) {
            rotationBusy = true
            rotationError = null
            runCatching { api.participantRotation(weekStart) }
                .onSuccess { stats -> rotationStats = stats.associateBy { it.memberId } }
                .onFailure { error ->
                    rotationStats = emptyMap()
                    rotationError = error.message ?: "Hindi makuha ang participant rotation."
                }
            rotationBusy = false
        } else {
            rotationStats = emptyMap()
            rotationBusy = false
            rotationError = null
        }
    }

    LaunchedEffect(signedIn, participantsLoaded, current) {
        val participantDataNeeded = current == Screen.PARTICIPANTS || current == Screen.ASSIGNMENTS || current == Screen.DETAILS
        if(signedIn && participantDataNeeded && !participantsLoaded && !participantsBusy && (canAccess(Screen.PARTICIPANTS) || canAccess(Screen.ASSIGNMENTS))) {
            participantsBusy=true
            participantsLoadError=null
            runCatching { api.members(includeInactive=true) }.onSuccess { response ->
                participantAdmin=response.optBoolean("isAdmin",participantAdmin); participantCanEdit=response.optBoolean("canManageRoles",participantCanEdit)
                val rows=response.optJSONArray("members") ?: JSONArray(); participants.clear()
                for(i in 0 until rows.length()) rows.optJSONObject(i)?.let { row -> participants.add(Participant(row.optString("name"),row.optString("appointment"),row.optString("serviceStatus"),row.optString("gender").ifBlank{"unspecified"},row.optBoolean("active",true),row.optInt("id"),row.optBoolean("unbaptized",false))) }
                store.saveParticipants(participants.toList()); participantsLoaded=true; participantsLoadError=null; cloudStatus="Nakuha ang participants mula sa website"
            }.onFailure { participantsLoadError=it.message ?: "Suriin ang internet at subukan ulit."; cloudStatus="Hindi makuha ang participants: ${it.message ?: "server error"}"; participantsLoaded=true }
            participantsBusy=false
        }
    }

    fun saveSchedule() {
        val saved = SavedSchedule(weekLabel, bible, LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")), weekStart, openingSong, closingSong, parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
        history.removeAll { it.weekStart == weekStart }
        history.add(0, saved)
        store.saveHistory(history.toList())
        store.saveDetails(weekStart, bible, openingSong, closingSong, workbook)
        store.saveAssignments(parts.toList())
        toastMessage = "Na-save sa device"
    }
    fun clearMeetingRoles() {
        chairman=""; openingPrayer=""; adviser=""; audienceGroup=""
        store.setDetail("chairman",""); store.setDetail("opening_prayer",""); store.setDetail("adviser",""); store.setDetail("audience_group","")
    }
    fun saveToCloud() {
        if (!signedIn || identityRole != "admin") {
            toastMessage = "Admin account lang ang puwedeng mag-save sa cloud"
            return
        }
        val missingSlots = missingRequiredAssignmentSlots(parts, chairman, openingPrayer, adviser)
        if (missingSlots.isNotEmpty()) {
            val message = "Complete the ${missingSlots.size} remaining required assignment slot${if (missingSlots.size == 1) "" else "s"} before saving."
            cloudSaveFeedback = message to false
            toastMessage = message
            return
        }
        if (cloudSaveBusy || cloudSaveReviewVisible) return
        cloudSaveFeedback = null
        cloudSaveReviewVisible = true
    }
    fun confirmSaveToCloud() {
        if (!signedIn || identityRole != "admin" || cloudSaveBusy) return
        val missingSlots = missingRequiredAssignmentSlots(parts, chairman, openingPrayer, adviser)
        if (missingSlots.isNotEmpty()) {
            cloudSaveReviewVisible = false
            val message = "Complete the ${missingSlots.size} remaining required assignment slot${if (missingSlots.size == 1) "" else "s"} before saving."
            cloudSaveFeedback = message to false
            toastMessage = message
            return
        }
        cloudSaveReviewVisible = false
        authScope.launch {
        cloudSaveBusy = true
        cloudSaveFeedback = null
        cloudStatus = "Sine-save sa cloud…"
        runCatching { api.saveSchedule(weekLabel, weekStart, bible, openingSong, closingSong, parts.map { it.toJson() }, chairman, openingPrayer, adviser, audienceGroup, workbook, editingRemoteId, originalCloudUpdatedAt) }
            .onSuccess { historySaved ->
                cloudStatus = if (historySaved) "Naka-save sa cloud" else "Na-save; hindi na-update ang assignment history"
                cloudSaveFeedback = cloudStatus to historySaved
                toastMessage = cloudStatus
                historyLoaded=false
                originalCloudAssignments = scheduleAssignmentValues(parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
                editingRemoteId=0
                originalCloudUpdatedAt=""
            }
            .onFailure {
                cloudStatus = "Hindi na-save: ${it.message ?: "server error"}"
                cloudSaveFeedback = cloudStatus to false
                toastMessage = cloudStatus
                if (it.message?.startsWith("SCHEDULE_CONFLICT:") == true) cloudConflictVisible = true
            }
        cloudSaveBusy = false
        }
    }
    fun persistDetails() = store.saveDetails(weekStart, bible, openingSong, closingSong, workbook)
    suspend fun fetchWorkbookForSelectedWeek() {
        workbookBusy = true
        workbookStatus = "Kinukuha ang workbook para sa $weekLabel…"
        workbookCaptureLog = listOf("$ capture-workbook --week $weekStart", "INIT · preparing a fresh workbook fetch")
        clearMeetingRoles(); parts.clear(); store.saveAssignments(emptyList())
        runCatching { api.importForWeek(weekStart) { step -> withContext(Dispatchers.Main.immediate) { workbookCaptureLog = (workbookCaptureLog + step).takeLast(7) } } }
            .onSuccess { result ->
                workbook = result.link
                bible = result.meta.optString("reading", bible)
                openingSong = result.meta.optString("openingSong", openingSong)
                closingSong = result.meta.optString("closingSong", closingSong)
                parts.clear()
                result.parts.forEach { part ->
                    parts.add(Assignment(
                        title=part.optString("title"), detail=part.optString("detail"), person=part.optString("assignee"),
                        id=part.optInt("id"), time=part.optString("time"), section=part.optString("section"), duration=part.optString("duration"),
                        assistant=part.optString("assistant"), secondaryAssignee=part.optString("secondaryAssignee"), secondaryAssistant=part.optString("secondaryAssistant"),
                        hasAssistant=part.has("assistant"), hasSecondary=part.has("secondaryAssignee"), hasSecondaryAssistant=part.has("secondaryAssistant")
                    ))
                }
                val unique = deduplicateAssignmentParticipants(parts.toList())
                parts.clear(); parts.addAll(unique)
                persistDetails()
                store.saveAssignments(parts.toList())
                workbookStatus = "✓ ${result.parts.size} meeting parts imported from JW.ORG"
                remoteScheduleLoadedWeek = weekStart
                workbookCaptureLog = (workbookCaptureLog + "PARSED · ${result.parts.size} meeting parts" + "DONE · metadata and assignments saved locally").takeLast(7)
            }
            .onFailure {
                workbookCaptureLog = (workbookCaptureLog + "ERROR · ${it.message ?: "workbook capture failed"}").takeLast(7)
                workbookStatus = when {
                    it.message?.contains("format was not recognized", true) == true -> "Hindi mabasa ang workbook page para sa linggong ito. Subukan ulit."
                    it.message?.contains("No workbook", true) == true || it.message?.contains("Walang nailathalang", true) == true -> "Wala pang workbook na nailalathala para sa linggong ito."
                    it.message?.contains("Viewer accounts", true) == true -> "Walang permission ang viewer account para mag-import ng workbook."
                    else -> it.message ?: "Hindi makuha ang workbook. Suriin ang internet at subukan ulit."
                }
            }
        workbookBusy = false
    }
    suspend fun applyRemoteSchedule(remote: RemoteSchedule, navigate: Boolean = false, edit: Boolean = false) {
        val row=api.schedule(remote.id); val meta=row.optJSONObject("meta") ?: JSONObject(); val raw=row.optJSONArray("parts") ?: JSONArray()
        val savedWeekStart=meta.optString("weekStart").ifBlank { remoteScheduleStartDate(remote)?.toString() ?: weekStart }
        if(savedWeekStart!=weekStart) suppressWeekLookup=true
        remoteScheduleLoadedWeek=savedWeekStart; weekLoadNonce++
        weekStart=savedWeekStart; bible=meta.optString("reading",remote.reading)
        openingSong=meta.optString("openingSong",openingSong); closingSong=meta.optString("closingSong",closingSong)
        workbook=row.optString("sourceUrl",workbook)
        chairman=meta.optString("chairman",""); openingPrayer=meta.optString("openingPrayer",""); adviser=meta.optString("adviser",""); audienceGroup=meta.optString("audienceGroup","")
        store.setDetail("chairman",chairman); store.setDetail("opening_prayer",openingPrayer); store.setDetail("adviser",adviser); store.setDetail("audience_group",audienceGroup)
        parts.clear()
        val loadedParts = (0 until raw.length()).mapNotNull { raw.optJSONObject(it)?.toAssignment() }
        parts.addAll(deduplicateAssignmentParticipants(loadedParts))
        originalCloudAssignments = scheduleAssignmentValues(parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
        originalCloudUpdatedAt = row.optString("updatedAt")
        editingRemoteId=remote.id; persistDetails(); store.saveAssignments(parts.toList())
        workbookStatus="✓ Na-load ang saved cloud schedule"
        if(navigate) current=if(edit) Screen.DETAILS else Screen.PREVIEW
    }
    LaunchedEffect(weekStart, signedIn, weekLoadNonce, identityToolAccess) {
        if (suppressWeekLookup) { suppressWeekLookup=false; return@LaunchedEffect }
        if (remoteScheduleLoadedWeek==weekStart) return@LaunchedEffect
        if (signedIn) {
            workbookBusy = true
            workbookStatus = "Sinusuri kung may saved schedule sa napiling petsa…"
            workbookCaptureLog = emptyList()
            val selectedDate=runCatching { LocalDate.parse(weekStart) }.getOrDefault(week)
            val saved=runCatching { api.schedules().firstOrNull { scheduleStartDate(it.weekLabel,it.updatedAt)==selectedDate } }.getOrNull()
            if(saved!=null) runCatching { applyRemoteSchedule(saved) }.onFailure { workbookStatus="Hindi ma-load ang saved schedule: ${it.message ?: "server error"}"; workbookBusy=false }
            else { clearMeetingRoles(); fetchWorkbookForSelectedWeek() }
            workbookBusy=false
        }
    }
    fun saveDraft() {
        persistDetails()
        store.saveAssignments(parts.toList())
        store.saveParticipants(participants.toList())
        toastMessage = "Na-save ang draft sa device"
    }
    fun printSchedule() {
        val html = buildOfficialScheduleHtml(context, weekStart, bible, openingSong, closingSong, parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
        val web = WebView(context)
        web.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean = true
            override fun onPageFinished(view: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print("Meeting schedule", view.createPrintDocumentAdapter("Meeting schedule"), android.print.PrintAttributes.Builder().build())
            }
        }
        web.loadDataWithBaseURL("file:///android_asset/", html, "text/HTML", "UTF-8", null)
    }
    fun shareAssignmentSlipsImages() {
        if (exportBusyType != null) return
        val date = runCatching { LocalDate.parse(weekStart).plusDays(3) }.getOrNull() ?: return
        val slips = buildAssignmentSlips(date, parts, chairman, openingPrayer, adviser)
        if (slips.isEmpty()) { toastMessage = "Magtalaga muna ng participant para sa assignment slips."; return }
        val exportWeek = weekStart
        val exportLabel = weekLabel
        exportBusyType = "slips_image"
        authScope.launch {
            runCatching { assignmentSlipsShareIntent(context, slips, exportWeek, exportLabel) }
                .onSuccess { intent -> launchScheduleShare(intent, "slips_image", "Share assignment slips images") }
                .onFailure { exportBusyType = null; toastMessage = "Hindi mabuo ang assignment slip images: ${it.message ?: "subukan muli"}" }
        }
    }
    fun shareSchedulePng() {
        if (exportBusyType != null) return
        exportBusyType = "png"
        renderSchedulePng(
            context,
            buildOfficialScheduleHtml(context, weekStart, bible, openingSong, closingSong, parts.toList(),
                chairman, openingPrayer, adviser, audienceGroup, exportMode = true),
            onSuccess = { intent -> launchScheduleShare(intent, "png", "Share schedule image") },
            onFailure = { error -> exportBusyType = null; toastMessage = error }
        )
    }
    fun openSaved(item: SavedSchedule) {
        editingRemoteId = 0; originalCloudAssignments = emptyList(); originalCloudUpdatedAt = ""
        if (item.weekStart.isNotBlank()) weekStart = item.weekStart
        bible = item.reading; openingSong = item.openingSong; closingSong = item.closingSong
        chairman=item.chairman; openingPrayer=item.openingPrayer; adviser=item.adviser; audienceGroup=item.audienceGroup
        if (item.assignments.isNotEmpty()) { val unique=deduplicateAssignmentParticipants(item.assignments); parts.clear(); parts.addAll(unique); store.saveAssignments(unique) }
        persistDetails(); current = Screen.DETAILS; toastMessage = "Na-open ang saved schedule"
    }
    fun duplicateSaved(item: SavedSchedule) {
        editingRemoteId = 0; originalCloudAssignments = emptyList(); originalCloudUpdatedAt = ""
        val sourceDate = runCatching { LocalDate.parse(item.weekStart) }.getOrDefault(week.plusDays(-7))
        weekStart = sourceDate.plusWeeks(1).toString()
        bible = item.reading; openingSong = item.openingSong; closingSong = item.closingSong
        chairman=item.chairman; openingPrayer=item.openingPrayer; adviser=item.adviser; audienceGroup=item.audienceGroup
        if (item.assignments.isNotEmpty()) { val unique=deduplicateAssignmentParticipants(item.assignments); parts.clear(); parts.addAll(unique); store.saveAssignments(unique) }
        persistDetails(); current = Screen.DETAILS; toastMessage = "Naduplicate para sa susunod na linggo"
    }
    fun loadRemoteSchedule(remote: RemoteSchedule, edit: Boolean) { authScope.launch {
        runCatching { applyRemoteSchedule(remote,navigate=true,edit=edit) }.onFailure { toastMessage=it.message ?: "Hindi mabuksan ang schedule" }
    } }

    fun closeHomePreview() {
        homePreviewRequest++
        homePreviewWeek = null
        homePreviewHtml = null
        homePreviewLoading = false
    }

    fun openUpcomingPreview(remote: RemoteSchedule) {
        val request = ++homePreviewRequest
        homePreviewWeek = remote.weekLabel
        homePreviewHtml = null
        homePreviewLoading = true
        authScope.launch {
            runCatching {
                val saved = api.schedule(remote.id)
                if (request != homePreviewRequest) return@launch
                val meta = saved.optJSONObject("meta") ?: JSONObject()
                homePreviewWeek = meta.optString("date").ifBlank { remote.weekLabel.ifBlank { "Schedule preview" } }
                val rawParts = saved.optJSONArray("parts") ?: JSONArray()
                val assignments = (0 until rawParts.length()).mapNotNull { rawParts.optJSONObject(it)?.toAssignment() }
                val start = meta.optString("weekStart").ifBlank { scheduleStartDate(meta.optString("date").ifBlank { remote.weekLabel }, remote.updatedAt)?.toString() ?: LocalDate.now().toString() }
                buildOfficialScheduleHtml(
                    context, start,
                    meta.optString("reading", remote.reading),
                    meta.optString("openingSong", "27"),
                    meta.optString("closingSong", "132"),
                    assignments,
                    meta.optString("chairman"),
                    meta.optString("openingPrayer"),
                    meta.optString("adviser"),
                    meta.optString("audienceGroup"),
                    exportMode = true
                )
            }.onSuccess { if (request == homePreviewRequest) homePreviewHtml = it }
                .onFailure { if (request == homePreviewRequest) {
                    toastMessage = it.message ?: "Could not open the schedule preview."
                    closeHomePreview()
                } }
            if (request == homePreviewRequest) homePreviewLoading = false
        }
    }

    val notificationActivity = context as? MainActivity
    val notificationScheduleId = notificationActivity?.pendingSchedulePreviewId ?: 0
    LaunchedEffect(notificationScheduleId, signedIn) {
        if (signedIn && notificationScheduleId > 0) {
            notificationActivity?.consumeSchedulePreview(notificationScheduleId)
            openUpcomingPreview(RemoteSchedule(notificationScheduleId, "Schedule preview", "", ""))
        }
    }

    val accountPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val account = accountPhotoTarget
        if (uri != null && account != null) authScope.launch {
            accountsBusy = true
            accountsError = null
            runCatching {
                val dataUrl = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1) / sample > 720) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Hindi mabasa ang napiling larawan.")
                    val scale = minOf(1f, 512f / maxOf(decoded.width, decoded.height).coerceAtLeast(1))
                    val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
                    val output = ByteArrayOutputStream()
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) { "Hindi ma-compress ang larawan." }
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    check(encoded.length <= 600_000) { "Masyadong malaki ang larawan. Pumili ng mas maliit." }
                    "data:image/jpeg;base64,$encoded"
                }
                api.uploadAccountPhoto(account.id, dataUrl)
            }.onSuccess {
                toastMessage = "Na-save sa cloud ang account photo."
                accountsLoaded = false
                accountReloadTick++
            }.onFailure { accountsError = it.message ?: "Hindi na-upload ang account photo." }
            accountsBusy = false
        }
        accountPhotoTarget = null
    }

    val availableScreens = Screen.entries.filter(::canAccess).toSet()
    fun signOut() {
        profileMenuOpen = false
        val oldSession = api.signOut()
        val pushToken = MeetingPushNotifications.savedToken(context)
        authScope.launch {
            if (!pushToken.isNullOrBlank()) runCatching { api.unregisterPushToken(pushToken, oldSession) }
            api.revokeSession(oldSession)
        }
        signedIn = false
        identityName = ""
        identityUsername = ""
        identityRole = ""
        loginError = null
        authReady = true
    }
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) authScope.launch {
            profileError = null
            val dataUrl = runCatching {
                withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1) / sample > 720) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Hindi mabasa ang napiling larawan.")
                    val scale = minOf(1f, 512f / maxOf(decoded.width, decoded.height).coerceAtLeast(1))
                    val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
                    val output = ByteArrayOutputStream()
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) { "Hindi ma-compress ang larawan." }
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    check(encoded.length <= 600_000) { "Masyadong malaki ang larawan. Pumili ng mas maliit." }
                    "data:image/jpeg;base64,$encoded"
                }
            }.getOrElse { error ->
                profileError = error.message ?: "Hindi maihanda ang larawan."
                null
            }
            if (dataUrl != null) runCatching { api.uploadOwnProfilePhoto(dataUrl) }
                .onSuccess {
                    profilePhotoDataUrl = dataUrl
                    profilePhotoPath = null
                    toastMessage = "Na-save sa cloud ang profile photo."
                    if (identityRole == "admin") { accountsLoaded = false; accountReloadTick++ }
                }
                .onFailure { profileError = it.message ?: "Hindi na-save sa cloud ang profile photo." }
        }
    }
    BackHandler(enabled = current != Screen.HOME && canAccess(Screen.HOME) && !profileMenuOpen && !toolsOpen) {
        navigateTo(Screen.HOME)
    }
    if (toolsOpen) {
        PreviewToolsSheet(availableScreens, current, onDismiss = { toolsOpen = false }) {
            toolsOpen = false
            navigateTo(it)
        }
    }
    LaunchedEffect(current, signedIn, identityRole, accountReloadTick) {
        if (current == Screen.ACCOUNTS && signedIn && identityRole.equals("admin", true) && !accountsLoaded) {
            accountsBusy = true
            accountsError = null
            try {
                val data = api.accountsWithParticipants()
                remoteAccounts = data.first
                accountLinkParticipants = data.second
                accountsLoaded = true
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                accountsError = error.message ?: "Hindi makuha ang accounts at participants."
            } finally {
                accountsBusy = false
            }
        }
    }

    if (workspacePickerOpen) {
        val localName = remember(context) {
            val prefs = context.getSharedPreferences("meeting_scheduler_device_only", Context.MODE_PRIVATE)
            prefs.getString("workspace", null)?.let { raw ->
                runCatching { JSONObject(raw).optString("name").takeIf { it.isNotBlank() } }.getOrNull()
            }
        }
        WorkspacePickerDialog(
            localActive = false,
            localName = localName,
            onDismiss = { workspacePickerOpen = false },
            canSwitch = exportBusyType == null,
            onSwitch = { workspacePickerOpen = false; onOpenLocal() }
        )
    }

    Scaffold(
        containerColor = AppSurface,
        topBar = {},
        bottomBar = {
            PreviewBottomNavigation(
                current = current,
                available = availableScreens,
                onNavigate = ::navigateTo,
                onCenterClick = { profileMenuOpen = false; toolsOpen = true },
                profileName = identityName,
                profileRole = identityRole,
                profileAvatar = { size -> ProfileAvatar(identityName, profilePhotoPath, size, profilePhotoDataUrl) },
                profileMenuOpen = profileMenuOpen,
                onProfileMenuToggle = { toolsOpen = false; profileMenuOpen = !profileMenuOpen },
                onProfileMenuDismiss = { profileMenuOpen = false },
                onProfileSettings = { profileMenuOpen = false; navigateTo(Screen.ACCOUNTS) },
                onProfileSignOut = ::signOut,
                onProfileWorkspace = { profileMenuOpen = false; workspacePickerOpen = true }
            )
        },
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { insets ->
        val scroll = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 18 }) togetherWith
                    (fadeOut(tween(120)) +
                        slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 18 })
            },
            label = "screen transition"
        ) { destination -> when (destination) {
            Screen.HOME -> HomeScreen(
                modifier = scroll,
                accountName = identityName,
                canCreateSchedule = canEdit(Screen.DETAILS),
                schedules = homeSchedules.map { it.workspaceEntry() },
                loading = homeBusy,
                error = homeError,
                onRefresh = { homeLoaded = false },
                onOpenSchedule = ::openUpcomingPreview,
                onCreateSchedule = { navigateTo(Screen.DETAILS) }
            )
            Screen.DETAILS -> DetailsScreen(
                modifier = scroll, weekLabel = weekLabel, bible = bible, onBible = { bible = it; persistDetails() },
                openingSong = openingSong, onOpeningSong = { openingSong = it; persistDetails() }, closingSong = closingSong,
                onClosingSong = { closingSong = it; persistDetails() },
                onPickDate = { datePicker = true }, onImport = { authScope.launch { fetchWorkbookForSelectedWeek() } },
                workbookStatus = workbookStatus,
                workbookBusy = workbookBusy, workbookCaptureLog = workbookCaptureLog,
                canContinue = !workbookBusy && participantsLoaded && !participantsBusy,
                participantsLoadFailed = participantsLoadError != null,
                onRetryParticipants = { participantsLoadError = null; participantsLoaded = false },
                canContinueToAssignments = canAccess(Screen.ASSIGNMENTS),
                readOnly = !canEdit(Screen.DETAILS),
                isCurrentWeek = weekStart == currentWorkbookWeekStart.toString(),
                onThisWeek = {
                    remoteScheduleLoadedWeek = ""
                    editingRemoteId = 0
                    originalCloudAssignments = emptyList()
                    originalCloudUpdatedAt = ""
                    suppressWeekLookup = false
                    weekStart = currentWorkbookWeekStart.toString()
                    persistDetails()
                },
                onNext = { navigateTo(Screen.ASSIGNMENTS) }
            )
            Screen.ASSIGNMENTS -> AssignmentsScreen(
                modifier = scroll, parts = parts, participants = participants, rotation = rotationStats,
                rotationBusy = rotationBusy, rotationError = rotationError,
                chairman=chairman, openingPrayer=openingPrayer, adviser=adviser, audienceGroup=audienceGroup,
                onRole = { role, value ->
                    if (role != "audience_group" && value.isNotBlank()) {
                        parts.indices.forEach { index -> parts[index] = parts[index].withoutParticipant(value) }
                        store.saveAssignments(parts.toList())
                        if (role != "chairman" && chairman.equals(value, true)) { chairman = ""; store.setDetail("chairman", "") }
                        if (role != "opening_prayer" && openingPrayer.equals(value, true)) { openingPrayer = ""; store.setDetail("opening_prayer", "") }
                        if (role != "adviser" && adviser.equals(value, true)) { adviser = ""; store.setDetail("adviser", "") }
                    }
                    when (role) {
                        "chairman" -> chairman = value
                        "opening_prayer" -> openingPrayer = value
                        "adviser" -> adviser = value
                        else -> audienceGroup = value
                    }
                    store.setDetail(role, value)
                },
                isAdmin = signedIn && identityRole == "admin", readOnly = !canEdit(Screen.ASSIGNMENTS), canContinueExport = canAccess(Screen.PREVIEW),
                onAssign = { index, slot, name ->
                    if (name.isNotBlank()) {
                        parts.indices.forEach { i -> parts[i] = parts[i].withoutParticipant(name) }
                        if (chairman.equals(name, true)) { chairman = ""; store.setDetail("chairman", "") }
                        if (openingPrayer.equals(name, true)) { openingPrayer = ""; store.setDetail("opening_prayer", "") }
                        if (adviser.equals(name, true)) { adviser = ""; store.setDetail("adviser", "") }
                    }
                    assignParticipantToOneSlot(parts, index, slot, name)
                    store.saveAssignments(parts.toList())
                },
                cloudSaveBusy = cloudSaveBusy, cloudSaveFeedback = cloudSaveFeedback,
                onSaveCloud = ::saveToCloud, onContinueExport = { navigateTo(Screen.PREVIEW) }
            )
            Screen.PREVIEW -> PreviewScreen(
                modifier = scroll, weekStart = weekStart, bible = bible, openingSong = openingSong,
                closingSong = closingSong, parts = parts, chairman=chairman, openingPrayer=openingPrayer,
                adviser=adviser, audienceGroup=audienceGroup, previewWebView=officialPreviewWebView, allowExport=canEdit(Screen.PREVIEW),
                exportBusyType = exportBusyType,
                onExportText = {
                    if (exportBusyType == null) {
                        exportBusyType = "text"
                        val text = buildOfficialScheduleText(weekStart, bible, openingSong, closingSong, parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
                        val exportWeekStart = weekStart
                        authScope.launch {
                            runCatching {
                                scheduleTextShareIntent(context, text, exportWeekStart, weekLabel)
                            }.onSuccess { intent -> launchScheduleShare(intent, "text", "Share schedule text") }
                                .onFailure { exportBusyType = null; toastMessage = "Hindi mabuo ang export text: ${it.message ?: "subukan muli"}" }
                        }
                    }
                }, onPng = ::shareSchedulePng, onAssignmentSlipImages = ::shareAssignmentSlipsImages
            )
            Screen.PARTICIPANTS -> {
                ParticipantsScreen(modifier = scroll, participants = participants, loading = participantsBusy, loadError = participantsLoadError,
                    onAdd = { name -> participants.add(0, Participant(name)); store.saveParticipants(participants.toList()); toastMessage = "Naidagdag si $name" },
                    onToggle = { index -> participants[index] = participants[index].copy(enabled = !participants[index].enabled); store.saveParticipants(participants.toList()) },
                    onEdit = { original, changed -> authScope.launch {
                        runCatching { if (changed.id > 0 && signedIn) api.updateMember(changed.id, changed.name, changed.appointment, changed.service, changed.gender, changed.unbaptized) else JSONObject() }
                            .onSuccess { response ->
                                if (changed.unbaptized && !response.optBoolean("unbaptized",false)) { toastMessage = "Hindi pa updated ang website API. I-deploy muna ang backend update." }
                                else participants.indexOfFirst { it.id == original.id && it.name == original.name }.takeIf { it >= 0 }?.let { index -> participants[index] = changed; store.saveParticipants(participants.toList()); toastMessage = "Na-update si ${changed.name}" }
                            }
                            .onFailure { toastMessage = it.message ?: "Hindi na-update ang participant" }
                    } },
                    isAdmin=participantAdmin,
                    canEdit=canEdit(Screen.PARTICIPANTS) && participantCanEdit,
                    onRefresh={participantsLoadError=null;participantsLoaded=false},
                    onSetActive={ person,active -> authScope.launch {
                        runCatching { api.setMemberActive(person.id,active) }.onSuccess {
                            val i=participants.indexOfFirst { it.id==person.id }; if(i>=0){participants[i]=participants[i].copy(enabled=active);store.saveParticipants(participants.toList())};toastMessage=if(active)"Na-activate si ${person.name}" else "Ginawang inactive si ${person.name}"
                        }.onFailure{toastMessage=it.message?:"Hindi nabago ang status"}
                    } },
                    onDelete={ person -> authScope.launch {
                        runCatching { api.deleteMember(person.id) }.onSuccess { response -> if(response.optBoolean("deleted",false)){participants.removeAll { it.id==person.id };store.saveParticipants(participants.toList());toastMessage="Na-delete si ${person.name}"}else toastMessage="Hindi pa updated ang website API para sa permanent delete." }
                            .onFailure{toastMessage=it.message?:"Hindi na-delete ang participant"}
                    } })
            }
            Screen.HISTORY -> {
                LaunchedEffect(current, signedIn, historyReloadTick) {
                    if (current == Screen.HISTORY && signedIn && !historyLoaded && !historyBusy) {
                        historyBusy = true
                        historyError = null
                        runCatching { api.schedulePage() }
                            .onSuccess { page -> remoteRows = page.schedules; historyNextOffset = page.nextOffset; historyHasMore = page.hasMore; availableHistoryYears = page.years; historyLoaded = true; cloudStatus = "History mula sa website" }
                            .onFailure { historyError = it.message ?: "Suriin ang internet at subukan ulit."; cloudStatus = "Hindi makuha ang history: ${it.message}" }
                        historyBusy = false
                    }
                }
                HistoryScreen(modifier = scroll, schedules = remoteRows.map { it.workspaceEntry() }, loading = historyBusy, error = historyError,
                    hasMore = historyHasMore, loadingMore = historyLoadingMore, availableYears = availableHistoryYears,
                    onLoadMore = {
                        if (!historyLoadingMore && historyHasMore) authScope.launch {
                            historyLoadingMore = true
                            runCatching { api.schedulePage(historyNextOffset) }
                                .onSuccess { page ->
                                    val knownWeeks = remoteRows.map { it.weekLabel.trim().lowercase(Locale.ROOT) }.toMutableSet()
                                    remoteRows = remoteRows + page.schedules.filter { knownWeeks.add(it.weekLabel.trim().lowercase(Locale.ROOT)) }
                                    historyNextOffset = page.nextOffset
                                    historyHasMore = page.hasMore
                                    availableHistoryYears = page.years
                                }
                                .onFailure { historyError = it.message ?: "Could not load more schedules." }
                            historyLoadingMore = false
                        }
                    },
                    isAdmin=signedIn && identityRole=="admin", canEdit=canEdit(Screen.HISTORY), canCreate=canEdit(Screen.DETAILS),
                    onRefresh={historyLoaded=false;historyReloadTick++}, onView={loadRemoteSchedule(it,false)}, onEdit={loadRemoteSchedule(it,true)},
                    onDelete={remote -> authScope.launch { runCatching { api.deleteSchedule(remote.id) }.onSuccess { historyLoaded=false;historyReloadTick++;toastMessage="Na-delete ang schedule" }.onFailure { toastMessage=it.message ?: "Hindi ma-delete" } }},
        onNew = { editingRemoteId=0; originalCloudAssignments=emptyList(); originalCloudUpdatedAt=""; navigateTo(Screen.DETAILS) })
            }
            Screen.ACCOUNTS -> {
                AccountsScreen(
                    modifier=scroll, accounts=remoteAccounts, linkParticipants=accountLinkParticipants, loading=accountsBusy, error=accountsError,
                    testNotificationBusy=testNotificationBusy, testNotificationFeedback=testNotificationFeedback,
                    profileName=identityName, profileUsername=identityUsername, profileRole=identityRole,
                    profilePhotoPath=profilePhotoPath, profilePhotoDataUrl=profilePhotoDataUrl, profileError=profileError,
                    onChangeOwnPhoto={ profileError = null; profilePhotoPicker.launch(arrayOf("image/*")) },
                    onSaveOwnName={ value ->
                        val cleanName = value.trim()
                        if (cleanName.isNotEmpty()) {
                            identityName = cleanName
                            store.saveProfileName(identityUsername, cleanName)
                        }
                    },
                    onTestNotification={
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            testNotificationFeedback = NotificationFeedback(false, "Allow notifications, then tap Send test notification again.")
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            testNotificationFeedback = NotificationFeedback(false, "Turn on notifications in Android Settings, then try again.")
                        } else if (!testNotificationBusy) authScope.launch {
                            testNotificationBusy = true
                            testNotificationFeedback = null
                            try {
                                val token = MeetingPushNotifications.savedToken(context)
                                    ?: throw IllegalStateException("Notifications are still being set up. Try again in a few seconds.")
                                api.registerPushToken(token)
                                val sent = api.sendTestNotification()
                                testNotificationFeedback = if (sent > 0) NotificationFeedback(true, "Test notification sent to this account.") else NotificationFeedback(false, "No test notification was sent. Check app notifications and try again.")
                            } catch (error: Exception) {
                                testNotificationFeedback = NotificationFeedback(false, error.message ?: "Could not send the test notification. Try again.")
                            } finally {
                                testNotificationBusy = false
                            }
                        }
                    },
                    currentUsername=identityUsername, isAdmin=identityRole.equals("admin", true),
                    fontScale=fontScale, onFontScaleChange=onFontScaleChange,
                    themeMode=themeMode, onThemeModeChange=onThemeModeChange, paletteStyle=paletteStyle, onPaletteStyleChange=onPaletteStyleChange, appLock=appLock,
                    onRefresh={accountsLoaded=false;accountReloadTick++},
                    onUploadPhoto={ account -> accountPhotoTarget=account; accountPhotoPicker.launch("image/*") },
                    onLinkParticipant={ account, memberId -> authScope.launch {
                        accountsBusy=true; accountsError=null
                        runCatching { api.linkAccountParticipant(account.id, memberId) }
                            .onSuccess { response -> toastMessage=response.optString("message","Participant link updated."); accountsLoaded=false; accountReloadTick++ }
                            .onFailure { accountsError=it.message ?: "Hindi na-link ang participant." }
                        accountsBusy=false
                    } },
                    onAction={ account,action,role,password -> authScope.launch {
                        accountsBusy=true;accountsError=null
                        runCatching { api.accountAction(account.id,action,role,password) }
                            .onSuccess { response -> toastMessage=response.optString("message","Account updated.");accountsLoaded=false;accountReloadTick++ }
                            .onFailure { accountsError=it.message ?: "Hindi na-update ang account." }
                        accountsBusy=false
                    } },
                    onUpdateToolAccess={ account, modes -> authScope.launch {
                        accountsBusy=true; accountsError=null
                        runCatching { api.updateAccountToolAccess(account.id, modes) }
                            .onSuccess { response -> toastMessage=response.optString("message","Tool access updated."); accountsLoaded=false; accountReloadTick++ }
                            .onFailure { accountsError=it.message ?: "Hindi na-save ang tool access." }
                        accountsBusy=false
                    } }
                )
            }
        } }
    }
    if (cloudSaveReviewVisible) {
        val changes = summarizeScheduleChanges(
            originalCloudAssignments,
            scheduleAssignmentValues(parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
        )
        AlertDialog(
            onDismissRequest = { cloudSaveReviewVisible = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Review schedule changes", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.55f).dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(weekLabel, color = Forest, fontWeight = FontWeight.Medium)
                    Text(
                        if (editingRemoteId > 0) "Check what changed from the saved cloud schedule." else "Review the assignments that will be added to this new schedule.",
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (changes.isEmpty()) {
                        Text("No assignment changes detected. The current schedule will be saved as-is.", color = Muted, fontSize = 13.sp)
                    } else {
                        listOf(
                            ScheduleChangeKind.ADDED to "Added",
                            ScheduleChangeKind.REPLACED to "Changed",
                            ScheduleChangeKind.REMOVED to "Removed"
                        ).forEach { (kind, heading) ->
                            val grouped = changes.filter { it.kind == kind }
                            if (grouped.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("$heading · ${grouped.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = when (kind) {
                                        ScheduleChangeKind.ADDED -> Forest
                                        ScheduleChangeKind.REPLACED -> MaterialTheme.colorScheme.primary
                                        ScheduleChangeKind.REMOVED -> MaterialTheme.colorScheme.error
                                    })
                                    grouped.forEach { change ->
                                        Column(
                                            Modifier.fillMaxWidth().padding(start = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            Text(
                                                when (kind) {
                                                    ScheduleChangeKind.ADDED -> change.currentPerson
                                                    ScheduleChangeKind.REPLACED -> "${change.previousPerson} → ${change.currentPerson}"
                                                    ScheduleChangeKind.REMOVED -> change.previousPerson
                                                },
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(change.label, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text("This review covers assignment changes. Group remains optional.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            },
            confirmButton = {
                Button(onClick = ::confirmSaveToCloud, colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                    Text("Confirm save")
                }
            },
            dismissButton = {
                TextButton(onClick = { cloudSaveReviewVisible = false }) { Text("Back to assignments", color = Forest) }
            }
        )
    }
    if (cloudConflictVisible) {
        AlertDialog(
            onDismissRequest = { cloudConflictVisible = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Cloud schedule changed", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Another admin updated or removed this schedule after you opened it. Your edits are still on this screen, and they were not saved. Reload the cloud version before making further changes.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = RemoteSchedule(editingRemoteId, weekLabel, bible, originalCloudUpdatedAt, weekStart)
                        authScope.launch {
                            runCatching { applyRemoteSchedule(target, navigate = false, edit = true) }
                                .onSuccess {
                                    cloudConflictVisible = false
                                    historyLoaded = false
                                    toastMessage = "Loaded the latest cloud schedule. Review it before saving."
                                }
                                .onFailure { toastMessage = it.message ?: "Could not reload the cloud schedule." }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest)
                ) { Text("Reload latest") }
            },
            dismissButton = {
                TextButton(onClick = { cloudConflictVisible = false }) { Text("Keep editing", color = Forest) }
            }
        )
    }
    if (homePreviewWeek != null) {
        val pageAspect = SCHEDULE_EXPORT_WIDTH_PX.toFloat() / SCHEDULE_EXPORT_HEIGHT_PX
        val previewView = remember { mutableStateOf<WebView?>(null) }
        Dialog(
            onDismissRequest = ::closeHomePreview,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val pageWidth = minOf(
                (configuration.screenWidthDp.dp - 56.dp).coerceAtLeast(1.dp),
                configuration.screenHeightDp.dp * 0.72f * pageAspect,
                580.dp
            )
            Surface(
                modifier = Modifier.width(pageWidth + 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(homePreviewWeek.orEmpty(), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = ::closeHomePreview) {
                            Icon(SproutIcons.Close, contentDescription = "Close preview")
                        }
                    }
                    Surface(
                        color = Color.White,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.width(pageWidth).aspectRatio(pageAspect)
                    ) {
                        val html = homePreviewHtml
                        if (homePreviewLoading || html == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Forest)
                            }
                        } else {
                            OfficialScheduleWebPreview(html, previewView, Modifier.fillMaxSize()) {}
                        }
                    }
                }
            }
        }
    }
    if (datePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = week.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            remoteScheduleLoadedWeek = ""; editingRemoteId = 0; originalCloudAssignments = emptyList(); originalCloudUpdatedAt = ""
                            val selectedWeek = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
                            weekStart = selectedWeek
                            store.saveDetails(selectedWeek, bible, openingSong, closingSong, workbook)
                        }
                        datePicker = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Forest,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(
                    onClick = { datePicker = false },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.textButtonColors(containerColor = ForestContainer, contentColor = Forest)
                ) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(containerColor = AppSurface)
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = AppSurface,
                    titleContentColor = Ink,
                    headlineContentColor = Ink,
                    weekdayContentColor = Muted,
                    subheadContentColor = Muted,
                    yearContentColor = Ink,
                    currentYearContentColor = Forest,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = Forest,
                    dayContentColor = Ink,
                    disabledDayContentColor = Muted.copy(alpha = 0.38f),
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = Forest,
                    todayContentColor = Forest,
                    todayDateBorderColor = Forest,
                    dayInSelectionRangeContentColor = Ink,
                    dayInSelectionRangeContainerColor = ForestContainer
                )
            )
        }
    }
    toastMessage?.let { msg -> LaunchedEffect(msg) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); toastMessage = null } }
    shareSuccessType?.let { type ->
        AlertDialog(
            onDismissRequest = { shareSuccessType = null },
            icon = { Icon(SproutIcons.CheckCircle, contentDescription = null, tint = Forest, modifier = Modifier.size(28.dp)) },
            title = { Text("Matagumpay ang pag-share", fontWeight = FontWeight.Bold) },
            text = { Text(when (type) { "png" -> "Naibahagi ang schedule PNG."; "slips_image" -> "Naibahagi ang assignment slip images."; else -> "Naibahagi ang schedule text." }) },
            confirmButton = { TextButton(onClick = { shareSuccessType = null }) { Text("Okay", color = Forest) } },
            containerColor = AppSurface,
            titleContentColor = Ink,
            textContentColor = Ink
        )
    }
    accessNotice?.let { (message, warning) ->
        LaunchedEffect(accessNotice) { delay(2_100); accessNotice = null }
        Dialog(onDismissRequest = { accessNotice = null }) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Icon(if (warning) SproutIcons.Warning else SproutIcons.ErrorOutline, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(30.dp))
                    Text(message, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun MeetingSplashScreen() {
    val colors = MaterialTheme.colorScheme
    val compact = LocalConfiguration.current.screenHeightDp < 440
    var reveal by remember { mutableStateOf(false) }
    val entrance = remember { Animatable(0f) }
    val orbit = rememberInfiniteTransition(label = "splash orbit")
    val orbitAngle by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Restart),
        label = "splash orbit angle"
    )
    LaunchedEffect(Unit) {
        reveal = true
        entrance.animateTo(1f, tween(880, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(colors.background, colors.surfaceContainerLow, colors.background)
        )),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(if (compact) 176.dp else 226.dp),
                contentAlignment = Alignment.Center
            ) {
                ComposeCanvas(
                    Modifier.fillMaxSize().graphicsLayer { rotationZ = orbitAngle }
                ) {
                    val inset = size.minDimension * 0.075f
                    val ringSize = androidx.compose.ui.geometry.Size(
                        size.width - inset * 2f,
                        size.height - inset * 2f
                    )
                    drawCircle(colors.primaryContainer.copy(alpha = 0.22f), radius = size.minDimension * 0.45f)
                    drawArc(
                        color = colors.primary.copy(alpha = 0.9f),
                        startAngle = -108f, sweepAngle = 126f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = ringSize,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = colors.secondary.copy(alpha = 0.48f),
                        startAngle = 78f, sweepAngle = 78f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = ringSize,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val radians = Math.toRadians(orbitAngle.toDouble())
                    val orbitRadius = size.minDimension * 0.45f
                    drawCircle(
                        color = colors.tertiary,
                        radius = 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width / 2f + kotlin.math.cos(radians).toFloat() * orbitRadius,
                            size.height / 2f + kotlin.math.sin(radians).toFloat() * orbitRadius
                        )
                    )
                }
                Surface(
                    modifier = Modifier.size(if (compact) 112.dp else 138.dp).graphicsLayer {
                        val scale = 0.76f + 0.24f * entrance.value
                        scaleX = scale
                        scaleY = scale
                        rotationY = (1f - entrance.value) * -22f
                        translationY = (1f - entrance.value) * 18.dp.toPx()
                    },
                    shape = RoundedCornerShape(if (compact) 30.dp else 38.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                    shadowElevation = 18.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MeetingSchedulerBrandIcon(Modifier.size(if (compact) 82.dp else 100.dp), "JW Meeting Manager")
                    }
                }
                if (reveal) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-23).dp)
                            .graphicsLayer { scaleX = entrance.value; scaleY = entrance.value; alpha = entrance.value },
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        border = BorderStroke(3.dp, colors.primary),
                        shadowElevation = 6.dp
                    ) {
                        Box(Modifier.size(if (compact) 34.dp else 40.dp), contentAlignment = Alignment.Center) {
                            Icon(SproutIcons.CheckCircle, contentDescription = null,
                                tint = colors.onPrimaryContainer, modifier = Modifier.size(if (compact) 22.dp else 26.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 10.dp else 18.dp))
            AnimatedVisibility(
                visible = reveal,
                enter = fadeIn(tween(380, delayMillis = 170)) +
                    slideInVertically(tween(440, delayMillis = 170)) { it / 4 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "JW Meeting Manager",
                        color = colors.onSurface,
                        fontSize = if (compact) 20.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Aayos ang bawat bahagi ng pulong",
                        color = colors.onSurfaceVariant,
                        fontSize = if (compact) 11.sp else 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(if (compact) 18.dp else 26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        Modifier.size(if (index == 1) 14.dp else 5.dp, 5.dp)
                            .clip(CircleShape)
                            .background(if (index == 1) colors.primary else colors.primary.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    error: String?,
    mustChangePassword: Boolean,
    onLogin: (String, String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onRequestAccess: suspend (String, String, String) -> Unit,
    onOpenLocal: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var changePasswordOpen by rememberSaveable { mutableStateOf(false) }
    var currentTemporaryPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmNewPassword by rememberSaveable { mutableStateOf("") }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mustChangePassword) {
        if (mustChangePassword) {
            currentTemporaryPassword = password
            changePasswordError = null
            changePasswordOpen = true
        } else changePasswordOpen = false
    }
    var requestOpen by remember { mutableStateOf(false) }
    var requestSuccess by remember { mutableStateOf(false) }
    var requestBusy by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf<String?>(null) }
    var requestName by rememberSaveable { mutableStateOf("") }
    var requestUsername by rememberSaveable { mutableStateOf("") }
    var requestMessage by rememberSaveable { mutableStateOf("") }
    val requestScope = rememberCoroutineScope()
    LaunchedEffect(requestSuccess) {
        if (requestSuccess) {
            delay(1_900)
            requestSuccess = false
        }
    }
    Surface(color = AppSurface, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            val compact = maxHeight < 700.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = if (compact) 18.dp else 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MeetingSchedulerBrandIcon(Modifier.size(if (compact) 62.dp else 72.dp), "JW Meeting Manager")
                Spacer(Modifier.height(13.dp))
                Text("JW Meeting Manager", fontWeight = FontWeight.Bold, fontSize = 23.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("Midweek meeting workspace", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(if (compact) 22.dp else 30.dp))
                AnimatedVisibility(visible = true, enter = fadeIn(tween(520)) + slideInVertically(tween(520)) { it / 10 }) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(if (compact) 18.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            Text("ACCOUNT ACCESS", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                            Text("Welcome back", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Sign in with your account.", color = Muted, fontSize = 13.sp)
                            OutlinedTextField(
                                username,
                                { username = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(SproutIcons.Person, contentDescription = null, tint = Forest) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                password,
                                { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(SproutIcons.Lock, contentDescription = null, tint = Forest) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(if (showPassword) SproutIcons.VisibilityOff else SproutIcons.Visibility, contentDescription = if (showPassword) "Hide password" else "Show password", tint = Forest)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            error?.let {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                            Button(
                                onClick = { onLogin(username, password) },
                                enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(min = 50.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Forest)
                            ) {
                                Text(if (busy) "Signing in…" else "Sign in", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Icon(SproutIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
                            }
                            OutlinedButton(onClick = onOpenLocal, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("Open Local Workspace")
                            }
                            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Text("Don’t have an account?", color = Muted, fontSize = 12.sp)
                                TextButton(onClick = { requestError = null; requestOpen = true }) {
                                    Text("Request Access", color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (changePasswordOpen) {
        AlertDialog(
            onDismissRequest = { if (!busy && !mustChangePassword) changePasswordOpen = false },
            containerColor = AppSurface,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Set a new password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("You’re using a temporary password. Choose a new password to continue.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(currentTemporaryPassword, { currentTemporaryPassword = it; changePasswordError = null }, label = { Text("Temporary password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newPassword, { newPassword = it; changePasswordError = null }, label = { Text("New password") }, singleLine = true, visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showNewPassword = !showNewPassword }) { Icon(if (showNewPassword) SproutIcons.VisibilityOff else SproutIcons.Visibility, contentDescription = "Toggle password visibility", tint = Forest) } }, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(confirmNewPassword, { confirmNewPassword = it; changePasswordError = null }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    (changePasswordError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    if (newPassword.isNotBlank() && newPassword.length < 10) Text("Use at least 10 characters.", color = Muted, fontSize = 11.sp)
                    else if (confirmNewPassword.isNotBlank() && confirmNewPassword != newPassword) Text("The new passwords do not match.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && currentTemporaryPassword.isNotBlank() && newPassword.length >= 10 && confirmNewPassword == newPassword,
                    onClick = { changePasswordError = null; onChangePassword(currentTemporaryPassword, newPassword) }
                ) { if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Forest) else Text("Change Password", color = Forest) }
            },
            dismissButton = { if (!mustChangePassword) TextButton(onClick = { changePasswordOpen = false }) { Text("Cancel", color = Muted) } }
        )
    }
    if (requestOpen) {
        AlertDialog(
            onDismissRequest = { if (!requestBusy) requestOpen = false },
            containerColor = AppSurface,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Request Access", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Send your name, username, and a message to the administrator.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(requestName, { requestName = it; requestError = null }, label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(requestUsername, { requestUsername = it; requestError = null }, label = { Text("Username") }, singleLine = true, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(requestMessage, { requestMessage = it; requestError = null }, label = { Text("Message for Administrator") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    requestError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !requestBusy && requestName.trim().length >= 2 && requestUsername.trim().length >= 3,
                    onClick = {
                        requestScope.launch {
                            requestBusy = true
                            requestError = null
                            runCatching { onRequestAccess(requestName.trim(), requestUsername.trim(), requestMessage.trim()) }
                                .onSuccess { requestOpen = false; requestSuccess = true }
                                .onFailure { requestError = it.message ?: "Hindi naipadala ang request." }
                            requestBusy = false
                        }
                    }
                ) {
                    if (requestBusy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Forest)
                    else Text("Send Request", color = Forest)
                }
            },
            dismissButton = { TextButton(enabled = !requestBusy, onClick = { requestOpen = false }) { Text("Cancel", color = Muted) } }
        )
    }
    if (requestSuccess) {
        Dialog(onDismissRequest = { requestSuccess = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(SproutIcons.CheckCircle, contentDescription = "Success", tint = Forest, modifier = Modifier.size(28.dp))
                    Text("Request sent. Waiting for administrator approval.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

private val avatarCache = object : android.util.LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

@Composable
private fun rememberAvatarBitmap(dataUrl: String, photoPath: String? = null): ImageBitmap? {
    var bitmap by remember(dataUrl, photoPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(dataUrl, photoPath) {
        bitmap = withContext(Dispatchers.IO) {
            val cacheKey = dataUrl.takeIf { it.isNotBlank() }?.let {
                Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(it.toByteArray()), Base64.NO_WRAP)
            }
            if (cacheKey != null) avatarCache.get(cacheKey)?.let { return@withContext it }
            runCatching {
                if (dataUrl.length > 800_000) return@runCatching null
                val bytes = dataUrl.substringAfter("base64,", "").takeIf { it.isNotBlank() }
                    ?.let { Base64.decode(it, Base64.DEFAULT) }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                else if (photoPath != null) BitmapFactory.decodeFile(photoPath, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    else photoPath?.let { BitmapFactory.decodeFile(it, options) }
                decoded?.asImageBitmap()?.also { if (cacheKey != null) avatarCache.put(cacheKey, it) }
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun ProfileAvatar(name: String, photoPath: String?, size: androidx.compose.ui.unit.Dp, photoDataUrl: String = "") {
    val bitmap = rememberAvatarBitmap(photoDataUrl, photoPath)
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(Modifier.size(size).background(ForestContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").ifBlank { "?" }.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = (size.value * .34f).sp)
        }
    }
}

@Composable
private fun PageColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    PageFrame(modifier) { page ->
        Column(
            page.verticalScroll(rememberScrollState()).padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun WorkflowPage(
    modifier: Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    PageFrame(modifier) { page ->
        BoxWithConstraints(page) {
            // Keep the full page scrollable when a fixed footer would crowd out its content.
            val pinActions = actions != null && maxHeight >= 450.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                    if (!pinActions) actions?.invoke(this)
                }
                if (pinActions && actions != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        content = actions
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        trailing?.invoke()
    }
}

@Composable
private fun M3Card(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Muted, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun rememberMascotAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val setting = Settings.Global.ANIMATOR_DURATION_SCALE
    var enabled by remember(resolver) { mutableStateOf(Settings.Global.getFloat(resolver, setting, 1f) > 0f) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = Settings.Global.getFloat(resolver, setting, 1f) > 0f
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(setting), false, observer)
        observer.onChange(false)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private data class WorkspaceScheduleEntry<T>(
    val source: T,
    val key: String,
    val weekLabel: String,
    val reading: String,
    val weekStart: LocalDate?,
    val updatedAt: String,
    val assignedNames: List<String>
)

private fun RemoteSchedule.workspaceEntry() = WorkspaceScheduleEntry(
    this, id.toString(), weekLabel, reading, remoteScheduleStartDate(this), updatedAt, assignedNames
)

private fun SavedSchedule.workspaceEntry() = WorkspaceScheduleEntry(
    this, weekStart, week, reading, LocalDate.parse(weekStart), saved,
    scheduleAssignmentValues(assignments, chairman, openingPrayer, adviser, audienceGroup)
        .map { it.person }.filter { it.isNotBlank() }
)

@Composable
private fun <T> HomeScreen(
    modifier: Modifier,
    accountName: String,
    canCreateSchedule: Boolean,
    schedules: List<WorkspaceScheduleEntry<T>>,
    loading: Boolean,
    error: String?,
    onRefresh: (() -> Unit)?,
    onOpenSchedule: (T) -> Unit,
    onCreateSchedule: () -> Unit,
    dayOffset: Long = 3,
    workspaceLabel: String? = null,
    emptyMessage: String = "Create a workbook and save the schedule to the cloud to see it here.",
    draftCard: @Composable () -> Unit = {}
) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    val today = LocalDate.now()
    val upcoming = remember(schedules, today, dayOffset) {
        schedules.mapNotNull { remote ->
            val weekStart = remote.weekStart ?: return@mapNotNull null
            val meetingDate = weekStart.plusDays(dayOffset)
            if (meetingDate.isBefore(today)) null else meetingDate to remote
        }.sortedBy { it.first }
    }
    var mascotTipVisible by rememberSaveable { mutableStateOf(false) }
    var showAllMeetings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = mascotTipVisible) { mascotTipVisible = false }
    val animationsEnabled = rememberMascotAnimationsEnabled()
    val mascotBob = if (animationsEnabled) {
        val motion = rememberInfiniteTransition(label = "home mascot idle")
        val bob by motion.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "home mascot bob"
        )
        bob
    } else 0f
    val nextMeetingDate = upcoming.firstOrNull()?.first
    val mascotTip = nextMeetingDate?.let {
        "May susunod kang meeting sa ${it.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}. I-tap ang card para makita ang schedule."
    } ?: "Wala pang paparating na meeting. Lalabas dito ang susunod mong schedule."
    PageColumn(modifier) {
        Text(
            "JW Meeting Manager",
            modifier = Modifier.fillMaxWidth().offset(y = (-4).dp).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 35.sp),
            fontWeight = FontWeight.SemiBold
        )
        if (workspaceLabel != null) Text(workspaceLabel, color = Muted, style = MaterialTheme.typography.bodyMedium)
        val greetingShape = RoundedCornerShape(26.dp)
        val abstractAccent = MaterialTheme.colorScheme.onPrimaryContainer
        Card(shape = greetingShape, colors = CardDefaults.cardColors(containerColor = ForestContainer)) {
            Box(Modifier.fillMaxWidth().clip(greetingShape)) {
                ComposeCanvas(Modifier.matchParentSize()) {
                    val orbit = size.height * 1.35f
                    drawCircle(
                        color = abstractAccent.copy(alpha = 0.07f),
                        radius = size.height * 0.68f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.99f, size.height * 0.02f)
                    )
                    drawCircle(
                        color = abstractAccent.copy(alpha = 0.055f),
                        radius = size.height * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.79f, size.height * 0.94f)
                    )
                    drawArc(
                        color = abstractAccent.copy(alpha = 0.24f),
                        startAngle = 128f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width - orbit * 0.69f, -orbit * 0.38f),
                        size = androidx.compose.ui.geometry.Size(orbit, orbit),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawLine(
                        color = abstractAccent.copy(alpha = 0.14f),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.79f, size.height * 0.82f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.96f, size.height * 0.58f),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$greeting,", color = Muted, fontSize = 14.sp)
                            Text(
                                accountName.ifBlank { "there" },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
                                color = Muted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        AnimatedContent(
                            targetState = mascotTipVisible,
                            transitionSpec = {
                                if (!animationsEnabled) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else (fadeIn(animationSpec = tween(220)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(150)) +
                                        scaleOut(
                                            targetScale = 0.96f,
                                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                                        ))
                            },
                            label = "otter tap reaction",
                            modifier = Modifier
                                .size(76.dp)
                                .offset(y = mascotBob.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (mascotTipVisible) "Hide meeting tip" else "Show meeting tip"
                                ) { mascotTipVisible = !mascotTipVisible }
                        ) { isWaving ->
                            Image(
                                painter = painterResource(
                                    if (isWaving) R.drawable.meeting_otter_mascot_wave
                                    else R.drawable.meeting_otter_mascot
                                ),
                                contentDescription = if (isWaving) "Otter mascot waving" else "Tap the otter mascot for a meeting tip",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = mascotTipVisible,
                        enter = if (!animationsEnabled) EnterTransition.None else fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it / 5 },
                        exit = if (!animationsEnabled) ExitTransition.None else fadeOut(animationSpec = tween(160)) +
                            slideOutVertically(animationSpec = tween(160)) { -it / 6 }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    mascotTip,
                                    modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                                        .semantics { liveRegion = LiveRegionMode.Polite },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = { mascotTipVisible = false }) {
                                    Icon(SproutIcons.Close, contentDescription = "Dismiss meeting tip")
                                }
                            }
                        }
                    }
                }
            }
        }
        draftCard()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Upcoming Meetings", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Tap a meeting to preview its official schedule.", color = Muted, fontSize = 12.sp)
            }
            if (onRefresh != null) IconButton(onClick = onRefresh) { Icon(SproutIcons.Refresh, contentDescription = "Refresh upcoming meetings", tint = Forest) }
        }
        when {
            loading && upcoming.isEmpty() -> Surface(color = SurfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest)
                    Column { Text("Checking your schedule", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Loading upcoming meetings…", color = Muted, fontSize = 12.sp) }
                }
            }
            error != null && upcoming.isEmpty() -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(SproutIcons.ErrorOutline, contentDescription = "Schedule unavailable", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (onRefresh != null) TextButton(onClick = onRefresh) { Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
            upcoming.isEmpty() -> Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(SproutIcons.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(30.dp))
                    Text("No upcoming saved meetings", fontWeight = FontWeight.SemiBold)
                    Text(emptyMessage, color = Muted, fontSize = 12.sp)
                    if (canCreateSchedule) FilledTonalButton(onClick = onCreateSchedule, shape = MaterialTheme.shapes.medium) {
                        Icon(SproutIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp)); Text("Open Workbook")
                    }
                }
            }
            else -> {
                if (loading || error != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Forest)
                    Text(if (loading) "Updating · showing saved meetings" else "Couldn’t refresh · showing saved meetings", color = Muted, fontSize = 12.sp)
                    if (error != null && onRefresh != null) TextButton(onClick = onRefresh) { Text("Retry", fontSize = 12.sp) }
                }
                val (nextDate, nextSchedule) = upcoming.first()
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Preview ${nextSchedule.weekLabel}") { onOpenSchedule(nextSchedule.source) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                            Icon(SproutIcons.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("NEXT MEETING", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Text(nextSchedule.weekLabel, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${nextSchedule.reading} · ${nextDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH))}", color = Muted, fontSize = 12.sp)
                        }
                        Icon(SproutIcons.Preview, contentDescription = "Preview next meeting schedule", tint = Forest)
                    }
                }
                if (upcoming.size > 1) Text("MORE UPCOMING", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 2.dp))
                (if (showAllMeetings) upcoming else upcoming.take(3)).drop(1).forEach { (date, remote) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Preview ${remote.weekLabel}") { onOpenSchedule(remote.source) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(ForestContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(SproutIcons.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp))
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(remote.weekLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(remote.reading, color = Muted, fontSize = 12.sp)
                                Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)), color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(SproutIcons.Preview, contentDescription = "Preview schedule", tint = Forest)
                        }
                    }
                }
                if (upcoming.size > 3) {
                    TextButton(
                        onClick = { showAllMeetings = !showAllMeetings },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (showAllMeetings) "Show fewer meetings" else "Show all ${upcoming.size} upcoming meetings")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkbookContinueButton(onNext: () -> Unit, canContinue: Boolean, label: String = "Continue to assignments") {
    Button(
        onClick = onNext,
        enabled = canContinue,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Forest),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(9.dp))
        Icon(SproutIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun WorkbookMeetingDetails(
    weekLabel: String,
    bible: String,
    onBible: (String) -> Unit,
    openingSong: String,
    onOpeningSong: (String) -> Unit,
    closingSong: String,
    onClosingSong: (String) -> Unit,
    readOnly: Boolean,
    onThisWeek: (() -> Unit)?,
    onPickDate: () -> Unit
) {
    M3Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(ForestContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(SproutIcons.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp)) }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Label("MEETING WEEK")
                Text("Meeting details", style = MaterialTheme.typography.titleMedium)
            }
            if (onThisWeek != null) {
                TextButton(onClick = onThisWeek, enabled = !readOnly, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("This Week", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        OutlinedButton(onClick = onPickDate, enabled=!readOnly, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Text(weekLabel, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
            Text("Change", color = Forest, fontSize = 12.sp)
        }
        OutlinedTextField(bible, onBible, enabled=!readOnly, label = { Text("Bible reading") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        AdaptivePair(
            { field -> OutlinedTextField(openingSong, onOpeningSong, enabled = !readOnly,
                label = { Text("Opening song") }, singleLine = true, modifier = field) },
            { field -> OutlinedTextField(closingSong, onClosingSong, enabled = !readOnly,
                label = { Text("Closing song") }, singleLine = true, modifier = field) }
        )
    }
}

@Composable
private fun WorkbookProgramCard(
    workbookStatus: String,
    workbookBusy: Boolean,
    workbookCaptureLog: List<String>,
    fetchError: Boolean,
    captureSucceeded: Boolean,
    canImport: Boolean,
    actionLabel: String,
    onImport: () -> Unit,
    showCaptureLog: Boolean,
    onToggleLog: () -> Unit,
    extraActions: @Composable () -> Unit = {}
) {
    M3Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(if (fetchError) MaterialTheme.colorScheme.error.copy(alpha = .12f) else ForestContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(if (workbookBusy) SproutIcons.Refresh else if (captureSucceeded) SproutIcons.CheckCircle else SproutIcons.AutoStories, contentDescription = null, tint = if (fetchError) MaterialTheme.colorScheme.error else Forest, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Label("JW.ORG WORKBOOK")
                Text("Weekly program", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onImport, enabled = canImport && !workbookBusy, modifier = Modifier.size(48.dp)) { if (workbookBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest) else Icon(SproutIcons.Refresh, contentDescription = actionLabel, tint = Forest, modifier = Modifier.size(23.dp)) }
        }
        if (workbookBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = Forest,
                trackColor = ForestContainer
            )
        }
        Text(
            workbookStatus,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = if (fetchError) MaterialTheme.colorScheme.error else Muted
        )
        extraActions()
        if (workbookCaptureLog.isNotEmpty()) {
            TextButton(onClick = onToggleLog) {
                Text(if (showCaptureLog) "Hide capture log" else "Show capture log")
            }
        }
        if (showCaptureLog && workbookCaptureLog.isNotEmpty()) Surface(
            color = if (IsDarkPalette) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (workbookBusy) "CAPTURE LOG · RUNNING" else "CAPTURE LOG", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                    if (workbookBusy) CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.5.dp, color = Forest)
                }
                workbookCaptureLog.takeLast(6).forEach { line ->
                    Text(
                        "› $line",
                        color = if (line.startsWith("ERROR")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsScreen(modifier: Modifier, weekLabel: String, bible: String, onBible: (String) -> Unit, openingSong: String, onOpeningSong: (String) -> Unit, closingSong: String, onClosingSong: (String) -> Unit, workbookStatus: String, workbookBusy: Boolean, workbookCaptureLog: List<String>, canContinue:Boolean, participantsLoadFailed: Boolean, onRetryParticipants: () -> Unit, canContinueToAssignments:Boolean, readOnly:Boolean, isCurrentWeek:Boolean, onThisWeek: () -> Unit, onPickDate: () -> Unit, onImport: () -> Unit, onNext: () -> Unit) {
    var showCaptureLog by rememberSaveable(weekLabel) { mutableStateOf(false) }
    val workbookActions: (@Composable ColumnScope.() -> Unit)? = if (canContinueToAssignments) {
        {
            WorkbookContinueButton(
                onNext = onNext,
                canContinue = canContinue,
                label = when {
                    workbookBusy -> "Loading weekly program…"
                    !canContinue -> "Loading participants…"
                    else -> "Continue to assignments"
                }
            )
        }
    } else null
    WorkflowPage(modifier, actions = workbookActions) {
        PageHeading("Workbook", if (canContinueToAssignments) "Step 1 of 3 · Choose a meeting week" else "Select a week to load its meeting program.")
        WorkbookMeetingDetails(
            weekLabel, bible, onBible, openingSong, onOpeningSong, closingSong, onClosingSong,
            readOnly, if (isCurrentWeek) null else onThisWeek, onPickDate
        )
        val fetchError = !workbookBusy && (workbookCaptureLog.lastOrNull()?.startsWith("ERROR") == true || workbookStatus.contains("hindi", ignoreCase = true) || workbookStatus.contains("wala", ignoreCase = true) || workbookStatus.contains("permission", ignoreCase = true))
        WorkbookProgramCard(
            workbookStatus = workbookStatus.removePrefix("✓ "),
            workbookBusy = workbookBusy,
            workbookCaptureLog = workbookCaptureLog,
            fetchError = fetchError,
            captureSucceeded = workbookStatus.startsWith("✓"),
            canImport = !readOnly,
            actionLabel = "Refresh weekly program",
            onImport = onImport,
            showCaptureLog = showCaptureLog,
            onToggleLog = { showCaptureLog = !showCaptureLog }
        )
        if (canContinueToAssignments && participantsLoadFailed) {
            M3Card {
                Text("Couldn’t refresh participants. You can continue with the list available on this device.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetryParticipants) { Text("Retry participant sync") }
            }
        }
    }
}

@Composable
private fun AssignmentsScreen(modifier: Modifier, parts: List<Assignment>, participants: List<Participant>, rotation: Map<Int, ParticipantRotation>, rotationBusy: Boolean, rotationError: String?, chairman:String, openingPrayer:String, adviser:String, audienceGroup:String, isAdmin:Boolean, readOnly:Boolean, canContinueExport:Boolean, cloudSaveBusy:Boolean, cloudSaveFeedback:Pair<String,Boolean>?, onRole:(String,String)->Unit, onAssign: (Int,String,String) -> Unit, onSaveCloud: () -> Unit, onContinueExport: () -> Unit, saveToDevice: Boolean = false) {
    val eligible = participants.filter { it.enabled }
    val missingSlots = missingRequiredAssignmentSlots(parts, chairman, openingPrayer, adviser)
    val totalRequiredSlots = 3 + parts.sumOf { 1 + (if (it.hasAssistant) 1 else 0) + (if (it.hasSecondary) 1 else 0) + (if (it.hasSecondaryAssistant) 1 else 0) }
    val completedRequiredSlots = (totalRequiredSlots - missingSlots.size).coerceAtLeast(0)
    val completionProgress = if (totalRequiredSlots == 0) 1f else completedRequiredSlots.toFloat() / totalRequiredSlots
    val selectedNames = buildSet {
        listOf(chairman, openingPrayer, adviser).forEach { name ->
            name.trim().takeIf { it.isNotEmpty() }?.let { add(it.lowercase(Locale.ROOT)) }
        }
        parts.forEach { part ->
            listOf(part.person, part.assistant, part.secondaryAssignee, part.secondaryAssistant).forEach { name ->
                name.trim().takeIf { it.isNotEmpty() }?.let { add(it.lowercase(Locale.ROOT)) }
            }
        }
    }
    var showAllMissing by rememberSaveable { mutableStateOf(false) }
    val assignmentActions: (@Composable ColumnScope.() -> Unit)? = if ((isAdmin && !readOnly) || canContinueExport) {
        {
            if (isAdmin && !readOnly) {
                OutlinedButton(
                    onClick = onSaveCloud,
                    enabled = !cloudSaveBusy && missingSlots.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (cloudSaveBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest)
                    else Icon(if (saveToDevice) SproutIcons.Check else SproutIcons.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        if (saveToDevice) "Save to Device" else if (cloudSaveBusy) "Saving to Cloud…" else "Save to Cloud",
                        modifier = Modifier.weight(1f, fill = false),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                cloudSaveFeedback?.let { (message, success) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(
                            if (success) SproutIcons.CheckCircle else SproutIcons.ErrorOutline,
                            contentDescription = null,
                            tint = if (success) Forest else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            message,
                            modifier = Modifier.weight(1f).heightIn(max = 60.dp)
                                .verticalScroll(rememberScrollState())
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            color = if (success) Forest else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (canContinueExport) Button(
                onClick = onContinueExport,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Text("Continue to Export", modifier = Modifier.weight(1f, fill = false), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(9.dp))
                Icon(SproutIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
    } else null
    WorkflowPage(modifier, actions = assignmentActions) {
        PageHeading("Assignments", if (canContinueExport) "Step 2 of 3 · Review meeting roles and parts" else null)
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(
                        if (missingSlots.isEmpty()) SproutIcons.CheckCircle else SproutIcons.ErrorOutline,
                        contentDescription = null,
                        tint = if (missingSlots.isEmpty()) Forest else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(21.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (missingSlots.isEmpty()) "Assignments complete" else "${missingSlots.size} required slots remaining",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("$completedRequiredSlots of $totalRequiredSlots required slots filled · Group is optional", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                LinearProgressIndicator(
                    progress = { completionProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (missingSlots.isEmpty()) Forest else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                if (missingSlots.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (IsDarkPalette) .30f else .52f),
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                if (showAllMissing) "Still needed" else "Next unfilled slot",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            (if (showAllMissing) missingSlots else missingSlots.take(1)).forEach { slot ->
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(SproutIcons.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp).padding(top = 1.dp))
                                    Text(slot, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 16.sp)
                                }
                            }
                            if (missingSlots.size > 1) {
                                TextButton(onClick = { showAllMissing = !showAllMissing }) {
                                    Text(if (showAllMissing) "Show fewer slots" else "Show all ${missingSlots.size} unfilled slots")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (rotationBusy || rotationError != null) {
            Text(
                if (rotationBusy) "Loading participant rotation for this week…" else "Could not load participant rotation: $rotationError",
                color = if (rotationError != null) MaterialTheme.colorScheme.error else Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Card(shape = MaterialTheme.shapes.medium, colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant), border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Label("MEETING ROLES")
                    Text("Closing prayer · ${chairman.ifBlank { "Chairman" }}",color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                AdaptivePair(
                    { field -> SlotPicker("Chairman",chairman,eligible.filter { it.isAppointedBrother() },field,rotation,enabled=!readOnly,excludedNames=selectedNames){onRole("chairman",it)} },
                    { field -> SlotPicker("Opening prayer",openingPrayer,eligible.filter { it.isBrother() && !it.unbaptized },field,rotation,enabled=!readOnly,excludedNames=selectedNames){onRole("opening_prayer",it)} }
                )
                AdaptivePair(
                    { field -> SlotPicker("Adviser",adviser,eligible.filter { it.isAppointedBrother() },field,rotation,enabled=!readOnly,excludedNames=selectedNames){onRole("adviser",it)} },
                    { field -> SlotPicker("Audience group",audienceGroup,(1..9).map { Participant("Group $it") },field,rotation,enabled=!readOnly){onRole("audience_group",it)} }
                )
            }
        }
        Card(shape = MaterialTheme.shapes.medium, colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant), border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Meeting parts", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.fillMaxWidth().padding(top=6.dp)) {
                    parts.forEachIndexed { index, part ->
                        if(index>0) HorizontalDivider(Modifier.padding(vertical=6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AssignmentPicker(index, part, participants, rotation, readOnly, selectedNames, useLocalClock = saveToDevice) { slot, name -> onAssign(index,slot,name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentPicker(index: Int, part: Assignment, participants: List<Participant>, rotation: Map<Int, ParticipantRotation>, readOnly:Boolean, excludedNames: Set<String> = emptySet(), useLocalClock: Boolean = false, onAssign: (String,String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val darkBadges = IsDarkPalette
            val badgeColors = when (part.section) {
                "ministry" -> if (darkBadges) Color(0xFF5C4300) to Color(0xFFFFD782) else Color(0xFFFFD782) to Color(0xFF513A00)
                "living" -> if (darkBadges) Color(0xFF63342F) to Color(0xFFFFB8AE) else Color(0xFFFFB8AE) to Color(0xFF652F2A)
                else -> if (darkBadges) Color(0xFF164C50) to Color(0xFF9DE2E0) else Color(0xFFB7E7E4) to Color(0xFF12494B)
            }
            Box(Modifier.size(30.dp).background(badgeColors.first, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                Text("${part.id.takeIf { it>0 } ?: index+1}", color = badgeColors.second, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) { Text(part.title.substringAfter(". ",part.title), fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp); Text(listOf(if (useLocalClock) formatLocalMeetingTime(part.time) else part.time, part.duration, part.detail).filter{it.isNotBlank()}.joinToString(" · "), color = Muted, fontSize = 12.sp, lineHeight = 16.sp) }
        }
        val title=part.title.lowercase(Locale.ROOT); val bibleReading=title.contains("pagbabasa ng bibliya"); val bibleStudy=title.contains("pag-aaral ng kongregasyon sa bibliya"); val what=title.contains("ano ang sasabihin mo")
        val qualified=!what && (part.section=="living" || (part.section=="treasures" && !bibleReading))
        val eligible = participants.filter { it.enabled }
        val brothers = eligible.filter { it.isBrother() && !it.unbaptized }
        val elders = brothers.filter { it.isAppointedBrother() }
        // Bible Reading is a student part. The Congregation Bible Study reader uses a separate rule.
        val bibleReaders = eligible.filter { it.isBrother() }
        if(bibleStudy) {
            AdaptivePair(
                { field -> SlotPicker("Conductor", part.person, elders, field, rotation, enabled = !readOnly, excludedNames = excludedNames) { onAssign("main", it) } },
                { field -> SlotPicker("Reader", part.assistant, brothers, field, rotation, enabled = !readOnly, excludedNames = excludedNames) { onAssign("assistant", it) } }
            )
        } else if((part.section=="ministry"&&!what)||bibleReading) {
            val speakers = if (bibleReading) bibleReaders else eligible
            val unavailable = if (bibleReading) participants else emptyList()
            AdaptivePair(
                first = { field ->
                    Column(field, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SlotPicker("Main Hall", part.person, speakers, rotation = rotation, enabled = !readOnly,
                            excludedNames = excludedNames, unavailableChoices = unavailable) { onAssign("main", it) }
                        if (part.hasAssistant) {
                            SlotPicker("Assistant", part.assistant, eligible, rotation = rotation, enabled = !readOnly,
                                excludedNames = excludedNames) { onAssign("assistant", it) }
                        }
                    }
                },
                second = { field ->
                    Column(field, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SlotPicker("Second Hall", part.secondaryAssignee, speakers, rotation = rotation, enabled = !readOnly,
                            excludedNames = excludedNames, unavailableChoices = unavailable) { onAssign("secondary", it) }
                        if (part.hasSecondaryAssistant) {
                            SlotPicker("Assistant", part.secondaryAssistant, eligible, rotation = rotation, enabled = !readOnly,
                                excludedNames = excludedNames) { onAssign("secondaryAssistant", it) }
                        }
                    }
                }
            )
        } else {
            SlotPicker(if(what) "Brother" else if(qualified) "Qualified brother" else "Participant",part.person,if(what) eligible.filter { it.isBrother() } else if(qualified) elders else eligible,rotation=rotation,enabled=!readOnly,excludedNames=excludedNames){onAssign("main",it)}
            if(part.hasAssistant&&!what) SlotPicker("Assistant",part.assistant,eligible,rotation=rotation,enabled=!readOnly,excludedNames=excludedNames){onAssign("assistant",it)}
        }
    }
}

@Composable
private fun SlotPicker(
    label: String,
    value: String,
    choices: List<Participant>,
    modifier: Modifier = Modifier,
    rotation: Map<Int, ParticipantRotation> = emptyMap(),
    enabled: Boolean = true,
    excludedNames: Set<String> = emptySet(),
    unavailableChoices: List<Participant> = emptyList(),
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Participant?>(null) }
    val orderedChoices = remember(choices) { choices.sortedBy { it.name.trim().lowercase(Locale.ROOT) } }
    val currentName = value.trim().lowercase(Locale.ROOT)
    val filteredChoices = remember(orderedChoices, excludedNames, currentName, searchQuery) {
        val normalizedQuery = searchQuery.trim()
        orderedChoices.filter {
            val key = it.name.trim().lowercase(Locale.ROOT)
            (key !in excludedNames || key == currentName) && it.name.contains(normalizedQuery, ignoreCase = true)
        }
    }
    val query = searchQuery.trim()
    val unavailableMatches = if (query.isNotEmpty()) unavailableChoices.mapNotNull { participant ->
        val nameKey = participant.name.trim().lowercase(Locale.ROOT)
        if (!participant.name.contains(query, ignoreCase = true)) return@mapNotNull null
        val reason = when {
            !participant.enabled -> "Inactive participant"
            participant.gender.isBlank() || participant.gender.equals("unspecified", true) ->
                "Set Brother / Sister status in Participants"
            !participant.isBrother() -> "This part requires a brother"
            nameKey != currentName && nameKey in excludedNames -> "Already assigned this meeting"
            else -> return@mapNotNull null
        }
        participant.name to reason
    } else emptyList()
    Column(modifier.padding(top = 3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { if (enabled) { searchQuery = ""; expanded = true } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(value.ifBlank { "Select" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    if (expanded) {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val maxDialogHeight = minOf(620, screenHeight - 48).coerceAtLeast(240)
        val minDialogHeight = minOf(340, maxDialogHeight)
        Dialog(
            onDismissRequest = { expanded = false; searchQuery = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pickerPaletteSurface = MaterialTheme.colorScheme.primaryContainer
                .copy(alpha = if (IsDarkPalette) 0.30f else 0.18f)
                .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(horizontal = 16.dp)
                    .heightIn(min = minDialogHeight.dp, max = maxDialogHeight.dp),
                color = pickerPaletteSurface,
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Select $label", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Choose one participant", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { expanded = false; searchQuery = "" }) {
                            Icon(SproutIcons.Close, contentDescription = "Close participant picker", tint = Muted)
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        placeholder = { Text(if (choices.isNotEmpty() && choices.all { it.isAppointedBrother() }) "Search qualified brother" else "Search by name") },
                        leadingIcon = { Icon(SproutIcons.Search, contentDescription = null, tint = Forest) },
                        trailingIcon = if (searchQuery.isNotBlank()) ({
                            IconButton(onClick = { searchQuery = "" }) { Icon(SproutIcons.Close, contentDescription = "Clear search") }
                        }) else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp).clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = enabled) { onPick(""); expanded = false; searchQuery = "" }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(SproutIcons.Close, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                        Text("Unassigned", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (value.isBlank()) Icon(SproutIcons.Check, contentDescription = "Currently unassigned", tint = Forest, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f))
                    if (unavailableMatches.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            unavailableMatches.forEach { (name, reason) ->
                                Text("$name · $reason", color = Muted, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }
                    }
                    if (filteredChoices.isEmpty() && unavailableMatches.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (query.isBlank()) "No participants available." else "No matching participants.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 6.dp)
                        ) {
                            itemsIndexed(filteredChoices, key = { _, participant -> if (participant.id > 0) "id:${participant.id}" else "name:${participant.name.lowercase(Locale.ROOT)}" }) { _, participant ->
                                val stat = rotation[participant.id]?.takeUnless { participant.isAppointedBrother() || it.exempt }
                                val rotationStatus = when {
                                    participant.isAppointedBrother() || rotation[participant.id]?.exempt == true -> "Exempt from rotation"
                                    rotation[participant.id] == null -> null
                                    rotation[participant.id]?.neverAssigned == true -> "Priority · never assigned"
                                    (rotation[participant.id]?.totalThisMonth ?: 0) > 0 -> "Has a part this month"
                                    rotation[participant.id]?.eligible == false -> "Eligible after ${rotationShortDate(rotation[participant.id]?.nextEligibleDate.orEmpty())}"
                                    else -> "Available"
                                }
                                val statusColor = when {
                                    rotationStatus?.startsWith("Priority") == true -> MaterialTheme.colorScheme.primary
                                    rotationStatus == "Available" -> MaterialTheme.colorScheme.secondary
                                    rotationStatus?.startsWith("Has a part") == true -> MaterialTheme.colorScheme.tertiary
                                    else -> Muted
                                }
                                Row(
                                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                                        .clickable(enabled = enabled) {
                                            expanded = false
                                            searchQuery = ""
                                            if (stat != null && (stat.totalThisMonth > 0 || !stat.eligible)) pending = participant
                                            else onPick(participant.name)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f).padding(vertical = 1.dp)) {
                                        Text(participant.name, color = Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        rotationStatus?.let {
                                            Text(it, color = statusColor, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (participant.name.equals(value, ignoreCase = true)) Icon(SproutIcons.Check, contentDescription = "Currently selected", tint = Forest, modifier = Modifier.size(19.dp))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
                            }
                        }
                    }
                }
            }
        }
    }
    pending?.let { participant ->
        val stat = rotation[participant.id]
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Check assignment") },
            text = {
                val message = when {
                    stat == null -> "Continue assigning ${participant.name}?"
                    stat.totalThisMonth > 0 -> "${participant.name} already has a part this month. Do you want to assign them again?"
                    else -> "${participant.name} is still within the rotation interval${stat.nextEligibleDate?.let { ". Suggested next turn: ${rotationShortDate(it)}" } ?: ""}. Continue anyway?"
                }
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = { onPick(participant.name); pending = null }) { Text("Assign anyway") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null; expanded = true; searchQuery = "" }) { Text("Choose another") }
            }
        )
    }
}

private fun rotationShortDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d", Locale("en", "PH")))
}.getOrDefault(value)

@Composable
private fun PreviewScreen(modifier: Modifier, weekStart:String, bible:String, openingSong:String, closingSong:String, parts:List<Assignment>, chairman:String, openingPrayer:String, adviser:String, audienceGroup:String, previewWebView:MutableState<WebView?>, allowExport:Boolean, exportBusyType:String?, onExportText:()->Unit, onPng:()->Unit, onAssignmentSlipImages:()->Unit, useLocalClock: Boolean = false) {
    val context = LocalContext.current
    val html=remember(weekStart,bible,openingSong,closingSong,parts.toList(),chairman,openingPrayer,adviser,audienceGroup,useLocalClock) {
        buildOfficialScheduleHtml(context, weekStart,bible,openingSong,closingSong,parts,chairman,openingPrayer,adviser,audienceGroup,exportMode=true,useLocalClock=useLocalClock)
    }
    var previewReady by remember(html){mutableStateOf(false)}
    val pageAspect = SCHEDULE_EXPORT_WIDTH_PX.toFloat() / SCHEDULE_EXPORT_HEIGHT_PX
    PageColumn(modifier) {
        PageHeading("Preview & Export", if (allowExport) "Step 3 of 3 · Review and share the schedule" else null)
        if (allowExport) AdaptivePair(
            first = { button ->
                FilledTonalButton(
                    onClick=onPng, enabled=previewReady && exportBusyType == null, modifier=button.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium, contentPadding=PaddingValues(horizontal=10.dp)
                ) {
                    if (exportBusyType == "png") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Forest)
                    else Icon(SproutIcons.Share, contentDescription=null, modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (exportBusyType == "png") "Preparing…" else "Share PNG",fontSize=14.sp)
                }
            },
            second = { button ->
                OutlinedButton(
                    onClick=onExportText, enabled=exportBusyType == null, modifier=button.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium, contentPadding=PaddingValues(horizontal=10.dp)
                ) {
                    if (exportBusyType == "text") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Forest)
                    else Icon(SproutIcons.Description, contentDescription=null, modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (exportBusyType == "text") "Preparing…" else "Export text",fontSize=14.sp)
                }
            }
        )
        val hasAssignmentSlips = parts.any { it.person.isNotBlank() || it.secondaryAssignee.isNotBlank() } ||
            chairman.isNotBlank() || openingPrayer.isNotBlank() || adviser.isNotBlank()
        if (allowExport) OutlinedButton(
            onClick = onAssignmentSlipImages,
            enabled = exportBusyType == null && hasAssignmentSlips,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (exportBusyType == "slips_image") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Forest)
            else Icon(SproutIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (exportBusyType == "slips_image") "Preparing images…" else "Share assignment slip images", fontSize = 13.sp)
        }
        if (exportBusyType != null || !previewReady) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ForestContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Forest)
                        Text(
                            when (exportBusyType) {
                                "png" -> "Preparing the schedule image…"
                                "text" -> "Preparing the schedule text…"
                                "slips_image" -> "Preparing assignment slip images…"
                                else -> "Loading the schedule preview…"
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Forest, trackColor = ForestContainer)
                }
            }
        }
        Surface(
            color = Color.White, contentColor = Color(0xFF171717),
            shape = MaterialTheme.shapes.small,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(pageAspect)
        ) {
            OfficialScheduleWebPreview(html, previewWebView, Modifier.fillMaxSize()) { previewReady = true }
        }
    }
}

@Composable
private fun OfficialScheduleWebPreview(html:String,previewWebView:MutableState<WebView?>,modifier:Modifier=Modifier,onPageRendered:()->Unit) {
    val pageRendered by rememberUpdatedState(onPageRendered)
    var measuredWidthPx by remember { mutableIntStateOf(0) }
    AndroidView(
        factory = { context -> WebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.loadsImagesAutomatically = true
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            tag = ""
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean = true
                override fun onPageFinished(view: WebView, url: String?) {
                    fitScheduleWebView(
                        view = view,
                        targetWidthPx = view.width,
                        targetHeightPx = view.height,
                        density = view.resources.displayMetrics.density.coerceAtLeast(1f),
                        onReady = {
                            isVerticalScrollBarEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER
                            pageRendered()
                        },
                        onFailure = pageRendered
                    )
                }
            }
            previewWebView.value = this
        } },
        modifier = modifier.fillMaxWidth().onSizeChanged { size ->
            if (size.width > 0 && size.width != measuredWidthPx) measuredWidthPx = size.width
        },
        update = { view ->
            previewWebView.value = view
            val contentKey = "$html#$measuredWidthPx"
            if (measuredWidthPx > 0 && view.tag != contentKey) {
                view.tag = contentKey
                view.scrollTo(0, 0)
                view.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { view ->
            if (previewWebView.value === view) previewWebView.value = null
            view.stopLoading()
            view.destroy()
        }
    )
}

private fun buildAssignmentSlips(
    date: LocalDate,
    parts: List<Assignment>,
    chairman: String,
    openingPrayer: String,
    adviser: String
): List<AssignmentSlip> {
    val dateText = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
    return buildList {
        if (chairman.isNotBlank()) add(AssignmentSlip(chairman, "", dateText, "Chairman", "Main Hall"))
        if (openingPrayer.isNotBlank()) add(AssignmentSlip(openingPrayer, "", dateText, "Panalangin sa Simula", "Main Hall"))
        if (adviser.isNotBlank()) add(AssignmentSlip(adviser, "", dateText, "Second Hall Adviser", "Second Hall"))
        parts.forEachIndexed { index, part ->
            val number = part.id.takeIf { it > 0 } ?: index + 1
            val partTitle = when (number) {
                1 -> "Kayamanan Mula sa Salita ng Diyos"
                2 -> "Espirituwal na Hiyas"
                8 -> "Pag-aaral ng Kongregasyon sa Bibliya"
                else -> part.title.replaceFirst(Regex("^\\s*\\d+\\.\\s*"), "").trim()
            }
            val duration = part.duration.trim()
            val label = buildString {
                append(number).append(". ").append(partTitle)
                if (duration.isNotBlank() && !partTitle.contains(duration, ignoreCase = true)) {
                    append(" (").append(duration).append(")")
                }
            }
            if (part.person.isNotBlank()) add(AssignmentSlip(part.person, part.assistant, dateText, label, "Main Hall"))
            if (part.secondaryAssignee.isNotBlank()) add(AssignmentSlip(part.secondaryAssignee, part.secondaryAssistant, dateText, label, "Second Hall"))
        }
    }
}

private suspend fun assignmentSlipsShareIntent(
    context: Context, slips: List<AssignmentSlip>, weekStart: String, weekLabel: String
): Intent {
    val files = withContext(Dispatchers.IO) { createAssignmentSlipsImages(context, slips, weekStart) }
    val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.files", it) })
    val multiple = uris.size > 1
    return Intent(if (multiple) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
        type = "image/png"
        if (multiple) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        else putExtra(Intent.EXTRA_STREAM, uris.first())
        putExtra(Intent.EXTRA_SUBJECT, "Meeting assignment slips · $weekLabel")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Assignment slips images", uris.first()).apply {
            uris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
        }
    }
}

private suspend fun scheduleTextShareIntent(context: Context, text: String, weekStart: String, weekLabel: String): Intent {
    val file = withContext(Dispatchers.IO) {
        File(File(context.cacheDir, "exports").apply { mkdirs() }, "meeting-schedule-$weekStart.txt")
            .apply { writeText(text, Charsets.UTF_8) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Meeting schedule · $weekLabel")
        putExtra(Intent.EXTRA_TEXT, "Iskedyul ng Pulong ng Gitnang Sanlinggo · $weekLabel")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Meeting schedule text", uri)
    }
}

private fun buildOfficialScheduleText(
    weekStart: String, bible: String, openingSong: String, closingSong: String, parts: List<Assignment>,
    chairman: String, openingPrayer: String, adviser: String, audienceGroup: String,
    useLocalClock: Boolean = false
): String {
    val start = runCatching { LocalDate.parse(weekStart) }.getOrDefault(LocalDate.now())
    val end = start.plusDays(6)
    val months = listOf("Enero", "Pebrero", "Marso", "Abril", "Mayo", "Hunyo", "Hulyo", "Agosto", "Setyembre", "Oktubre", "Nobyembre", "Disyembre")
    val weekText = if (start.month == end.month) "${months[start.monthValue - 1]} ${start.dayOfMonth}–${end.dayOfMonth}" else "${months[start.monthValue - 1]} ${start.dayOfMonth}–${months[end.monthValue - 1]} ${end.dayOfMonth}"
    fun minutes(time: String): Int {
        val fields = time.split(":")
        return if (fields.size == 2) (fields[0].toIntOrNull() ?: 0) * 60 + (fields[1].toIntOrNull() ?: 0) else 0
    }
    fun clock(value: Int): String {
        if (useLocalClock) return localMeetingTimeFromMinutes(value)
        val hour = (value / 60) % 12
        return "${if (hour == 0) 12 else hour}:${(value % 60).toString().padStart(2, '0')}"
    }
    fun durationMinutes(value: String) = Regex("(\\d+)").find(value)?.value?.toIntOrNull() ?: 0
    fun isBibleReading(part: Assignment) = part.title.contains("pagbabasa ng bibliya", true)
    fun isBibleStudy(part: Assignment) = part.title.contains("pag-aaral ng kongregasyon sa bibliya", true)
    fun isWhat(part: Assignment) = part.title.contains("ano ang sasabihin mo", true)
    fun isDual(part: Assignment) = (part.section == "ministry" && !isWhat(part)) || isBibleReading(part)
    fun sectionTitle(section: String) = when (section) {
        "ministry" -> "MAGING MAHUSAY SA MINISTERYO"
        "living" -> "PAMUMUHAY BILANG KRISTIYANO"
        else -> "KAYAMANAN MULA SA SALITA NG DIYOS"
    }
    val firstMinute = parts.firstOrNull()?.time?.let(::minutes) ?: 366
    val last = parts.lastOrNull()
    val lastEnd = last?.let { minutes(it.time) + durationMinutes(it.duration) } ?: 457
    val grouped = parts.groupBy { it.section.ifBlank { "treasures" } }
    val order = listOf("treasures", "ministry", "living")
    return buildString {
        appendLine("ISKEDYUL NG PULONG NG GITNANG SANLINGGO")
        appendLine("$weekText | ${bible.uppercase()}")
        appendLine()
        appendLine("${clock(firstMinute - 6)}  •  Awit $openingSong at Panalangin")
        appendLine("       Panalangin : ${openingPrayer.ifBlank { "—" }}")
        appendLine("${clock(firstMinute - 1)}  •  Pambungad na Mga Komento (1 min.)")
        appendLine("       Chairman : ${chairman.ifBlank { "—" }}")
        appendLine("       Second Hall Adviser : ${adviser.ifBlank { "—" }} / ${audienceGroup.ifBlank { "Group —" }}")
        appendLine()
        order.forEach { section ->
            val rows = grouped[section].orEmpty()
            if (rows.isNotEmpty()) {
                appendLine(sectionTitle(section))
                rows.forEachIndexed { index, part ->
                    val number = part.id.takeIf { it > 0 } ?: index + 1
                    val title = part.title.replaceFirst(Regex("^\\s*\\d+\\.\\s*"), "").trim()
                    val heading = buildString {
                        append((if (useLocalClock) formatLocalMeetingTime(part.time) else part.time).ifBlank { "—" }).append("  •  ").append(number).append(". ").append(title)
                        if (part.duration.isNotBlank()) append(" (").append(part.duration).append(")")
                    }
                    appendLine(heading)
                    if (part.detail.isNotBlank()) appendLine("       ${part.detail}")
                    when {
                        isBibleStudy(part) -> {
                            appendLine("       Conductor : ${part.person.ifBlank { "—" }}")
                            appendLine("       Reader : ${part.assistant.ifBlank { "—" }}")
                        }
                        isDual(part) -> {
                            appendLine("       Main Hall : ${part.person.ifBlank { "—" }}")
                            if (part.assistant.isNotBlank()) appendLine("       Assistant : ${part.assistant}")
                            appendLine("       Second Hall : ${part.secondaryAssignee.ifBlank { "—" }}")
                            if (part.secondaryAssistant.isNotBlank()) appendLine("       Assistant : ${part.secondaryAssistant}")
                        }
                        else -> {
                            val qualified = !isWhat(part) && (part.section == "living" || (part.section == "treasures" && !isBibleReading(part)))
                            appendLine("       ${if (isWhat(part)) "Brother" else if (qualified) "Qualified brother" else "Participant"} : ${part.person.ifBlank { "—" }}")
                            if (part.assistant.isNotBlank()) appendLine("       Assistant : ${part.assistant}")
                        }
                    }
                    appendLine()
                }
            }
        }
        appendLine("${clock(lastEnd)}  •  Pangwakas na Komento (3 min.)")
        appendLine("${clock(lastEnd + 3)}  •  Awit $closingSong at Panalangin")
        appendLine("       Panalangin : ${chairman.ifBlank { "—" }}")
    }
}

private const val SCHEDULE_EXPORT_WIDTH_PX = 2480
private const val SCHEDULE_EXPORT_HEIGHT_PX = 3508
private const val SCHEDULE_EXPORT_DPI = 300
private const val SCHEDULE_EXPORT_CSS_WIDTH = 397
private const val SCHEDULE_EXPORT_CSS_HEIGHT = 562

private fun buildOfficialScheduleHtml(context: Context, weekStart:String,bible:String,openingSong:String,closingSong:String,parts:List<Assignment>,chairman:String,openingPrayer:String,adviser:String,audienceGroup:String,exportMode:Boolean=false,useLocalClock:Boolean=false):String {
    val start=runCatching{LocalDate.parse(weekStart)}.getOrDefault(LocalDate.now())
    val end=start.plusDays(6)
    val months=listOf("Enero","Pebrero","Marso","Abril","Mayo","Hunyo","Hulyo","Agosto","Setyembre","Oktubre","Nobyembre","Disyembre")
    val weekText=if(start.month==end.month) "${months[start.monthValue-1]} ${start.dayOfMonth}-${end.dayOfMonth}" else "${months[start.monthValue-1]} ${start.dayOfMonth}–${months[end.monthValue-1]} ${end.dayOfMonth}"
    fun minutes(time:String):Int { val p=time.split(":");return if(p.size==2) (p[0].toIntOrNull()?:0)*60+(p[1].toIntOrNull()?:0) else 0 }
    fun clock(value:Int):String {
        if (useLocalClock) return localMeetingTimeFromMinutes(value)
        val hour = (value / 60) % 12
        return "${if(hour==0)12 else hour}:${(value%60).toString().padStart(2,'0')}"
    }
    fun isBibleReading(p:Assignment)=p.title.contains("pagbabasa ng bibliya",true)
    fun isBibleStudy(p:Assignment)=p.title.contains("pag-aaral ng kongregasyon sa bibliya",true)
    fun isWhat(p:Assignment)=p.title.contains("ano ang sasabihin mo",true)
    fun dual(p:Assignment)=((p.section=="ministry")&&!isWhat(p))||isBibleReading(p)
    fun sectionMeta(section:String)=when(section){"ministry"->Triple("MAGING MAHUSAY SA MINISTERYO","#9b6d17","ministry");"living"->Triple("PAMUMUHAY BILANG KRISTIYANO","#942926","living");else->Triple("KAYAMANAN MULA SA SALITA NG DIYOS","#2a6b77","treasures")}
    fun durationMinutes(text:String)=Regex("(\\d+)").find(text)?.value?.toIntOrNull()?:0
    val firstMinute=parts.firstOrNull()?.time?.let(::minutes)?:366
    val last=parts.lastOrNull()
    val lastEnd=(last?.let{minutes(it.time)+durationMinutes(it.duration)}?:457)
    val closeSongTime=lastEnd+3
    val grouped=parts.groupBy{it.section.ifBlank{"treasures"}}
    val sectionOrder=listOf("treasures","ministry","living")
    val html=buildString {
        val viewport = if (exportMode) "width=$SCHEDULE_EXPORT_CSS_WIDTH,initial-scale=1,maximum-scale=1,user-scalable=no" else "width=device-width,initial-scale=1"
        append("<!doctype html><html><head><meta name=\"viewport\" content=\"").append(viewport).append("\"><style>")
        append(scheduleFontCss(context))
        append("*{box-sizing:border-box}html,body{margin:0;background:#fff;color:#171717;font-family:'Google Sans',sans-serif}body{padding:18px 20px 14px;font-size:6.4px;line-height:1.25")
        if (exportMode) append(";width:${SCHEDULE_EXPORT_CSS_WIDTH}px;min-height:${SCHEDULE_EXPORT_CSS_HEIGHT}px;overflow:visible;zoom:1")
        append("}.title{font-size:9.5px;font-weight:700;padding-bottom:5px;border-bottom:1px solid #252525}.sub{margin:7px 0 14px;color:#397782;font-size:7.2px}.toprow{display:grid;grid-template-columns:30px minmax(0,1fr) minmax(95px,.9fr);gap:5px;align-items:start;margin:0 0 5px}.time{white-space:nowrap}.bullet{font-size:6px;margin-right:3px}.role strong{margin-right:3px}.advisor{margin:7px 0 16px}.section{display:flex;align-items:center;gap:6px;margin:16px 0 8px;font-size:7.8px;font-weight:600}.badge{width:17px;height:17px;border-radius:4px;overflow:hidden;flex:0 0 17px}.badge img{display:block;width:100%;height:100%}.program{display:grid;grid-template-columns:30px minmax(80px,1.55fr) minmax(62px,1fr) minmax(62px,1fr);gap:5px;align-items:start;margin:0 0 8px}.part{overflow-wrap:anywhere}.part-dot{font-size:6px;line-height:1;margin-right:2px;vertical-align:middle}.part-number{font-size:7px;font-weight:400;line-height:1;margin-right:3px}.hall{font-size:5.6px;color:#555;text-align:left;margin-bottom:2px}.name{font-size:6.4px;font-weight:700;overflow-wrap:anywhere}.assistant{font-size:5.8px;font-weight:400;color:#777;margin-top:2px;overflow-wrap:anywhere}.single{grid-column:3/5}.footer{border-top:1px solid #ddd;margin-top:14px;padding-top:8px}.footnote{text-align:right;color:#999;font-size:5px;margin-top:20px}@media print{body{padding:18px 20px 14px}}")
        append("</style></head><body>")
        append("<div class=\"title\">Iskedyul ng Pulong ng Gitnang Sanlinggo</div><div class=\"sub\">").append(weekText.htmlSafe()).append(" | <b>").append(bible.uppercase().htmlSafe()).append("</b></div>")
        append("<div class=\"toprow\"><div class=\"time\">").append(clock(firstMinute-6)).append("</div><div><span class=\"bullet\">●</span>Awit ").append(openingSong.htmlSafe()).append(" at Panalangin</div><div class=\"role\"><strong>Panalangin :</strong> ").append(openingPrayer.ifBlank{"—"}.htmlSafe()).append("</div></div>")
        append("<div class=\"toprow\"><div class=\"time\">").append(clock(firstMinute-1)).append("</div><div><span class=\"bullet\">●</span>Pambungad na Mga Komento (1 min.)</div><div class=\"role\"><strong>Chairman :</strong> ").append(chairman.ifBlank{"—"}.htmlSafe()).append("</div></div>")
        append("<div class=\"toprow advisor\"><div></div><div></div><div class=\"role\"><strong>Second Hall Adviser :</strong> ").append(adviser.ifBlank{"—"}.htmlSafe()).append(" / ").append(audienceGroup.ifBlank{"Group —"}.htmlSafe()).append("</div></div>")
        var ministryHeadingsShown=false
        sectionOrder.forEach { key ->
            val rows=grouped[key].orEmpty(); if(rows.isEmpty()) return@forEach
            val meta=sectionMeta(key)
            append("<div class=\"section\" style=\"color:").append(meta.second).append("\"><span class=\"badge\"><img src=\"section-icons/").append(meta.third).append(".svg\" alt=\"\"></span>").append(meta.first).append("</div>")
            rows.forEach { p ->
                val bulletColor=meta.second
                val partNumber = p.id.takeIf { it > 0 } ?: (parts.indexOf(p) + 1)
                append("<div class=\"program\"><div class=\"time\">").append((if (useLocalClock) formatLocalMeetingTime(p.time) else p.time).ifBlank { "—" }.htmlSafe()).append("</div><div class=\"part\"><span class=\"part-dot\" style=\"color:").append(bulletColor).append("\">●</span><span class=\"part-number\">").append(partNumber).append(".</span>").append(p.title.replaceFirst(Regex("^\\s*\\d+\\.\\s*"),"").htmlSafe())
                if(p.duration.isNotBlank()) append(" (").append(p.duration.htmlSafe()).append(")")
                if(p.detail.isNotBlank()) append(" ").append(p.detail.htmlSafe())
                append("</div>")
                when {
                    isBibleStudy(p) -> { append("<div><div class=\"hall\">Conductor</div><div class=\"name\">").append(p.person.ifBlank{"—"}.htmlSafe()).append("</div></div><div><div class=\"hall\">Reader</div><div class=\"name\">").append(p.assistant.ifBlank{"—"}.htmlSafe()).append("</div></div>") }
                    dual(p) -> {
                        val showHallHeadings=p.section!="ministry" || !ministryHeadingsShown
                        append("<div>");if(showHallHeadings)append("<div class=\"hall\">Main Hall</div>");append("<div class=\"name\">").append(p.person.ifBlank{"—"}.htmlSafe()).append("</div>")
                        if(p.assistant.isNotBlank())append("<div class=\"assistant\">").append(p.assistant.htmlSafe()).append("</div>")
                        append("</div><div>");if(showHallHeadings)append("<div class=\"hall\">Second Hall</div>");append("<div class=\"name\">").append(p.secondaryAssignee.ifBlank{"—"}.htmlSafe()).append("</div>")
                        if(p.secondaryAssistant.isNotBlank())append("<div class=\"assistant\">").append(p.secondaryAssistant.htmlSafe()).append("</div>")
                        append("</div>");if(p.section=="ministry")ministryHeadingsShown=true
                    }
                    else -> { append("<div class=\"single\"><div class=\"name\">").append(p.person.ifBlank{"—"}.htmlSafe()).append("</div>");if(p.assistant.isNotBlank())append("<div class=\"assistant\">").append(p.assistant.htmlSafe()).append("</div>");append("</div>") }
                }
                append("</div>")
            }
        }
        append("<div class=\"footer\"><div class=\"toprow\"><div class=\"time\">").append(clock(lastEnd)).append("</div><div><span class=\"bullet\">●</span>Pangwakas na Komento (3 min.)</div><div></div></div>")
        append("<div class=\"toprow\"><div class=\"time\">").append(clock(closeSongTime)).append("</div><div><span class=\"bullet\">●</span>Awit ").append(closingSong.htmlSafe()).append(" at Panalangin</div><div class=\"role\"><strong>Panalangin :</strong> ").append(chairman.ifBlank{"—"}.htmlSafe()).append("</div></div></div>")
        append("</body></html>")
    }
    return html
}

@Composable
private fun ParticipantsScreen(
    modifier: Modifier, participants: List<Participant>, onAdd: (String) -> Unit,
    onToggle: (Int) -> Unit, onEdit: (Participant, Participant) -> Unit,
    isAdmin: Boolean, canEdit: Boolean, loading: Boolean, loadError: String?, onRefresh: () -> Unit,
    onSetActive: (Participant, Boolean) -> Unit, onDelete: (Participant) -> Unit,
    showRefresh: Boolean = true,
    onImportParticipants: (() -> Unit)? = null,
    participantImportBusy: Boolean = false
) {
    var query by rememberSaveable { mutableStateOf("") }
    var genderFilter by rememberSaveable { mutableStateOf("all") }
    var appointmentFilter by rememberSaveable { mutableStateOf("all") }
    var serviceFilter by rememberSaveable { mutableStateOf("all") }
    var baptismFilter by rememberSaveable { mutableStateOf("all") }
    var statusFilter by rememberSaveable { mutableStateOf("active") }
    var sortBy by rememberSaveable { mutableStateOf("name") }
    var showFilters by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Participant?>(null) }
    var deleteTarget by remember { mutableStateOf<Participant?>(null) }
    var editName by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("brother") }
    var editAppointment by remember { mutableStateOf("publisher") }
    var editService by remember { mutableStateOf("none") }
    var editUnbaptized by remember { mutableStateOf(false) }
    var visibleCount by rememberSaveable { mutableIntStateOf(50) }
    LaunchedEffect(query, genderFilter, appointmentFilter, serviceFilter, baptismFilter, statusFilter, sortBy) { visibleCount = 50 }
    val participantsSnapshot by remember(participants) { derivedStateOf { participants.toList() } }
    val filtered = remember(participantsSnapshot, query, genderFilter, appointmentFilter, serviceFilter, baptismFilter, statusFilter, sortBy) {
        participantsSnapshot.withIndex().filter { (_, p) ->
            p.name.contains(query, true) && (statusFilter == "all" || (statusFilter == "active" && p.enabled) || (statusFilter == "inactive" && !p.enabled)) &&
                (genderFilter == "all" || p.gender == genderFilter) && (appointmentFilter == "all" || p.appointment == appointmentFilter) &&
                (serviceFilter == "all" || p.service == serviceFilter) && (baptismFilter == "all" || (baptismFilter == "unbaptized" && p.unbaptized) || (baptismFilter == "baptized" && !p.unbaptized))
        }.sortedWith(when (sortBy) {
            "gender" -> compareBy<IndexedValue<Participant>> { it.value.gender }.thenBy { it.value.name.lowercase() }
            "appointment" -> compareBy<IndexedValue<Participant>> { it.value.appointment }.thenBy { it.value.name.lowercase() }
            "service" -> compareBy<IndexedValue<Participant>> { it.value.service }.thenBy { it.value.name.lowercase() }
            "unbaptized" -> compareByDescending<IndexedValue<Participant>> { it.value.unbaptized }.thenBy { it.value.name.lowercase() }
            "name_desc" -> compareByDescending<IndexedValue<Participant>> { it.value.name.lowercase() }
            else -> compareBy { it.value.name.lowercase() }
        })
    }
    fun beginEdit(p: Participant) { editing=p; editName=p.name; editGender=p.gender; editAppointment=p.appointment; editService=p.service; editUnbaptized=p.unbaptized }
    PageFrame(modifier) { page ->
        Column(page.fillMaxHeight().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeading("Participants") {
                if (onImportParticipants != null) TextButton(onClick = onImportParticipants, enabled = !participantImportBusy) { Text("Import") }
                if (showRefresh) IconButton(onClick = onRefresh) {
                    Icon(SproutIcons.Refresh, contentDescription = "Refresh participants", tint = Forest)
                }
                if (canEdit) IconButton(onClick = { showAdd = true }) {
                    Icon(SproutIcons.PersonAdd, contentDescription = "Add participant", tint = Forest)
                }
            }
            Text("${participants.size} participants · showing ${minOf(visibleCount, filtered.size)} of ${filtered.size} matches",color=Muted,fontSize=12.sp)
            if (loading && participants.isNotEmpty()) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(15.dp),strokeWidth=2.dp,color=Forest)
                Text("Updating participant list · showing saved data",color=Muted,fontSize = 12.sp)
            }
            if (loadError != null) Surface(color=MaterialTheme.colorScheme.errorContainer,shape = MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(SproutIcons.ErrorOutline,contentDescription="Participant refresh failed",tint=MaterialTheme.colorScheme.onErrorContainer,modifier=Modifier.size(19.dp))
                    Text((if (participants.isEmpty()) "Could not load participants. " else "Showing saved participants. ") + loadError,color=MaterialTheme.colorScheme.onErrorContainer,fontSize = 12.sp,modifier=Modifier.weight(1f))
                    TextButton(onClick=onRefresh,contentPadding=PaddingValues(horizontal=6.dp)){Text("Retry",color=MaterialTheme.colorScheme.onErrorContainer,fontSize = 12.sp)}
                }
            }
            Card(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=SurfaceContainer),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(query,{query=it},placeholder={Text("Search participant")},singleLine=true,shape = MaterialTheme.shapes.medium,modifier=Modifier.weight(1f).heightIn(min=48.dp),textStyle=MaterialTheme.typography.bodyMedium)
                        IconButton(onClick={showFilters=true},modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)){Icon(SproutIcons.Sort,contentDescription="Filter and sort participants",tint=Forest,modifier=Modifier.size(22.dp))}
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=6.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        itemsIndexed(filtered.take(visibleCount),key={_,entry->if(entry.value.id>0)entry.value.id.toLong() else entry.index.toLong()}) { _,entry ->
                            val (index,p)=entry
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth().heightIn(min=52.dp).semantics { if (canEdit) role = Role.Button }.then(if(canEdit) Modifier.clickable { beginEdit(p) } else Modifier).padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).background(ForestContainer,CircleShape),contentAlignment=Alignment.Center){Text(p.name.split(" ").take(2).mapNotNull{it.firstOrNull()}.joinToString("").uppercase(),color=Forest,fontWeight=FontWeight.Bold,fontSize = 12.sp)}
                                Column(Modifier.weight(1f).padding(start=10.dp)) {
                                    Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (p.unbaptized) "Unbaptized · ${genderLabel(p.gender)}${if(!p.enabled) " · Inactive" else ""}"
                                        else "${appointmentLabel(p.appointment)} · ${serviceLabel(p.service)} · ${genderLabel(p.gender)}${if(!p.enabled) " · Inactive" else ""}",
                                        color=Muted,fontSize = 12.sp,maxLines=1
                                    )
                                }
                                if(canEdit) IconButton(onClick={beginEdit(p)},modifier=Modifier.size(48.dp)){Icon(SproutIcons.Edit,contentDescription="Edit participant",tint=Forest,modifier=Modifier.size(20.dp))}
                                if(isAdmin) {
                                    var menu by remember(p.id){mutableStateOf(false)}
                                    Box {
                                        IconButton(onClick = { menu = true }) { Icon(SproutIcons.MoreVert, contentDescription = "Participant actions") }
                                        DropdownMenu(expanded=menu,onDismissRequest={menu=false},modifier=Modifier.background(AppSurface)) {
                                            DropdownMenuItem(text={Text(if(p.enabled) "Set inactive" else "Set active")},onClick={menu=false;onSetActive(p,!p.enabled)})
                                            DropdownMenuItem(text={Text("Delete permanently",color=MaterialTheme.colorScheme.error)},onClick={menu=false;deleteTarget=p})
                                        }
                                    }
                                }
                            }
                        }
                        if (loading && participants.isEmpty()) item {
                            Row(Modifier.fillMaxWidth().padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Forest)
                                Text("Loading participants…",color=Muted,fontSize=12.sp)
                            }
                        } else if(filtered.isEmpty()) item { Text(if (query.isNotBlank() || genderFilter!="all" || appointmentFilter!="all" || serviceFilter!="all" || baptismFilter!="all") "No participants match the current search or filters." else if (!showRefresh) "No participants yet. Add or import participants to get started." else if (canEdit) "No participants yet. Refresh the list or add a participant." else "No participants are available yet. Refresh the list to try again.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=12.dp)) }
                        if (filtered.size > visibleCount) item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                                FilledTonalButton(onClick = { visibleCount += 50 }, shape = MaterialTheme.shapes.medium) {
                                    Icon(SproutIcons.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Show next ${minOf(50, filtered.size - visibleCount)} participants")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if(showAdd && canEdit) AlertDialog(onDismissRequest={showAdd=false},containerColor=MaterialTheme.colorScheme.surfaceContainerHigh,shape = MaterialTheme.shapes.extraLarge,title={Text("Add participant")},text={OutlinedTextField(newName,{newName=it},label={Text("Full name")},singleLine=true)},confirmButton={TextButton(onClick={if(newName.isNotBlank())onAdd(newName.trim());newName="";showAdd=false}){Text("Add",color=Forest)}},dismissButton={TextButton(onClick={showAdd=false}){Text("Cancel",color=Forest)}})
    if(editing!=null && canEdit) AlertDialog(
        onDismissRequest={editing=null},containerColor=MaterialTheme.colorScheme.surfaceContainerHigh,shape = MaterialTheme.shapes.extraLarge,title={Text("Edit participant",fontWeight=FontWeight.Bold)},
        text={Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(editName,{editName=it},label={Text("Name")},singleLine=true,modifier=Modifier.fillMaxWidth())
            ParticipantChoice("Gender",editGender,listOf("brother" to "Brother","sister" to "Sister")){editGender=it}
            ParticipantChoice("Appointment",editAppointment,listOf("" to "","publisher" to "Publisher","ministerial" to "Ministerial servant","elder" to "Elder"),enabled=!editUnbaptized){editAppointment=it;if(it!="publisher")editUnbaptized=false}
            ParticipantChoice("Service status",editService,listOf("" to "","none" to "Publisher","regular_pioneer" to "Regular pioneer"),enabled=!editUnbaptized){editService=it;if(it=="regular_pioneer")editUnbaptized=false}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Unbaptized",fontSize=14.sp);Switch(checked=editUnbaptized,onCheckedChange={editUnbaptized=it;if(it){editAppointment="";editService=""}},colors=SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = Forest))}
        }},
        confirmButton={TextButton(onClick={editing?.let{old->if(editName.isNotBlank())onEdit(old,old.copy(name=editName.trim(),gender=editGender,appointment=editAppointment,service=editService,unbaptized=editUnbaptized))};editing=null}){Text("Save",color=Forest)}},
        dismissButton={TextButton(onClick={editing=null}){Text("Cancel",color=Forest)}}
    )
    if(showFilters) AlertDialog(
        onDismissRequest={showFilters=false},containerColor=MaterialTheme.colorScheme.surfaceContainerHigh,shape = MaterialTheme.shapes.extraLarge,title={Text("Filter and sort",fontWeight=FontWeight.Bold)},
        text={Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ParticipantChoice("Status",statusFilter,listOf("active" to "Active","inactive" to "Inactive","all" to "All")){statusFilter=it}
            ParticipantChoice("Baptism",baptismFilter,listOf("all" to "All","baptized" to "Baptized","unbaptized" to "Unbaptized")){baptismFilter=it}
            ParticipantChoice("Brother / Sister",genderFilter,listOf("all" to "All","brother" to "Brother","sister" to "Sister")){genderFilter=it}
            ParticipantChoice("Appointment",appointmentFilter,listOf("all" to "All","elder" to "Elder","ministerial" to "Ministerial servant","publisher" to "Publisher")){appointmentFilter=it}
            ParticipantChoice("Service status",serviceFilter,listOf("all" to "All","regular_pioneer" to "Regular pioneer","none" to "Publisher")){serviceFilter=it}
            ParticipantChoice("Sort by",sortBy,listOf("name" to "Name A–Z","name_desc" to "Name Z–A","gender" to "Brother / Sister","appointment" to "Appointment","service" to "Service status","unbaptized" to "Baptism status")){sortBy=it}
        }},
        confirmButton={TextButton(onClick={showFilters=false}){Text("Done",color=Forest)}},
        dismissButton={TextButton(onClick={statusFilter="active";baptismFilter="all";genderFilter="all";appointmentFilter="all";serviceFilter="all";sortBy="name"}){Text("Reset",color=Forest)}}
    )
    if(deleteTarget!=null) AlertDialog(onDismissRequest={deleteTarget=null},containerColor=MaterialTheme.colorScheme.surfaceContainerHigh,shape = MaterialTheme.shapes.extraLarge,title={Text("Delete participant?")},text={Text("Permanent delete removes ${deleteTarget?.name} and their assignment history. This cannot be undone.")},confirmButton={TextButton(onClick={deleteTarget?.let(onDelete);deleteTarget=null}){Text("Delete",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleteTarget=null}){Text("Cancel",color=Forest)}})
}

private fun appointmentLabel(value: String) = when(value.lowercase()) { "" -> ""; "elder" -> "Elder"; "ministerial" -> "Ministerial"; "publisher" -> "Publisher"; else -> "Publisher" }
private fun serviceLabel(value: String) = when(value.lowercase()) { "" -> ""; "regular_pioneer" -> "Regular pioneer"; "none" -> "Publisher"; else -> "Publisher" }
private fun genderLabel(value: String) = when(value.lowercase()) { "sister" -> "Sister"; "brother" -> "Brother"; else -> "Unspecified" }

@Composable
private fun ParticipantChoice(label: String, value: String, options: List<Pair<String,String>>, enabled: Boolean = true, onSelect: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color=Muted, fontSize = 12.sp)
        Box {
            OutlinedButton(onClick={expanded=true}, enabled=enabled, modifier=Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding=PaddingValues(horizontal=13.dp, vertical=9.dp)) {
                Text(options.firstOrNull { it.first==value }?.second?.ifBlank { "Not set" } ?: options.first().second.ifBlank { "Not set" }, color=Ink, modifier=Modifier.weight(1f))
            }
            DropdownMenu(expanded=expanded, onDismissRequest={expanded=false}, modifier=Modifier.background(AppSurface)) {
                options.forEach { (key,title) -> DropdownMenuItem(text={Text(title.ifBlank { "Not set" })}, enabled=enabled, onClick={onSelect(key);expanded=false}) }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Column(Modifier.padding(12.dp)) { Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(value, color = Forest, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun <T> HistoryScreen(
    modifier: Modifier,
    schedules: List<WorkspaceScheduleEntry<T>>,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    loadingMore: Boolean,
    availableYears: List<Int>,
    onLoadMore: () -> Unit,
    isAdmin: Boolean,
    canEdit: Boolean,
    canCreate: Boolean,
    onView: (T) -> Unit,
    onEdit: (T) -> Unit,
    onDelete: (T) -> Unit,
    onRefresh: (() -> Unit)?,
    onNew: () -> Unit,
    local: Boolean = false
) {
    var deleteTarget by remember { mutableStateOf<WorkspaceScheduleEntry<T>?>(null) }
    var historyQuery by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableStateOf("all") }
    val sortedSchedules = schedules.sortedWith(
        compareByDescending<WorkspaceScheduleEntry<T>> { it.weekStart ?: LocalDate.MIN }
            .thenByDescending { it.updatedAt }
    )
    val historyYears = (availableYears + sortedSchedules.mapNotNull { it.weekStart?.year }).distinct().sortedDescending()
    val filteredSchedules = sortedSchedules.filter { schedule ->
        val startDate = schedule.weekStart
        val matchesYear = selectedYear == "all" || startDate?.year?.toString() == selectedYear
        val matchesQuery = historyQuery.isBlank() || schedule.weekLabel.contains(historyQuery.trim(), ignoreCase = true) ||
            schedule.reading.contains(historyQuery.trim(), ignoreCase = true) ||
            schedule.updatedAt.contains(historyQuery.trim(), ignoreCase = true) ||
            schedule.assignedNames.any { it.contains(historyQuery.trim(), ignoreCase = true) }
        matchesYear && matchesQuery
    }
    PageColumn(modifier) {
        PageHeading("History")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${schedules.size} loaded · newest first", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRefresh != null) IconButton(onClick=onRefresh,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)){Icon(SproutIcons.Refresh,contentDescription="Refresh schedule history",tint=Forest,modifier=Modifier.size(20.dp))}
                if (canCreate) FilledTonalButton(onClick = onNew, shape = MaterialTheme.shapes.medium) {
                    Icon(SproutIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New schedule")
                }
            }
        }
        if (loading && schedules.isNotEmpty()) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(15.dp),strokeWidth=2.dp,color=Forest)
            Text("Refreshing · showing saved schedules",color=Muted,fontSize = 12.sp)
        }
        if (error != null) Surface(color=MaterialTheme.colorScheme.errorContainer,shape = MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(SproutIcons.ErrorOutline,contentDescription="History refresh failed",tint=MaterialTheme.colorScheme.onErrorContainer,modifier=Modifier.size(19.dp))
                Text((if(schedules.isEmpty()) "Could not load history. " else "Showing saved history. ")+error,color=MaterialTheme.colorScheme.onErrorContainer,fontSize = 12.sp,modifier=Modifier.weight(1f))
                if (onRefresh != null) TextButton(onClick=onRefresh,contentPadding=PaddingValues(horizontal=6.dp)){Text("Retry",color=MaterialTheme.colorScheme.onErrorContainer,fontSize = 12.sp)}
            }
        }
        M3Card {
            if (schedules.isEmpty()) {
                if (loading) Row(Modifier.fillMaxWidth().padding(vertical=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Forest)
                    Text("Loading saved schedules…",color=Muted,fontSize=12.sp)
                } else if (error == null) Column(Modifier.fillMaxWidth().padding(vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(SproutIcons.CalendarMonth,contentDescription=null,tint=Forest,modifier=Modifier.size(28.dp))
                    Text("No saved schedules yet",fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                    Text(if (local) "Schedules saved on this device will appear here." else "Schedules saved to the cloud will appear here.",color=Muted,fontSize=12.sp)
                    if(canCreate) FilledTonalButton(onClick=onNew,shape = MaterialTheme.shapes.medium){Icon(SproutIcons.Add,contentDescription=null,modifier=Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("Create schedule")}
                }
            } else {
                OutlinedTextField(
                    value = historyQuery,
                    onValueChange = { historyQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search loaded weeks, readings, or names") },
                    leadingIcon = { Icon(SproutIcons.Search, contentDescription = null, tint = Forest) },
                    trailingIcon = if (historyQuery.isNotBlank()) ({
                        IconButton(onClick = { historyQuery = "" }) { Icon(SproutIcons.Close, contentDescription = "Clear history search") }
                    }) else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedYear == "all",
                        onClick = { selectedYear = "all" },
                        label = { Text("All years", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    historyYears.forEach { year ->
                        FilterChip(
                            selected = selectedYear == year.toString(),
                            onClick = { selectedYear = year.toString() },
                            label = { Text(year.toString(), fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                if (filteredSchedules.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(SproutIcons.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(25.dp))
                        Text("No schedules match your search or year filter.", color = Muted, fontSize = 12.sp)
                        TextButton(onClick = { historyQuery = ""; selectedYear = "all" }) { Text("Clear search and filters", color = Forest) }
                    }
                }
            }
            filteredSchedules.forEach { remote ->
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(ForestContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(SproutIcons.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                        Text(remote.weekLabel, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${remote.reading} · ${remote.updatedAt.take(10)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onView(remote.source) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(SproutIcons.Visibility, contentDescription = null, tint = Forest, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View", fontSize = 12.sp, color = Forest)
                    }
                    if (isAdmin) {
                        var menu by remember(remote.key) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                                Icon(SproutIcons.MoreVert, contentDescription = "Schedule actions", tint = Forest)
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(AppSurface)) {
                                if (canEdit) DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(SproutIcons.Edit, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Edit") } },
                                    onClick = { menu = false; onEdit(remote.source) }
                                )
                                if (isAdmin) DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(SproutIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Delete", color = MaterialTheme.colorScheme.error) } },
                                    onClick = { menu = false; deleteTarget = remote }
                                )
                            }
                        }
                    }
                }
            }
            if (hasMore) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    FilledTonalButton(onClick = onLoadMore, enabled = !loadingMore, shape = MaterialTheme.shapes.medium) {
                        if (loadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Forest)
                        else Icon(SproutIcons.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (loadingMore) "Loading…" else if (historyQuery.isNotBlank()) "Load more to continue search" else "Load more schedules")
                    }
                }
            }
        }
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = AppSurface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete saved schedule?") },
            text = { Text(if (local) "${item.weekLabel} will be removed from local schedule history." else "${item.weekLabel} will be removed from schedule history. Assignment history will be archived safely.", color = Muted) },
            confirmButton = { TextButton(onClick = { onDelete(item.source); deleteTarget = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = Forest) } }
        )
    }
}

private fun scheduleStartDate(label:String, updatedAt:String=""): LocalDate? = runCatching {
    val clean=label.substringBefore('|').trim().replace(Regex("(?i)^week of\\s+"),"")
    Regex("^\\d{4}-\\d{2}-\\d{2}$").takeIf { it.matches(clean) }?.let { return LocalDate.parse(clean) }
    val match=Regex("^([\\p{L}]+)\\s+(\\d{1,2})\\s*[–—-]\\s*(?:([\\p{L}]+)\\s+)?(\\d{1,2})(?:,?\\s*(\\d{4}))?").find(clean)
    if(match==null) {
        updatedAt.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }?.let { LocalDate.parse(it) } ?: return null
    } else {
        fun monthValue(raw:String):Int {
            val normalized=raw.lowercase(Locale.ROOT).take(3)
            return when(normalized){"jan","ene"->1;"feb","peb"->2;"mar"->3;"apr","abr"->4;"may"->5;"jun","hun"->6;"jul","hul"->7;"aug","ago"->8;"sep","set"->9;"oct","okt"->10;"nov","nob"->11;"dec","dic","dis"->12;else->throw IllegalArgumentException("Unknown month: $raw")}
        }
        val startMonth=monthValue(match.groupValues[1]); val endMonth=monthValue(match.groupValues[3].ifBlank { match.groupValues[1] })
        val year=match.groupValues[5].toIntOrNull() ?: updatedAt.take(4).toIntOrNull() ?: LocalDate.now().year
        LocalDate.of(if(startMonth>endMonth) year-1 else year,startMonth,match.groupValues[2].toInt())
    }
}.getOrNull()

private fun remoteScheduleStartDate(schedule: RemoteSchedule): LocalDate? =
    runCatching { LocalDate.parse(schedule.weekStart) }.getOrNull()
        ?: scheduleStartDate(schedule.weekLabel, schedule.updatedAt)

private data class NotificationFeedback(val sent: Boolean, val message: String)

@Composable
private fun WorkspacePreferencesCard(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    paletteStyle: AppPaletteStyle,
    onPaletteStyleChange: (AppPaletteStyle) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    appLock: AppLockController
) {
    M3Card {
        Text("Preferences", style = MaterialTheme.typography.titleMedium)
        Text("Appearance", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { option ->
                val selected = themeMode == option
                val icon = when (option) {
                    AppThemeMode.SYSTEM -> SproutIcons.Settings
                    AppThemeMode.LIGHT -> SproutIcons.LightMode
                    AppThemeMode.DARK -> SproutIcons.DarkMode
                }
                Column(
                    modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                        .clickable { onThemeModeChange(option) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = if (selected) ForestContainer else SurfaceContainer,
                        border = if (selected) BorderStroke(2.dp, Forest) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = if (selected) Forest else Muted, modifier = Modifier.size(24.dp))
                        }
                    }
                    Text(
                        option.label,
                        color = if (selected) Forest else Ink,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Text("Color palette", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        Text("Choose the accent colors used throughout the app.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPaletteStyle.entries.forEach { option ->
                val selected = option == paletteStyle
                Column(
                    modifier = Modifier.width(88.dp).clip(MaterialTheme.shapes.medium)
                        .clickable { onPaletteStyleChange(option) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(12.dp))
                            .then(if (selected) Modifier.border(2.dp, Forest, RoundedCornerShape(12.dp)) else Modifier)
                    ) {
                        option.swatches.forEach { swatch ->
                            Box(Modifier.weight(1f).fillMaxWidth().background(swatch))
                        }
                    }
                    Text(option.label, color = if (selected) Forest else Ink, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
        BiometricLockPreference(appLock)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(SproutIcons.ManageAccounts, contentDescription = null, tint = Forest, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("App font size", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${(fontScale * 100).toInt()}%", color = Muted, fontSize = 12.sp)
            }
            TextButton(onClick = { onFontScaleChange(1f) }, enabled = fontScale != 1f) { Text("Reset", color = Forest) }
        }
        Slider(value = fontScale, onValueChange = onFontScaleChange, valueRange = 0.85f..1.35f, steps = 9)
        Text("Applies across the app and is saved on this device.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun AccountsScreen(
    modifier: Modifier, accounts: List<RemoteAccount>, linkParticipants: List<AccountLinkParticipant>, loading: Boolean, error: String?,
    testNotificationBusy: Boolean, testNotificationFeedback: NotificationFeedback?, onTestNotification: () -> Unit, currentUsername: String,
    profileName: String, profileUsername: String, profileRole: String, profilePhotoPath: String?, profilePhotoDataUrl: String,
    profileError: String?, onChangeOwnPhoto: () -> Unit, onSaveOwnName: (String) -> Unit,
    isAdmin: Boolean, fontScale: Float, onFontScaleChange: (Float) -> Unit,
    themeMode: AppThemeMode, onThemeModeChange: (AppThemeMode) -> Unit, paletteStyle: AppPaletteStyle, onPaletteStyleChange: (AppPaletteStyle) -> Unit, appLock: AppLockController,
    onRefresh: () -> Unit, onUploadPhoto: (RemoteAccount) -> Unit,
    onLinkParticipant: (RemoteAccount, Int?) -> Unit,
    onAction: (RemoteAccount, String, String, String) -> Unit,
    onUpdateToolAccess: (RemoteAccount, Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    var selectedAccount by remember { mutableStateOf<RemoteAccount?>(null) }
    var actionTarget by remember { mutableStateOf<RemoteAccount?>(null) }
    var selectedAction by remember { mutableStateOf("") }
    var temporaryPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("viewer") }
    var editProfileNameOpen by remember { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    var licenseInfoOpen by remember { mutableStateOf(false) }
    var profileNameDraft by rememberSaveable(profileName) { mutableStateOf(profileName) }
    val filtered = accounts.sortedWith(compareBy<RemoteAccount>({ if (it.role == "admin") 0 else 1 }, { when (it.status) { "requested" -> 0; "active" -> 1; else -> 2 } }, { it.displayName.lowercase() }))
    fun beginAction(account: RemoteAccount, action: String) {
        selectedAccount = null
        actionTarget = account
        selectedAction = action
        temporaryPassword = ""
        selectedRole = account.role.ifBlank { "viewer" }
    }
    PageColumn(modifier) {
        PageHeading("Account & Settings") {
            if (isAdmin) IconButton(onClick = onRefresh, enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(SproutIcons.Refresh, contentDescription = "Refresh accounts", tint = Forest)
            }
        }
        M3Card {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileAvatar(profileName, profilePhotoPath, 64.dp, profilePhotoDataUrl)
                Column(Modifier.weight(1f)) {
                    Text(profileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@$profileUsername", color = Muted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when (profileRole.lowercase(Locale.ROOT)) {
                            "admin", "administrator" -> "ADMINISTRATOR"
                            else -> profileRole.uppercase(Locale.ROOT)
                        },
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChangeOwnPhoto, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium) {
                    Icon(SproutIcons.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change photo", maxLines = 1)
                }
                OutlinedButton(onClick = { profileNameDraft = profileName; editProfileNameOpen = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(SproutIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit name", maxLines = 1)
                }
            }
            profileError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        WorkspacePreferencesCard(
            themeMode, onThemeModeChange, paletteStyle, onPaletteStyleChange,
            fontScale, onFontScaleChange, appLock
        )
        M3Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(SproutIcons.NotificationsActive, contentDescription = null, tint = Forest, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Test notification", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Check that this account can receive push notifications.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onTestNotification, enabled = !testNotificationBusy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) {
                if (testNotificationBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(SproutIcons.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (testNotificationBusy) "Sending…" else "Send test notification")
            }
            testNotificationFeedback?.let { feedback ->
                val colors = MaterialTheme.colorScheme
                Surface(
                    color = if (feedback.sent) colors.primaryContainer else colors.errorContainer,
                    contentColor = if (feedback.sent) colors.onPrimaryContainer else colors.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (feedback.sent) SproutIcons.CheckCircle else SproutIcons.ErrorOutline,
                            contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(feedback.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (isAdmin) M3Card {
            Text("Accounts", style = MaterialTheme.typography.titleMedium)
            if (loading && accounts.isEmpty()) Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest, modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            if (accounts.isEmpty() && !loading && error == null) Text("No accounts available.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = (maxWidth / 100.dp).toInt().coerceIn(2, 7)
                Column {
                    filtered.chunked(columns).forEach { rowAccounts ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowAccounts.forEach { account ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClickLabel = "Open account details") { selectedAccount = account }
                                        .semantics { contentDescription = account.displayName.ifBlank { account.username } }.padding(vertical = 10.dp, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box {
                                        AccountPhotoAvatar(account, 68.dp)
                                        if (account.status == "requested") Box(Modifier.align(Alignment.TopEnd).size(11.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                                        if (account.role.equals("admin", true)) {
                                            Surface(Modifier.align(Alignment.BottomEnd).size(24.dp), shape = CircleShape, color = ForestContainer, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                                Box(contentAlignment = Alignment.Center) { Icon(SproutIcons.AdminPanelSettings, contentDescription = "Administrator", tint = Forest, modifier = Modifier.size(15.dp)) }
                                            }
                                        } else if (account.status.equals("active", true) || account.status.equals("disabled", true)) {
                                            val active = account.status.equals("active", true)
                                            Surface(
                                                Modifier.align(Alignment.BottomEnd).size(24.dp),
                                                shape = CircleShape,
                                                color = if (active) ForestContainer else MaterialTheme.colorScheme.errorContainer,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (active) SproutIcons.CheckCircle else SproutIcons.Block,
                                                        contentDescription = if (active) "Active" else "Disabled",
                                                        tint = if (active) Forest else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(columns - rowAccounts.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
            }
            }
        }
        M3Card {
            Text("App", style = MaterialTheme.typography.titleMedium)
            AboutRow(
                title = "About this App",
                description = "Version, changelog, links and privacy",
                icon = SproutIcons.Description,
                onClick = { aboutOpen = true }
            )
        }
    }
    if (editProfileNameOpen) AlertDialog(
        onDismissRequest = { editProfileNameOpen = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Edit profile name") },
        text = {
            OutlinedTextField(
                value = profileNameDraft,
                onValueChange = { profileNameDraft = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveOwnName(profileNameDraft.trim())
                editProfileNameOpen = false
            }, enabled = profileNameDraft.isNotBlank()) { Text("Save", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = { editProfileNameOpen = false }) { Text("Cancel") } }
    )
    selectedAccount?.let { selected ->
        val account = accounts.firstOrNull { it.id == selected.id } ?: selected
        var linkedParticipantId by remember(account.id, account.memberId) { mutableIntStateOf(account.memberId ?: 0) }
        var participantMenuExpanded by remember(account.id) { mutableStateOf(false) }
        val roleName = when (account.role.lowercase()) { "admin" -> "Administrator"; "scheduler" -> "Scheduler"; "viewer" -> "Viewer"; else -> account.role.replaceFirstChar { it.uppercase() } }
        val statusName = when (account.status.lowercase()) { "requested" -> "Requested"; "active" -> "Active"; "disabled" -> "Disabled"; else -> account.status.replaceFirstChar { it.uppercase() } }
        val isCurrentAccount = account.username.equals(currentUsername, true)
        val isSelf = isCurrentAccount || account.role == "admin"
        Dialog(onDismissRequest = { selectedAccount = null }) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.imePadding().padding(20.dp).heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.78f).dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountPhotoAvatar(account, 64.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(account.displayName.ifBlank { account.username }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("@" + account.username, color = Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedAccount = null }, modifier = Modifier.size(48.dp)) { Icon(SproutIcons.Close, contentDescription = "Close account details", tint = Forest, modifier = Modifier.size(20.dp)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AccountInfoLine("Role", roleName)
                    AccountInfoLine("Status", statusName, if (account.status == "requested") MaterialTheme.colorScheme.error else Forest)
                    if (!isCurrentAccount) {
                        AccountInfoLine(
                            "Last login",
                            account.lastLoginAt.takeIf { it.isNotBlank() }?.let(::formatAccountLastLogin) ?: "Never"
                        )
                    }
                    if (account.status == "active" && account.role != "admin" && !isSelf) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Linked participant", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Box {
                                OutlinedButton(onClick = { participantMenuExpanded = true }, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                                    Text(
                                        linkParticipants.firstOrNull { it.id == linkedParticipantId }?.name
                                            ?: if (account.memberId == null && linkedParticipantId == 0) "Select a participant" else "Choose participant",
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start,
                                        color = Ink,
                                        fontSize = 13.sp
                                    )
                                    Icon(SproutIcons.ArrowDropDown, contentDescription = null, tint = Forest)
                                }
                                DropdownMenu(expanded = participantMenuExpanded, onDismissRequest = { participantMenuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("No participant linked") },
                                        onClick = { linkedParticipantId = 0; participantMenuExpanded = false }
                                    )
                                    linkParticipants.forEach { participant ->
                                        DropdownMenuItem(
                                            text = { Text(participant.name) },
                                            onClick = { linkedParticipantId = participant.id; participantMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { onLinkParticipant(account, linkedParticipantId.takeIf { it > 0 }) },
                                enabled = !loading && (linkedParticipantId != (account.memberId ?: 0)),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Forest),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Save participant link") }
                        }
                    }
                    if (account.status == "requested") {
                        Text("Message for Administrator", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(color = SurfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Text(account.requestNote.ifBlank { "No message was included with this request." }, color = Ink, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    OutlinedButton(onClick = { onUploadPhoto(account) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(SproutIcons.AddAPhoto, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (account.photoDataUrl.isBlank()) "Upload account photo" else "Change account photo", color = Forest, fontSize = 12.sp)
                    }
                    if (!isSelf && account.status != "requested" && account.role != "admin") {
                        val toolLabels = listOf(
                            "home" to "Home Page", "workbook" to "Workbook", "assignments" to "Assignments",
                            "preview" to "Preview & Export", "participants" to "Participants", "history" to "Schedule History"
                        )
                        var toolAccessDraft by remember(account.id, account.toolAccess) { mutableStateOf(account.toolAccess) }
                        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = ForestContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(SproutIcons.ManageAccounts, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
                                    }
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text("Tool access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        if (account.role == "viewer") "Always read-only" else "Choose what this account can do",
                                        color = Muted, fontSize = 12.sp
                                    )
                                }
                            }
                            Surface(
                                color = SurfaceContainer.copy(alpha = 0.32f),
                                shape = MaterialTheme.shapes.medium,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    toolLabels.forEachIndexed { index, (key, label) ->
                                        val icon = when (key) {
                                            "home" -> SproutIcons.Home
                                            "workbook" -> SproutIcons.CalendarMonth
                                            "assignments" -> SproutIcons.Assignment
                                            "preview" -> SproutIcons.Preview
                                            "participants" -> SproutIcons.Groups
                                            else -> SproutIcons.History
                                        }
                                        val selectedMode = toolAccessDraft[key]
                                        Surface(
                                            color = AppSurface.copy(alpha = 0.78f),
                                            shape = MaterialTheme.shapes.medium,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                                        ) {
                                            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(19.dp))
                                                    Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        when (selectedMode) { "hidden" -> "Hidden"; "view" -> "View"; "edit" -> "Edit"; else -> "Not set" },
                                                        color = if (selectedMode == null) Muted else Forest,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val modes = if (account.role == "viewer") listOf("hidden" to "Hidden", "view" to "View") else listOf("hidden" to "Hidden", "view" to "View", "edit" to "Edit")
                                                    modes.forEach { (mode, caption) ->
                                                        FilterChip(
                                                            selected = selectedMode == mode,
                                                            onClick = { toolAccessDraft = toolAccessDraft + (key to mode) },
                                                            label = { Text(caption, fontSize = 12.sp, maxLines = 1) },
                                                            modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                                                            shape = RoundedCornerShape(12.dp),
                                                            leadingIcon = if (selectedMode == mode) ({
                                                                Icon(SproutIcons.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }) else null
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (index < toolLabels.lastIndex) Spacer(Modifier.height(1.dp))
                                    }
                                }
                            }
                            Button(
                                onClick = { onUpdateToolAccess(account, toolAccessDraft) },
                                enabled = toolAccessDraft != account.toolAccess,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = Forest)
                            ) {
                                Icon(SproutIcons.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save access settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (!isSelf) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        when (account.status) {
                            "requested" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccountActionButton("Approve", SproutIcons.CheckCircle, Forest, Modifier.weight(1f)) { beginAction(account, "approve") }
                                AccountActionButton("Decline", SproutIcons.Close, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "decline") }
                            }
                            "active" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccountActionButton("Change role", SproutIcons.ManageAccounts, Forest, Modifier.weight(1f)) { beginAction(account, "change_role") }
                                    AccountActionButton("Reset password", SproutIcons.Lock, Forest, Modifier.weight(1f)) { beginAction(account, "reset") }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccountActionButton("Disable", SproutIcons.Warning, Color(0xFFFFA726), Modifier.weight(1f)) { beginAction(account, "disable") }
                                    AccountActionButton("Delete", SproutIcons.Delete, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "delete") }
                                }
                            }
                            "disabled" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccountActionButton("Enable", SproutIcons.CheckCircle, Forest, Modifier.weight(1f)) { beginAction(account, "enable") }
                                AccountActionButton("Delete", SproutIcons.Delete, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (actionTarget != null) {
        val needsPassword = selectedAction == "approve" || selectedAction == "reset"
        val needsRole = selectedAction == "approve" || selectedAction == "change_role"
        val confirmOnly = selectedAction == "decline" || selectedAction == "disable" || selectedAction == "enable" || selectedAction == "delete"
        AlertDialog(
            onDismissRequest = { actionTarget = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge,
            title = { Text(when (selectedAction) { "approve" -> "Approve account"; "reset" -> "Reset password"; "change_role" -> "Change access role"; "decline" -> "Decline request?"; "disable" -> "Disable account?"; "delete" -> "Delete account permanently?"; else -> "Enable account?" }, fontWeight = FontWeight.Bold) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text((actionTarget?.displayName ?: "") + " · @" + (actionTarget?.username ?: ""), color = Muted, fontSize = 12.sp)
                if (needsRole) ParticipantChoice("Role", selectedRole, listOf("viewer" to "Viewer", "scheduler" to "Scheduler")) { selectedRole = it }
                if (needsPassword) OutlinedTextField(temporaryPassword, { temporaryPassword = it }, label = { Text("Temporary password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (confirmOnly) Text(when (selectedAction) { "decline" -> "This access request will be removed."; "disable" -> "This account will lose access and be signed out."; "delete" -> "This removes the login permanently and revokes its sessions. Saved schedule and audit records are retained."; else -> "This account will regain website access." }, color = Muted, fontSize = 12.sp)
            } },
            confirmButton = { TextButton(enabled = (!needsPassword || temporaryPassword.length >= 10), onClick = { actionTarget?.let { onAction(it, selectedAction, if (needsRole) selectedRole else "", temporaryPassword) }; actionTarget = null }) { Text(when (selectedAction) { "approve" -> "Approve"; "reset" -> "Set password"; "decline" -> "Decline"; "disable" -> "Disable"; "enable" -> "Enable"; "delete" -> "Delete"; else -> "Save role" }, color = if (selectedAction == "delete" || selectedAction == "decline") MaterialTheme.colorScheme.error else if (selectedAction == "disable") Color(0xFFFFA726) else Forest) } },
            dismissButton = { TextButton(onClick = { actionTarget = null }) { Text("Cancel", color = Forest) } }
        )
    }
    if (aboutOpen) AboutThisAppDialog(
        onDismiss = { aboutOpen = false },
        onOpenLink = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { Toast.makeText(context, "Walang app na mabubuksan ang link.", Toast.LENGTH_SHORT).show() }
        },
        onLicenseClick = { licenseInfoOpen = true }
    )
    if (licenseInfoOpen) AlertDialog(
        onDismissRequest = { licenseInfoOpen = false },
        title = { Text("License information") },
        text = { Text("Walang app-level LICENSE file sa repository sa ngayon, kaya hindi nakatalaga rito ang isang open-source license.") },
        confirmButton = { TextButton(onClick = { licenseInfoOpen = false }) { Text("Okay", color = Forest) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun AboutRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(SproutIcons.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutThisAppDialog(
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
    onLicenseClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = AppSurface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                TopAppBar(
                    title = { Text("About this App") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(SproutIcons.ArrowBack, contentDescription = "Back", tint = Ink)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MeetingSchedulerBrandIcon(Modifier.size(58.dp), "JW Meeting Manager")
                            Text("JW Meeting Manager", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    M3Card {
                        Text("App", style = MaterialTheme.typography.titleMedium)
                        AboutRow("Changelog", "Full version history", SproutIcons.History) {
                            onOpenLink("https://github.com/charledave2/meeting-scheduler/blob/main/CHANGELOG.md")
                        }
                        AboutRow("Contributors", "Everyone who helped build this app", SproutIcons.Groups) {
                            onOpenLink("https://github.com/charledave2/meeting-scheduler/graphs/contributors")
                        }
                    }
                    M3Card {
                        Text("Links", style = MaterialTheme.typography.titleMedium)
                        AboutRow("GitHub Repository", "Source code, issues and documentation", SproutIcons.Description) {
                            onOpenLink("https://github.com/charledave2/meeting-scheduler")
                        }
                        AboutRow("Developer", "GitHub Profile: @charledave2", SproutIcons.Person) {
                            onOpenLink("https://github.com/charledave2")
                        }
                        AboutRow("License", "License information", SproutIcons.Lock, onLicenseClick)
                    }
                    M3Card {
                        Text("Privacy", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "The app uses its connected cloud service to sync account, profile, participant and meeting schedule data with authorized users. Theme and font preferences are stored on this device. If notifications are enabled, Firebase Cloud Messaging is used to deliver assignment alerts. The app contains no ads or analytics SDK.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AccountPhotoAvatar(account: RemoteAccount, size: androidx.compose.ui.unit.Dp) {
    val bitmap = rememberAvatarBitmap(account.photoDataUrl)
    if (bitmap != null) {
        Image(bitmap, contentDescription = account.displayName + " profile photo", contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(Modifier.size(size).background(ForestContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(account.displayName.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").ifBlank { "?" }.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = (size.value * .30f).sp)
        }
    }
}

@Composable
private fun AccountInfoLine(label: String, value: String, tint: Color = Muted) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatAccountLastLogin(value: String): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return "Unknown"
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Composable
private fun AccountActionButton(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

private fun sampleParticipants() = listOf(
    Participant("Alfredo Casingal Jr", "Elder"), Participant("Aljon Orellano", "Elder"),
    Participant("Ariel Peñaroyo", "Ministerial Servant", "Regular Pioneer"), Participant("Benjamin Chavez Jr", "Elder"),
    Participant("Benjo Laurio", "Ministerial Servant"), Participant("Charle Dave Ode", "Elder", "Regular Pioneer"),
    Participant("Edward Philip Mata", "Elder"), Participant("Glenn Asombrado", "Elder"),
    Participant("Abegail Hamtig", "Publisher", "Regular Pioneer", "Sister"), Participant("Aliza Solis", "Publisher", "Publisher", "Sister"),
    Participant("Andre Orellano"), Participant("Angelica Casingal", "Publisher", "Regular Pioneer", "Sister"),
    Participant("James Simon Pascual", "Elder"), Participant("Jerome Ontog", "Elder"), Participant("Lucio Esquerdo", "Ministerial Servant")
)

private fun String.htmlSafe(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

private fun sampleAssignments() = listOf(
    Assignment("1. “Makikipagtipan Ako ng Isang Bagong Tipan”", section="treasures", time="6:06", duration="10 min.", id=1),
    Assignment("2. Espirituwal na Hiyas", section="treasures", time="6:16", duration="10 min.", id=2),
    Assignment("3. Pagbabasa ng Bibliya — Jeremias 31:1-11", section="treasures", time="6:26", duration="4 min.", id=3, hasSecondary=true),
    Assignment("4. Pagpapasimula ng Pakikipag-usap — Bahay-bahay", section="ministry", time="6:31", duration="3 min.", id=4, hasAssistant=true, hasSecondary=true, hasSecondaryAssistant=true),
    Assignment("5. Pagpapasimula ng Pakikipag-usap — Di-pormal", section="ministry", time="6:35", duration="4 min.", id=5, hasAssistant=true, hasSecondary=true, hasSecondaryAssistant=true),
    Assignment("6. Ipaliwanag ang Paniniwala Mo", section="ministry", time="6:40", duration="5 min.", id=6, hasSecondary=true),
    Assignment("7. Makibagay—Gamitin ang JW.ORG", section="living", time="6:46", duration="15 min.", id=7),
    Assignment("8. Pag-aaral ng Kongregasyon sa Bibliya", section="living", time="7:04", duration="30 min.", id=8, hasAssistant=true)
)

private fun sampleHistory() = mutableListOf(
    SavedSchedule("Aug 31 – Sep 6, 2026", "Jeremias 31", "Aug 24, 2026", "2026-08-31"),
    SavedSchedule("Aug 24 – 30, 2026", "Jeremias 30", "Aug 17, 2026", "2026-08-24"),
    SavedSchedule("Aug 17 – 23, 2026", "Jeremias 29", "Aug 10, 2026", "2026-08-17")
)




private fun renderSchedulePng(context: Context, exportHtml: String, onSuccess: (Intent) -> Unit, onFailure: (String) -> Unit) {
    val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
    val outputWidth = SCHEDULE_EXPORT_WIDTH_PX
    val outputHeight = SCHEDULE_EXPORT_HEIGHT_PX
    var hostContext: Context = context
    while (hostContext !is android.app.Activity && hostContext is android.content.ContextWrapper) {
        val base = hostContext.baseContext
        if (base === hostContext) break
        hostContext = base
    }
    val activityRoot = (hostContext as? android.app.Activity)?.findViewById<android.view.ViewGroup>(android.R.id.content)
    if (activityRoot == null) {
        onFailure("Hindi maihanda ang PNG renderer. Isara at buksan muli ang Preview & Export.")
        return
    }
    val renderer = WebView(context)
    renderer.visibility = View.INVISIBLE
    activityRoot.addView(renderer, android.widget.FrameLayout.LayoutParams(outputWidth, outputHeight))
    var captureStarted = false
    fun removeRenderer(view: WebView) {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        runCatching { view.stopLoading(); view.destroy() }
    }
    fun captureRenderedSchedule(view: WebView) {
        if (captureStarted) return
        captureStarted = true
        try {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(outputWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(outputHeight, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, outputWidth, outputHeight)
            val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            bitmap.density = SCHEDULE_EXPORT_DPI
            view.visibility = View.VISIBLE
            view.draw(Canvas(bitmap))
            view.visibility = View.INVISIBLE
            removeRenderer(view)
            val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, "meeting-schedule.png")
            Thread {
                val saved = runCatching {
                    FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = android.content.ClipData.newUri(context.contentResolver, "Meeting schedule PNG", uri)
                    }
                    intent
                }
                bitmap.recycle()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    saved.onSuccess { intent -> onSuccess(intent) }
                        .onFailure { onFailure("Hindi mabuo ang export image: ${it.message ?: "subukan muli"}") }
                }
            }.start()
        } catch (error: Exception) {
            removeRenderer(view)
            onFailure("Hindi mabuo ang export image: ${error.message ?: "subukan muli"}")
        }
    }
    renderer.apply {
        setBackgroundColor(android.graphics.Color.WHITE)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean = true
            override fun onPageFinished(view: WebView, url: String?) {
                fitScheduleWebView(
                    view = view,
                    targetWidthPx = outputWidth,
                    targetHeightPx = outputHeight,
                    density = density,
                    onReady = { captureRenderedSchedule(view) },
                    onFailure = {
                        if (!captureStarted) {
                            captureStarted = true
                            removeRenderer(view)
                            onFailure("Hindi masukat ang buong schedule para sa PNG. Subukan muli.")
                        }
                    }
                )
            }
        }
        measure(
            View.MeasureSpec.makeMeasureSpec(outputWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(outputHeight, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, outputWidth, outputHeight)
        loadDataWithBaseURL("file:///android_asset/", exportHtml, "text/html", "UTF-8", null)
        postDelayed({
            if (!captureStarted) {
                captureStarted = true
                removeRenderer(this)
                onFailure("Hindi natapos ang pagbuo ng PNG. Suriin ang preview at subukan muli.")
            }
        }, 12_000L)
    }
}


private fun SavedSchedule.toLocalJson() = JSONObject()
    .put("week", week).put("saved", saved).put("weekStart", weekStart)
    .put("reading", reading).put("openingSong", openingSong).put("closingSong", closingSong)
    .put("chairman", chairman).put("openingPrayer", openingPrayer)
    .put("adviser", adviser).put("audienceGroup", audienceGroup)
    .put("assignments", JSONArray().apply { assignments.forEach { put(it.toJson()) } })

private fun JSONArray.localObjects() = (0 until length()).map { getJSONObject(it) }

private fun JSONObject.toLocalSchedule(): SavedSchedule {
    val date = LocalDate.parse(getString("weekStart"))
    require(date.dayOfWeek == java.time.DayOfWeek.MONDAY)
    return SavedSchedule(
        getString("week"), getString("reading"), getString("saved"), date.toString(),
        getString("openingSong"), getString("closingSong"),
        getJSONArray("assignments").localObjects().map { it.toAssignment() },
        getString("chairman"), getString("openingPrayer"), getString("adviser"), getString("audienceGroup")
    )
}

private fun Participant.toLocalJson() = JSONObject()
    .put("id", id).put("name", name).put("appointment", appointment).put("service", service)
    .put("gender", gender).put("enabled", enabled).put("unbaptized", unbaptized)

private fun JSONObject.toLocalParticipant() = Participant(
    getString("name"), getString("appointment"), getString("service"), getString("gender"),
    getBoolean("enabled"), getInt("id"), getBoolean("unbaptized")
).also { require(it.id > 0 && it.name.isNotBlank()) }

private data class DeviceWorkspace(
    val name: String,
    val meetingDay: java.time.DayOfWeek,
    val participants: List<Participant> = emptyList(),
    val drafts: List<SavedSchedule> = emptyList(),
    val history: List<SavedSchedule> = emptyList(),
    val downloads: List<SavedSchedule> = emptyList(),
    val selectedWeek: String = ""
) {
    fun toJson() = JSONObject()
        .put("format", "jw-meeting-manager-local").put("version", 1)
        .put("name", name).put("meetingDay", meetingDay.name).put("selectedWeek", selectedWeek)
        .put("participants", JSONArray().apply { participants.forEach { put(it.toLocalJson()) } })
        .put("drafts", JSONArray().apply { drafts.forEach { put(it.toLocalJson()) } })
        .put("history", JSONArray().apply { history.forEach { put(it.toLocalJson()) } })
        .put("downloads", JSONArray().apply { downloads.forEach { put(it.toLocalJson()) } })

    companion object {
        fun read(raw: String): DeviceWorkspace {
            val json = JSONObject(raw)
            require(json.getString("format") == "jw-meeting-manager-local" && json.getInt("version") == 1)
            val name = json.getString("name").trim()
            require(name.isNotBlank() && name.length <= 80)
            val selectedWeek = json.getString("selectedWeek")
            require(selectedWeek.isEmpty() || LocalDate.parse(selectedWeek).dayOfWeek == java.time.DayOfWeek.MONDAY)
            val people = json.getJSONArray("participants").localObjects().map { it.toLocalParticipant() }
            require(people.map { it.id }.distinct().size == people.size)
            fun schedules(key: String) = json.getJSONArray(key).localObjects()
                .map { it.toLocalSchedule() }.also { entries ->
                    require(entries.map { it.weekStart }.distinct().size == entries.size)
                }
            return DeviceWorkspace(
                name, java.time.DayOfWeek.valueOf(json.getString("meetingDay")), people,
                schedules("drafts"), schedules("history"), schedules("downloads"), selectedWeek
            )
        }
    }
}


private val participantCsvColumns = listOf("Name", "Gender", "Appointment", "Service Status", "Baptism Status", "Status")
private const val participantCsvLimit = 2 * 1024 * 1024
private const val participantCsvRowLimit = 3000

private data class ParticipantCsvRow(val line: Int, val fields: List<String>)
private data class ParticipantImportPreview(val entries: List<Participant>, val issues: List<String>, val duplicates: Int)

private fun participantImportNameKey(name: String): String =
    name.trim().replace(Regex("""\s+"""), " ").lowercase(Locale.ROOT)

private fun readParticipantCsv(input: java.io.InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val size = input.read(buffer)
        if (size < 0) break
        require(output.size() + size <= participantCsvLimit) { "Use a CSV file under 2 MB." }
        output.write(buffer, 0, size)
    }
    return try {
        Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(output.toByteArray())).toString().removePrefix("\uFEFF")
    } catch (_: java.nio.charset.CharacterCodingException) {
        throw IllegalArgumentException("Save the template as CSV UTF-8 and try again.")
    }
}

private fun parseParticipantCsvRows(text: String): List<ParticipantCsvRow> {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    val delimiter = if (';' in firstLine && ',' !in firstLine) ';' else ','
    val rows = mutableListOf<ParticipantCsvRow>()
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var afterQuote = false
    var line = 1
    var rowLine = 1
    var index = 0

    fun endField() {
        fields.add(field.toString().trim())
        field.setLength(0)
        afterQuote = false
    }
    fun endRow() {
        endField()
        if (fields.any { it.isNotBlank() }) {
            require(rows.size <= participantCsvRowLimit) { "Import up to $participantCsvRowLimit participants per file." }
            rows.add(ParticipantCsvRow(rowLine, fields.toList()))
        }
        fields.clear()
    }

    while (index < text.length) {
        val char = text[index++]
        val newline = char == '\r' || char == '\n'
        if (char == '\r' && text.getOrNull(index) == '\n') index++
        when {
            quoted && char == '"' && text.getOrNull(index) == '"' -> {
                field.append('"')
                index++
            }
            quoted && char == '"' -> {
                quoted = false
                afterQuote = true
            }
            quoted -> field.append(if (newline) '\n' else char)
            char == delimiter -> endField()
            newline -> endRow()
            afterQuote && (char == ' ' || char == '\t') -> Unit
            afterQuote -> throw IllegalArgumentException("Line $line: unexpected text after a closing quote.")
            char == '"' && field.isEmpty() -> quoted = true
            char == '"' -> throw IllegalArgumentException("Line $line: put quotes around the whole CSV field.")
            else -> field.append(char)
        }
        if (newline) {
            line++
            if (!quoted) rowLine = line
        }
    }
    require(!quoted) { "Line $rowLine: missing closing quote." }
    endRow()
    return rows
}

private fun parseParticipantImport(text: String, existing: List<Participant>): ParticipantImportPreview {
    val rows = parseParticipantCsvRows(text)
    require(rows.isNotEmpty()) { "The CSV is empty. Download the participant template to get started." }
    val expected = participantCsvColumns.map { it.lowercase(Locale.ROOT) }
    // Older templates had a UTF-8 BOM. Some editors save its misdecoded bytes as header text.
    val legacyBom = Regex("""^(?:\uFEFF|\u00EF\u00BB\u00BF|\u00C3\u00AF\u00C2\u00BB\u00C2\u00BF)+""")
    val columns = rows.first().fields
        .map { it.replaceFirst(legacyBom, "").trim().lowercase(Locale.ROOT) }
        .dropLastWhile { it.isEmpty() }
    require(columns.distinct().size == columns.size && columns.all { it in expected } &&
        expected.take(2).all { it in columns }) {
        "The CSV must have Name and Gender headers. Use the template’s column names for optional details."
    }
    val positions = expected.map(columns::indexOf)
    val names = existing.mapTo(mutableSetOf()) { participantImportNameKey(it.name) }
    val result = mutableListOf<Participant>()
    val issues = mutableListOf<String>()
    var duplicates = 0

    for ((line, fields) in rows.drop(1)) {
        if (fields.drop(columns.size).any { it.isNotBlank() }) {
            issues += "Line $line: found values with no matching header."
            continue
        }
        fun field(index: Int) = fields.getOrNull(positions[index]).orEmpty()
        val name = field(0)
        fun value(index: Int) = field(index).lowercase(Locale.ROOT).replace('_', ' ')
        val gender = value(1)
        val rawAppointment = value(2)
        val rawService = value(3)
        val baptism = value(4)
        val status = value(5)
        val unbaptized = baptism == "unbaptized"
        val appointment = when (rawAppointment) {
            "", "publisher" -> "publisher"
            "ministerial", "ministerial servant" -> "ministerial"
            "elder" -> "elder"
            else -> null
        }
        val service = when (rawService) {
            "", "none", "publisher" -> "none"
            "regular pioneer" -> "regular_pioneer"
            else -> null
        }
        val issue = when {
            name.isBlank() || name.length > 120 || name.any { it.isISOControl() } -> "Enter a name of 1–120 characters."
            gender !in setOf("brother", "sister") -> "Gender must be Brother or Sister."
            appointment == null -> "Use Publisher, Ministerial servant, or Elder for Appointment."
            service == null -> "Use Publisher or Regular pioneer for Service Status."
            baptism !in setOf("", "baptized", "unbaptized") -> "Use Baptized or Unbaptized for Baptism Status."
            status !in setOf("", "active", "inactive") -> "Use Active or Inactive for Status."
            (unbaptized || gender == "sister") && appointment != "publisher" -> "Elder and Ministerial servant require a baptized brother."
            unbaptized && service == "regular_pioneer" -> "An unbaptized participant can’t be marked as a regular pioneer."
            else -> null
        }
        if (issue != null) {
            issues += "Line $line: $issue"
        } else if (!names.add(participantImportNameKey(name))) {
            duplicates++
        } else {
            result += Participant(
                name = name, gender = gender, enabled = status != "inactive", unbaptized = unbaptized,
                appointment = if (unbaptized) "" else requireNotNull(appointment),
                service = if (unbaptized) "" else requireNotNull(service)
            )
        }
    }
    return ParticipantImportPreview(result, issues, duplicates)
}

private fun List<SavedSchedule>.withLocalSchedule(value: SavedSchedule) =
    filterNot { it.weekStart == value.weekStart } + value

private fun localWeek(date: LocalDate) = date.minusDays((date.dayOfWeek.value - 1).toLong())

private fun nextLocalMeetingWeek(today: LocalDate, day: java.time.DayOfWeek): LocalDate {
    val week = localWeek(today)
    return if (week.plusDays((day.value - 1).toLong()).isBefore(today)) week.plusWeeks(1) else week
}

private fun blankLocalSchedule(week: LocalDate) = SavedSchedule(
    week = week.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)),
    reading = "", saved = "", weekStart = week.toString(), openingSong = "", closingSong = ""
)

@Composable
private fun DeviceWorkspaceApp(
    onOpenShared: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    paletteStyle: AppPaletteStyle,
    onPaletteStyleChange: (AppPaletteStyle) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    appLock: AppLockController
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("meeting_scheduler_device_only", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var workspace by remember { mutableStateOf(preferences.getString("workspace", null)?.let { runCatching { DeviceWorkspace.read(it) }.getOrNull() }) }
    var pageName by remember { mutableStateOf(Screen.HOME.name) }
    val page = Screen.valueOf(pageName)
    var toolsOpen by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }
    var workspacePickerOpen by remember { mutableStateOf(false) }
    var captureWeek by remember { mutableStateOf<String?>(null) }
    var captureBusy by remember { mutableStateOf(false) }
    var captureLog by remember { mutableStateOf(emptyList<String>()) }
    var captureResult by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var fileBusy by remember { mutableStateOf(false) }
    var localExportType by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<DeviceWorkspace?>(null) }
    var pendingCreate by remember { mutableStateOf<DeviceWorkspace?>(null) }
    var participantImportOpen by rememberSaveable { mutableStateOf(false) }
    var participantImportMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var participantImport by remember { mutableStateOf<ParticipantImportPreview?>(null) }
    var previewSnapshot by remember { mutableStateOf<SavedSchedule?>(null) }
    val previewWebView = remember { mutableStateOf<WebView?>(null) }
    val available = remember { Screen.entries.toSet() }

    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun update(value: DeviceWorkspace) {
        preferences.edit().putString("workspace", value.toJson().toString()).apply()
        workspace = value
    }
    fun navigate(next: Screen) {
        previewSnapshot = null
        pageName = next.name
    }


    val downloadParticipantTemplate = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && !fileBusy) scope.launch {
            fileBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Couldn’t open the template file.")
                    stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(participantCsvColumns.joinToString(","))
                        writer.write("\r\n")
                    }
                }
                participantImportMessage = "Participant template saved." to false
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                participantImportMessage = "Couldn’t save the participant template." to true
            } finally {
                fileBusy = false
            }
        }
    }
    val chooseParticipantCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val existing = workspace?.participants
        if (uri != null && existing != null && !fileBusy) scope.launch {
            fileBusy = true
            try {
                participantImport = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Couldn’t open the CSV file.")
                    parseParticipantImport(stream.use(::readParticipantCsv), existing)
                }
                participantImportOpen = false
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val detail = if (error is IllegalArgumentException) error.message ?: "Invalid CSV file." else "Couldn’t read the participant CSV file."
                participantImportMessage = detail to true
            } finally {
                fileBusy = false
            }
        }
    }

    if (participantImportOpen) AlertDialog(
        onDismissRequest = { if (!fileBusy) participantImportOpen = false },
        title = { Text("Import participants") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Download the template, fill in one participant per row, and save it as CSV UTF-8.")
                Text("Name and Gender are required. The other template columns are optional.")
                Text("Gender: Brother or Sister\nAppointment: Publisher, Ministerial servant, or Elder\nService Status: Publisher or Regular pioneer\nBaptism Status: Baptized or Unbaptized\nStatus: Active or Inactive",
                    color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Blank optional fields default to Publisher, Baptized, and Active. Existing names are skipped.",
                    color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { downloadParticipantTemplate.launch("participants-template.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !fileBusy
                ) { Text("Download template") }
                participantImportMessage?.let { (detail, error) ->
                    Text(detail, color = if (error) MaterialTheme.colorScheme.error else Muted,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (fileBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !fileBusy, onClick = {
                participantImportMessage = null
                chooseParticipantCsv.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
            }) { Text("Choose CSV file") }
        },
        dismissButton = { TextButton(enabled = !fileBusy, onClick = { participantImportOpen = false }) { Text("Cancel") } }
    )

    participantImport?.let { preview ->
        AlertDialog(
            onDismissRequest = { participantImport = null },
            title = { Text("Review participant import") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${preview.entries.size} ready to import · ${preview.duplicates} duplicate names · ${preview.issues.size} invalid rows")
                    preview.entries.take(8).forEach { person ->
                        Column {
                            Text(person.name, style = MaterialTheme.typography.titleSmall)
                            val details = buildList {
                                add(genderLabel(person.gender))
                                if (person.unbaptized) add("Unbaptized")
                                else {
                                    add(appointmentLabel(person.appointment))
                                    if (person.service == "regular_pioneer") add(serviceLabel(person.service))
                                }
                                if (!person.enabled) add("Inactive")
                            }
                            Text(details.joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (preview.entries.size > 8) Text("And ${preview.entries.size - 8} more.", color = Muted)
                    if (preview.issues.isNotEmpty()) {
                        Text("Invalid rows will be skipped.", color = MaterialTheme.colorScheme.error)
                        preview.issues.take(8).forEach { issue ->
                            Text(issue, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (preview.issues.size > 8) Text("And ${preview.issues.size - 8} more invalid rows.", color = Muted)
                    }
                    if (preview.entries.isEmpty() && preview.issues.isEmpty() && preview.duplicates == 0) {
                        Text("The template has no participant rows. Add names and try again.", color = Muted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (preview.entries.isNotEmpty()) {
                        val latest = workspace
                        if (latest != null) {
                            val names = latest.participants.mapTo(mutableSetOf()) { participantImportNameKey(it.name) }
                            val newEntries = preview.entries.filter { names.add(participantImportNameKey(it.name)) }
                            val lastId = latest.participants.maxOfOrNull { it.id } ?: 0
                            if (newEntries.size > Int.MAX_VALUE - lastId) {
                                message("Couldn’t assign participant IDs. No participants were imported.")
                            } else if (newEntries.isEmpty()) {
                                message("No new participants to import.")
                            } else {
                                update(latest.copy(participants = latest.participants + newEntries.mapIndexed { index, person ->
                                    person.copy(id = lastId + index + 1)
                                }))
                                message("Imported ${newEntries.size} participants on this device.")
                            }
                        }
                    }
                    participantImport = null
                }) { Text(if (preview.entries.isEmpty()) "Done" else "Import ${preview.entries.size}") }
            },
            dismissButton = {
                if (preview.entries.isNotEmpty()) TextButton(onClick = { participantImport = null }) { Text("Cancel") }
            }
        )
    }

    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val snapshot = workspace
        if (uri != null && snapshot != null) scope.launch {
            fileBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn’t open backup file.")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(snapshot.toJson().toString()) }
                }
                message("Workspace backup saved.")
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                message("Couldn’t save the backup.")
            } finally { fileBusy = false }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !captureBusy) scope.launch {
            fileBusy = true
            try {
                val restored = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: error("Couldn’t open backup file.")
                    val bytes = input.use { stream ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 16 * 1024 * 1024)
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    DeviceWorkspace.read(bytes.toString(Charsets.UTF_8))
                }
                if (workspace == null && !preferences.contains("workspace")) {
                    update(restored)
                    navigate(Screen.HOME)
                    message("Workspace restored.")
                } else pendingRestore = restored
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                message("Couldn’t read this workspace backup.")
            } finally { fileBusy = false }
        }
    }
    pendingRestore?.let { restored ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace Local Workspace?") },
            text = { Text("Restore ${restored.name} and replace the participants, drafts, history, and downloads currently on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    update(restored)
                    pendingRestore = null
                    navigate(Screen.HOME)
                    message("Workspace restored.")
                }) { Text("Replace and restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }
    pendingCreate?.let { replacement ->
        AlertDialog(
            onDismissRequest = { pendingCreate = null },
            title = { Text("Start a new workspace?") },
            text = { Text("The workspace saved on this device couldn’t be opened. Creating a new one replaces that copy. Choose Restore backup on the setup screen to recover from a backup.") },
            confirmButton = {
                TextButton(onClick = {
                    update(replacement)
                    pendingCreate = null
                    navigate(Screen.HOME)
                }) { Text("Create new workspace") }
            },
            dismissButton = { TextButton(onClick = { pendingCreate = null }) { Text("Cancel") } }
        )
    }
    val data = workspace
    if (workspacePickerOpen && data != null) WorkspacePickerDialog(
        localActive = true,
        localName = data.name,
        onDismiss = { workspacePickerOpen = false },
        canSwitch = !fileBusy && !captureBusy && localExportType == null,
        onSwitch = { workspacePickerOpen = false; onOpenShared() }
    )
    BackHandler(enabled = page != Screen.HOME && data != null) {
        navigate(if (page == Screen.PREVIEW && previewSnapshot == null) Screen.ASSIGNMENTS else Screen.HOME)
    }
    Scaffold(
        containerColor = AppSurface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (data != null) PreviewBottomNavigation(
                current = page, available = available, onNavigate = ::navigate,
                onCenterClick = { profileOpen = false; toolsOpen = true },
                profileName = data.name, profileRole = "Local workspace",
                profileAvatar = { size -> ProfileAvatar(data.name, null, size) },
                profileMenuOpen = profileOpen, onProfileMenuToggle = { profileOpen = !profileOpen },
                onProfileMenuDismiss = { profileOpen = false },
                onProfileSettings = { profileOpen = false; navigate(Screen.ACCOUNTS) },
                onProfileSignOut = null,
                onProfileWorkspace = { profileOpen = false; workspacePickerOpen = true }
            )
        }
    ) { insets ->
        val modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)
        if (data == null) {
            LocalCongregationSetup(
                modifier = modifier,
                restoring = fileBusy,
                needsRecovery = preferences.contains("workspace"),
                onRestore = { restore.launch(arrayOf("application/json", "application/octet-stream")) },
                onOpenShared = onOpenShared,
                onCreate = { name, day ->
                    val nextWeek = nextLocalMeetingWeek(LocalDate.now(), day)
                    val created = DeviceWorkspace(name, day, selectedWeek = nextWeek.toString())
                    if (preferences.contains("workspace")) pendingCreate = created
                    else {
                        update(created)
                        navigate(Screen.HOME)
                    }
                }
            )
            return@Scaffold
        }
        if (toolsOpen) PreviewToolsSheet(available, page, { toolsOpen = false }) {
            toolsOpen = false
            navigate(it)
        }
        val selected = data.selectedWeek.takeIf { it.isNotBlank() } ?: localWeek(LocalDate.now()).toString()
        val draft = data.drafts.firstOrNull { it.weekStart == selected } ?: blankLocalSchedule(LocalDate.parse(selected))
        fun edit(schedule: SavedSchedule) {
            val latest = workspace ?: return
            update(latest.copy(selectedWeek = schedule.weekStart, drafts = latest.drafts.withLocalSchedule(schedule)))
        }
        fun viewSaved(schedule: SavedSchedule) { previewSnapshot = schedule; pageName = Screen.PREVIEW.name }
        when (page) {
            Screen.HOME -> LocalWorkspaceHome(
                modifier = modifier,
                workspace = data,
                onOpenWeek = { week ->
                    update(data.copy(selectedWeek = week))
                    navigate(Screen.DETAILS)
                },
                onPreview = ::viewSaved
            )
            Screen.HISTORY -> HistoryScreen(
                modifier = modifier,
                schedules = data.history.map { it.workspaceEntry() },
                loading = false, error = null, hasMore = false, loadingMore = false,
                availableYears = emptyList(), onLoadMore = {},
                isAdmin = true, canEdit = true, canCreate = true,
                onView = ::viewSaved,
                onEdit = { item ->
                    if (data.drafts.none { it.weekStart == item.weekStart }) edit(item)
                    else update(data.copy(selectedWeek = item.weekStart))
                    navigate(Screen.DETAILS)
                },
                onDelete = { item ->
                    workspace?.let { latest ->
                        update(latest.copy(history = latest.history.filterNot { it.weekStart == item.weekStart }))
                    }
                },
                onRefresh = null,
                onNew = {
                    val nextWeek = nextLocalMeetingWeek(LocalDate.now(), data.meetingDay)
                    update(data.copy(selectedWeek = nextWeek.toString()))
                    navigate(Screen.DETAILS)
                },
                local = true
            )
            Screen.DETAILS -> DeviceWorkbook(
                modifier, draft, data.downloads,
                captureBusy = captureBusy,
                captureLog = if (captureWeek == selected) captureLog else emptyList(),
                captureResult = if (captureWeek == selected) captureResult else null,
                capturingSelectedWeek = captureBusy && captureWeek == selected,
                onSelectWeek = { week -> update(data.copy(selectedWeek = week.toString())) },
                onEdit = ::edit,
                onCapture = {
                    if (!captureBusy && !fileBusy) {
                        val week = draft.weekStart
                        captureWeek = week
                        captureBusy = true
                        captureLog = emptyList()
                        captureResult = null
                        scope.launch {
                            try {
                                val result = SchedulerApi(context).capturePublicWorkbook(week) { line ->
                                    withContext(Dispatchers.Main.immediate) {
                                        captureLog = (captureLog + line).takeLast(10)
                                    }
                                }
                                val template = blankLocalSchedule(LocalDate.parse(week)).copy(
                                    reading = result.meta.optString("reading"),
                                    openingSong = result.meta.optString("openingSong"),
                                    closingSong = result.meta.optString("closingSong"),
                                    assignments = result.parts.map { it.toAssignment() }
                                )
                                val latest = workspace ?: return@launch
                                val existingDraft = latest.drafts.firstOrNull { it.weekStart == week }
                                val applyNow = existingDraft == null || !existingDraft.hasLocalWorkbookContent()
                                update(latest.copy(
                                    drafts = if (applyNow) latest.drafts.withLocalSchedule(template.withLocalRoles(existingDraft)) else latest.drafts,
                                    downloads = latest.downloads.withLocalSchedule(template)
                                ))
                                captureResult = (if (applyNow) "Workbook captured and applied to the draft. Available offline."
                                    else "Workbook captured and saved offline. Apply it when you’re ready.") to true
                            } catch (error: Exception) {
                                if (error is kotlinx.coroutines.CancellationException) throw error
                                val detail = error.message ?: "Check your connection and try again."
                                captureLog = (captureLog + detail).takeLast(10)
                                captureResult = "Couldn’t capture this workbook. Check your connection and try again." to false
                            } finally { captureBusy = false }
                        }
                    }
                },
                onNext = { navigate(Screen.ASSIGNMENTS) }
            )
            Screen.ASSIGNMENTS -> AssignmentsScreen(
                modifier = modifier, parts = draft.assignments, participants = data.participants,
                rotation = emptyMap(), rotationBusy = false, rotationError = null,
                chairman = draft.chairman, openingPrayer = draft.openingPrayer,
                adviser = draft.adviser, audienceGroup = draft.audienceGroup, isAdmin = true,
                readOnly = false, canContinueExport = true, cloudSaveBusy = false, cloudSaveFeedback = null,
                onRole = { role, value ->
                    val updated = draft.withoutLocalParticipant(value)
                    edit(when (role) {
                        "chairman" -> updated.copy(chairman = value)
                        "opening_prayer" -> updated.copy(openingPrayer = value)
                        "adviser" -> updated.copy(adviser = value)
                        else -> draft.copy(audienceGroup = value)
                    })
                },
                onAssign = { index, slot, name ->
                    val updated = draft.withoutLocalParticipant(name)
                    val parts = updated.assignments.toMutableList()
                    assignParticipantToOneSlot(parts, index, slot, name)
                    edit(updated.copy(assignments = parts))
                },
                onSaveCloud = {
                    val saved = draft.copy(saved = Instant.now().toString())
                    update(data.copy(history = data.history.withLocalSchedule(saved)))
                    message("Schedule saved to this device.")
                },
                onContinueExport = { navigate(Screen.PREVIEW) },
                saveToDevice = true
            )
            Screen.PREVIEW -> {
                val item = previewSnapshot ?: draft
                fun shareFile(type: String, title: String, createIntent: suspend () -> Intent) {
                    if (localExportType != null) return
                    localExportType = type
                    scope.launch {
                        try {
                            context.startActivity(Intent.createChooser(createIntent(), title))
                        } catch (error: Exception) {
                            if (error is kotlinx.coroutines.CancellationException) throw error
                            message(error.message ?: "Couldn’t share this export. Try again.")
                        } finally {
                            localExportType = null
                        }
                    }
                }
                PreviewScreen(
                    modifier, item.weekStart, item.reading, item.openingSong, item.closingSong,
                    item.assignments, item.chairman, item.openingPrayer, item.adviser, item.audienceGroup,
                    previewWebView, true, localExportType,
                    onExportText = {
                        shareFile("text", "Share schedule text") {
                            val text = buildOfficialScheduleText(
                                item.weekStart, item.reading, item.openingSong, item.closingSong,
                                item.assignments, item.chairman, item.openingPrayer, item.adviser,
                                item.audienceGroup, useLocalClock = true
                            )
                            scheduleTextShareIntent(context, text, item.weekStart, item.week)
                        }
                    },
                    onPng = {
                        if (localExportType == null) {
                            localExportType = "png"
                            renderSchedulePng(context, buildOfficialScheduleHtml(
                                context, item.weekStart, item.reading, item.openingSong, item.closingSong,
                                item.assignments, item.chairman, item.openingPrayer, item.adviser, item.audienceGroup,
                                exportMode = true, useLocalClock = true
                            ), onSuccess = { intent ->
                                localExportType = null
                                context.startActivity(Intent.createChooser(intent, "Share schedule image"))
                            }, onFailure = { error ->
                                localExportType = null
                                message(error)
                            })
                        }
                    },
                    onAssignmentSlipImages = {
                        val date = LocalDate.parse(item.weekStart).plusDays((data.meetingDay.value - 1).toLong())
                        val slips = buildAssignmentSlips(date, item.assignments, item.chairman, item.openingPrayer, item.adviser)
                        if (slips.isEmpty()) message("Assign a participant before exporting assignment slips.")
                        else shareFile("slips_image", "Share assignment slip images") {
                            assignmentSlipsShareIntent(context, slips, item.weekStart, item.week)
                        }
                    },
                    useLocalClock = true
                )
            }
            Screen.PARTICIPANTS -> {
                fun savePeople(people: List<Participant>) { update(data.copy(participants = people)) }
                fun setActive(person: Participant, enabled: Boolean) {
                    savePeople(data.participants.map { if (it.id == person.id) it.copy(enabled = enabled) else it })
                }
                ParticipantsScreen(
                    modifier = modifier, participants = data.participants,
                    onAdd = { name ->
                        if (data.participants.any { it.name.equals(name.trim(), true) }) message("A participant already uses that name.")
                        else savePeople(data.participants + Participant(
                            name = name.trim(), appointment = "publisher", service = "none", gender = "brother",
                            id = (data.participants.maxOfOrNull { it.id } ?: 0) + 1
                        ))
                    },
                    onToggle = {},
                    onEdit = { old, updated ->
                        if (data.participants.any { it.id != old.id && it.name.equals(updated.name, true) }) {
                            message("A participant already uses that name.")
                        } else {
                            update(data.copy(
                                participants = data.participants.map { if (it.id == old.id) updated.copy(id = old.id) else it },
                                drafts = data.drafts.map { it.renameLocalParticipant(old.name, updated.name) }
                            ))
                        }
                    },
                    isAdmin = true, canEdit = true, loading = false, loadError = null,
                    onRefresh = {}, onSetActive = ::setActive,
                    onDelete = { person ->
                        update(data.copy(
                            participants = data.participants.filterNot { it.id == person.id },
                            drafts = data.drafts.map { it.withoutLocalParticipant(person.name) }
                        ))
                    },
                    showRefresh = false,
                    onImportParticipants = { participantImportMessage = null; participantImportOpen = true },
                    participantImportBusy = fileBusy
                )
            }
            Screen.ACCOUNTS -> PageColumn(modifier) {
                PageHeading("Account & Settings", "Local Workspace")
                DeviceWorkspaceFields(data, "Save workspace settings") { name, day ->
                    update(data.copy(name = name, meetingDay = day))
                    message("Workspace settings saved.")
                }
                WorkspacePreferencesCard(
                    themeMode, onThemeModeChange, paletteStyle, onPaletteStyleChange,
                    fontScale, onFontScaleChange, appLock
                )
                M3Card {
                    Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
                    Text("Move your local participants, drafts, and schedules to another device.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    AdaptivePair(
                        { field -> OutlinedButton(modifier = field, enabled = !fileBusy,
                            onClick = { backup.launch("jw-meeting-workspace.json") }) { Text("Create backup") } },
                        { field -> OutlinedButton(modifier = field, enabled = !fileBusy && !captureBusy,
                            onClick = { restore.launch(arrayOf("application/json", "application/octet-stream")) }) { Text("Restore backup") } }
                    )
                }
                M3Card {
                    Text("Workspace", style = MaterialTheme.typography.titleMedium)
                    Text("Local Workspace · Active", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { workspacePickerOpen = true }) { Text("Switch workspace") }
                }
            }
        }
    }
}

private fun SavedSchedule.withoutLocalParticipant(name: String): SavedSchedule = renameLocalParticipant(name, "")

private fun SavedSchedule.renameLocalParticipant(from: String, to: String): SavedSchedule {
    if (from.isBlank()) return this
    fun replace(value: String) = if (value.trim().equals(from.trim(), true)) to else value
    return copy(
        chairman = replace(chairman), openingPrayer = replace(openingPrayer), adviser = replace(adviser),
        assignments = assignments.map {
            it.copy(person = replace(it.person), assistant = replace(it.assistant),
                secondaryAssignee = replace(it.secondaryAssignee), secondaryAssistant = replace(it.secondaryAssistant))
        }
    )
}

@Composable
private fun WorkspacePickerDialog(
    localActive: Boolean,
    localName: String?,
    onDismiss: () -> Unit,
    canSwitch: Boolean,
    onSwitch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (localActive) "Local Workspace · Active" else "Shared Workspace · Active",
                    style = MaterialTheme.typography.titleSmall, color = Forest)
                if (localName != null) Text(localName, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onSwitch, enabled = canSwitch) {
                Text(when {
                    localActive -> "Switch to Shared"
                    localName != null -> "Switch to Local"
                    else -> "Set up Local Workspace"
                })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LocalCongregationSetup(
    modifier: Modifier,
    restoring: Boolean,
    needsRecovery: Boolean,
    onRestore: () -> Unit,
    onOpenShared: () -> Unit,
    onCreate: (String, java.time.DayOfWeek) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedDayName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDay = selectedDayName?.let { java.time.DayOfWeek.valueOf(it) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH) }
    BackHandler { if (!restoring) onOpenShared() }

    PageColumn(modifier.imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onOpenShared, enabled = !restoring) {
                Icon(SproutIcons.ArrowBack, contentDescription = "Back to Shared Workspace")
            }
            Column(Modifier.weight(1f)) {
                Text("Local setup", style = MaterialTheme.typography.titleMedium)
            }
        }
        PageHeading("Set up your congregation", "Manage participants and prepare meeting schedules on this device. No account is needed.")
        if (needsRecovery) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Text("Your saved workspace couldn’t be opened. Restore a backup or create a new workspace.",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        M3Card {
            Text("Congregation details", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Congregation name") },
                singleLine = true,
                enabled = !restoring,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            LocalMeetingDayPicker(selectedDay, enabled = !restoring) { selectedDayName = it.name }
            if (selectedDay != null) {
                val nextDate = nextLocalMeetingWeek(LocalDate.now(), selectedDay)
                    .plusDays((selectedDay.value - 1).toLong())
                Text("Next meeting · ${nextDate.format(dateFormat)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                enabled = name.isNotBlank() && selectedDay != null && !restoring,
                onClick = { selectedDay?.let { onCreate(name.trim(), it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Local Workspace") }
        }
        M3Card {
            Text("Restore from backup", style = MaterialTheme.typography.titleMedium)
            Text("Use a Local Workspace backup to bring your congregation data to this device.",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRestore, enabled = !restoring, modifier = Modifier.fillMaxWidth()) {
                if (restoring) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (restoring) "Restoring backup…" else "Choose backup file")
            }
        }
    }
}

@Composable
private fun LocalMeetingDayPicker(
    selectedDay: java.time.DayOfWeek?,
    enabled: Boolean = true,
    onSelect: (java.time.DayOfWeek) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    fun displayName(day: java.time.DayOfWeek) =
        day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

    Text("Midweek meeting day", style = MaterialTheme.typography.labelLarge)
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Icon(SproutIcons.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(selectedDay?.let(::displayName) ?: "Choose a day", modifier = Modifier.weight(1f))
            Icon(SproutIcons.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }) {
            java.time.DayOfWeek.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayName(option)) },
                    onClick = { open = false; onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun DeviceWorkspaceFields(data: DeviceWorkspace, action: String, onSave: (String, java.time.DayOfWeek) -> Unit) {
    var name by rememberSaveable(data.name) { mutableStateOf(data.name) }
    var day by rememberSaveable(data.meetingDay) { mutableStateOf(data.meetingDay.name) }
    M3Card {
        OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text("Congregation name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        LocalMeetingDayPicker(java.time.DayOfWeek.valueOf(day)) { day = it.name }
        Button(
            enabled = name.trim().isNotEmpty() && (name.trim() != data.name || day != data.meetingDay.name),
            onClick = { onSave(name.trim(), java.time.DayOfWeek.valueOf(day)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(action) }
    }
}

private val localMeetingClock = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

private fun localMeetingTimeFromMinutes(value: Int): String {
    val minuteOfDay = Math.floorMod(value, 24 * 60)
    return java.time.LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(localMeetingClock)
}

private fun formatLocalMeetingTime(value: String): String = runCatching {
    java.time.LocalTime.parse(value.trim()).format(localMeetingClock)
}.getOrDefault(value)

private fun SavedSchedule.matchesWorkbook(template: SavedSchedule): Boolean {
    fun Assignment.programPart() = copy(person = "", assistant = "", secondaryAssignee = "", secondaryAssistant = "")
    return reading == template.reading && openingSong == template.openingSong &&
        closingSong == template.closingSong && assignments.map { it.programPart() } == template.assignments
}

private fun SavedSchedule.hasLocalWorkbookContent() =
    assignments.isNotEmpty() || reading.isNotBlank() || openingSong.isNotBlank() || closingSong.isNotBlank()

private fun SavedSchedule.withLocalRoles(draft: SavedSchedule?) = if (draft == null) this else copy(
    chairman = draft.chairman, openingPrayer = draft.openingPrayer,
    adviser = draft.adviser, audienceGroup = draft.audienceGroup
)

@Composable
private fun LocalWorkspaceHome(
    modifier: Modifier,
    workspace: DeviceWorkspace,
    onOpenWeek: (String) -> Unit,
    onPreview: (SavedSchedule) -> Unit
) {
    val today = LocalDate.now()
    val nextWeek = nextLocalMeetingWeek(today, workspace.meetingDay)
    val nextWeekKey = nextWeek.toString()
    val offset = (workspace.meetingDay.value - 1).toLong()
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH) }
    val savedByWeek = workspace.history.associateBy { it.weekStart }
    fun isUnsaved(draft: SavedSchedule): Boolean = draft.hasLocalWorkbookContent() &&
        savedByWeek[draft.weekStart]?.copy(saved = draft.saved) != draft
    val activeDraft = workspace.drafts.firstOrNull { it.weekStart == workspace.selectedWeek && isUnsaved(it) }
        ?: workspace.drafts.firstOrNull { it.weekStart == nextWeekKey && isUnsaved(it) }

    HomeScreen(
        modifier = modifier,
        accountName = workspace.name,
        canCreateSchedule = true,
        schedules = workspace.history.map { it.workspaceEntry() },
        loading = false,
        error = null,
        onRefresh = null,
        onOpenSchedule = onPreview,
        onCreateSchedule = { onOpenWeek(nextWeekKey) },
        dayOffset = offset,
        workspaceLabel = "Local Workspace",
        emptyMessage = "Next meeting: ${nextWeek.plusDays(offset).format(dateFormat)}. Save a schedule to this device to see it here."
    ) {
        if (activeDraft != null) M3Card {
            Text("Continue your draft", style = MaterialTheme.typography.titleMedium)
            Text(LocalDate.parse(activeDraft.weekStart).plusDays(offset).format(dateFormat), color = Muted)
            TextButton(onClick = { onOpenWeek(activeDraft.weekStart) }) { Text("Resume draft") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceWorkbook(
    modifier: Modifier,
    draft: SavedSchedule,
    downloads: List<SavedSchedule>,
    captureBusy: Boolean,
    captureLog: List<String>,
    captureResult: Pair<String, Boolean>?,
    capturingSelectedWeek: Boolean,
    onSelectWeek: (LocalDate) -> Unit,
    onEdit: (SavedSchedule) -> Unit,
    onCapture: () -> Unit,
    onNext: () -> Unit
) {
    val date = LocalDate.parse(draft.weekStart)
    val template = downloads.firstOrNull { it.weekStart == draft.weekStart }
    var pickDate by remember { mutableStateOf(false) }
    var useDownload by remember(draft.weekStart) { mutableStateOf(false) }
    var showCaptureLog by rememberSaveable(draft.weekStart) { mutableStateOf(false) }
    var showParts by rememberSaveable(draft.weekStart) { mutableStateOf(false) }
    val currentWeek = localWeek(LocalDate.now())
    val weekLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("MMM d")) + " – " +
            date.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    WorkflowPage(modifier, actions = {
        WorkbookContinueButton(onNext = onNext, canContinue = draft.assignments.isNotEmpty())
    }) {
        PageHeading("Workbook", "Step 1 of 3 · Local Workspace")
        WorkbookMeetingDetails(
            weekLabel = weekLabel,
            bible = draft.reading,
            onBible = { onEdit(draft.copy(reading = it)) },
            openingSong = draft.openingSong,
            onOpeningSong = { onEdit(draft.copy(openingSong = it)) },
            closingSong = draft.closingSong,
            onClosingSong = { onEdit(draft.copy(closingSong = it)) },
            readOnly = false,
            onThisWeek = if (date == currentWeek) null else ({ onSelectWeek(currentWeek) }),
            onPickDate = { pickDate = true }
        )
        val status = when {
            capturingSelectedWeek -> captureLog.lastOrNull() ?: "Starting workbook capture…"
            captureResult != null -> captureResult.first
            captureBusy -> "Capture is running for another week."
            template != null -> "A captured workbook is available offline for this week."
            else -> "Use the capture button while online to load this week’s meeting parts."
        }
        WorkbookProgramCard(
            workbookStatus = status,
            workbookBusy = capturingSelectedWeek,
            workbookCaptureLog = captureLog,
            fetchError = captureResult?.second == false,
            captureSucceeded = template != null || captureResult?.second == true,
            canImport = !captureBusy,
            actionLabel = "Capture workbook",
            onImport = onCapture,
            showCaptureLog = showCaptureLog,
            onToggleLog = { showCaptureLog = !showCaptureLog }
        ) {
            if (template != null && !draft.matchesWorkbook(template)) TextButton(onClick = {
                if (draft.hasLocalWorkbookContent()) useDownload = true
                else onEdit(template.withLocalRoles(draft))
            }) { Text("Apply captured workbook") }
        }
        if (draft.assignments.isNotEmpty()) M3Card {
            Text("Meeting parts", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showParts = !showParts }) {
                Text(if (showParts) "Hide meeting parts" else "Show ${draft.assignments.size} meeting parts")
            }
            if (showParts) draft.assignments.forEach { part ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(part.title, style = MaterialTheme.typography.bodyMedium)
                    val details = listOf(formatLocalMeetingTime(part.time), part.duration).filter { it.isNotBlank() }
                    if (details.isNotEmpty()) Text(details.joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(enabled = state.selectedDateMillis != null, onClick = {
                    state.selectedDateMillis?.let {
                        onSelectWeek(localWeek(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    pickDate = false
                }) { Text("Select week") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } }
        ) { DatePicker(state) }
    }
    if (useDownload && template != null) AlertDialog(
        onDismissRequest = { useDownload = false },
        title = { Text("Apply captured workbook?") },
        text = { Text("This replaces the reading, songs, and meeting parts in this draft, including the names assigned to those parts.") },
        confirmButton = { TextButton(onClick = {
            onEdit(template.withLocalRoles(draft))
            useDownload = false
        }) { Text("Apply workbook") } },
        dismissButton = { TextButton(onClick = { useDownload = false }) { Text("Cancel") } }
    )
}
