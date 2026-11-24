Warning: truncated output (original token count: 65944)
Total output lines: 4000

package com.example.meetingscheduler

import android.Manifest

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.BroadcastReceiver
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.AutoStories
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.ArrowForward
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.CloudUpload
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Menu
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.AddAPhoto
import androidx.compose.material.icons.twotone.Logout
import androidx.compose.material.icons.twotone.Assignment
import androidx.compose.material.icons.twotone.Groups
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.ManageAccounts
import androidx.compose.material.icons.twotone.Preview
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material.icons.twotone.PersonAdd
import androidx.compose.material.icons.twotone.Sort
import androidx.compose.material.icons.twotone.AdminPanelSettings
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Block
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.SystemUpdateAlt
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SHARE_RESULT_PREFS = "schedule_share_result"
private const val SHARE_CHOSEN_ACTION = "com.example.meetingscheduler.SHARE_TARGET_CHOSEN"

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

private fun syncLauncherPalette(context: Context, selected: AppPaletteStyle) {
    val packageManager = context.packageManager
    fun setEnabled(style: AppPaletteStyle, enabled: Boolean) {
        val component = ComponentName(context, "${context.packageName}.${style.launcherAlias}")
        packageManager.setComponentEnabledSetting(
            component,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
    setEnabled(selected, true)
    AppPaletteStyle.entries.filterNot { it == selected }.forEach { setEnabled(it, false) }
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
    ACCOUNTS("Settings", "ACCOUNT")
}

private class LocalWorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("meeting_scheduler_workspace", Context.MODE_PRIVATE)
    private fun profileKey(kind: String, username: String) = "${kind}_${username.trim().lowercase(Locale.ROOT).ifBlank { "account" }}"
    fun profileName(username: String, default: String) = prefs.getString(profileKey("profile_name", username), null)?.takeIf { it.isNotBlank() } ?: default
    fun saveProfileName(username: String, value: String) { prefs.edit().putString(profileKey("profile_name", username), value).apply() }
    fun profilePhoto(username: String) = prefs.getString(profileKey("profile_photo_path", username), null)
    fun saveProfilePhoto(username: String, path: String) { prefs.edit().putString(profileKey("profile_photo_path", username), path).apply() }
 …53944 tokens truncated…        } else if (account.status.equals("active", true) || account.status.equals("disabled", true)) {
                                            val active = account.status.equals("active", true)
                                            Surface(
                                                Modifier.align(Alignment.BottomEnd).size(24.dp),
                                                shape = CircleShape,
                                                color = if (active) ForestContainer else MaterialTheme.colorScheme.errorContainer,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (active) Icons.TwoTone.CheckCircle else Icons.TwoTone.Block,
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
    }
    selectedAccount?.let { selected ->
        val account = accounts.firstOrNull { it.id == selected.id } ?: selected
        var linkedParticipantId by remember(account.id, account.memberId) { mutableIntStateOf(account.memberId ?: 0) }
        var participantMenuExpanded by remember(account.id) { mutableStateOf(false) }
        val roleName = when (account.role.lowercase()) { "admin" -> "Administrator"; "scheduler" -> "Scheduler"; "viewer" -> "Viewer"; else -> account.role.replaceFirstChar { it.uppercase() } }
        val statusName = when (account.status.lowercase()) { "requested" -> "Requested"; "active" -> "Active"; "disabled" -> "Disabled"; else -> account.status.replaceFirstChar { it.uppercase() } }
        val isSelf = account.username.equals(currentUsername, true) || account.role == "admin"
        Dialog(onDismissRequest = { selectedAccount = null }) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.imePadding().padding(20.dp).heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.78f).dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountPhotoAvatar(account, 64.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(account.displayName.ifBlank { account.username }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("@" + account.username, color = Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedAccount = null }, modifier = Modifier.size(48.dp)) { Icon(Icons.TwoTone.Close, contentDescription = "Close account details", tint = Forest, modifier = Modifier.size(20.dp)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AccountInfoLine("Role", roleName)
                    AccountInfoLine("Status", statusName, if (account.status == "requested") MaterialTheme.colorScheme.error else Forest)
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
                                    Icon(Icons.TwoTone.ArrowDropDown, contentDescription = null, tint = Forest)
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
                        Icon(Icons.TwoTone.AddAPhoto, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
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
                                        Icon(Icons.TwoTone.ManageAccounts, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
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
                                            "home" -> Icons.TwoTone.Home
                                            "workbook" -> Icons.TwoTone.CalendarMonth
                                            "assignments" -> Icons.TwoTone.Assignment
                                            "preview" -> Icons.TwoTone.Preview
                                            "participants" -> Icons.TwoTone.Groups
                                            else -> Icons.TwoTone.History
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
                                                                Icon(Icons.TwoTone.Check, contentDescription = null, modifier = Modifier.size(14.dp))
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
                                Icon(Icons.TwoTone.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save access settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (!isSelf) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        when (account.status) {
                            "requested" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccountActionButton("Approve", Icons.TwoTone.CheckCircle, Forest, Modifier.weight(1f)) { beginAction(account, "approve") }
                                AccountActionButton("Decline", Icons.TwoTone.Close, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "decline") }
                            }
                            "active" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccountActionButton("Change role", Icons.TwoTone.ManageAccounts, Forest, Modifier.weight(1f)) { beginAction(account, "change_role") }
                                    AccountActionButton("Reset password", Icons.TwoTone.Lock, Forest, Modifier.weight(1f)) { beginAction(account, "reset") }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccountActionButton("Disable", Icons.TwoTone.Warning, Color(0xFFFFA726), Modifier.weight(1f)) { beginAction(account, "disable") }
                                    AccountActionButton("Delete", Icons.TwoTone.Delete, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "delete") }
                                }
                            }
                            "disabled" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccountActionButton("Enable", Icons.TwoTone.CheckCircle, Forest, Modifier.weight(1f)) { beginAction(account, "enable") }
                                AccountActionButton("Delete", Icons.TwoTone.Delete, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { beginAction(account, "delete") }
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



