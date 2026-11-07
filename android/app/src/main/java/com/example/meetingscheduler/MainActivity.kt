package com.example.meetingscheduler

import android.content.Context
import android.content.Intent
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
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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
    onError = Color(0xFF690005)
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
    outlineVariant = Color(0xFFC1C9C0)
)

private val Forest @Composable get() = MaterialTheme.colorScheme.primary
private val ForestContainer @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val AppSurface @Composable get() = MaterialTheme.colorScheme.background
private val SurfaceContainer @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val Ink @Composable get() = MaterialTheme.colorScheme.onSurface
private val Muted @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeetingSchedulerRoot(window) }
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
private enum class Screen(val title: String, val section: String) {
    HOME("Home Page", "OVERVIEW"),
    DETAILS("Workbook", "SCHEDULE TOOLS"),
    ASSIGNMENTS("Assignments", "SCHEDULE TOOLS"),
    PREVIEW("Preview & Export", "SCHEDULE TOOLS"),
    PARTICIPANTS("Participants", "CONGREGATION"),
    HISTORY("Schedule History", "CONGREGATION"),
    ACCOUNTS("Accounts & Access", "ADMINISTRATION")
}

private class LocalWorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("meeting_scheduler_workspace", Context.MODE_PRIVATE)
    private fun profileKey(kind: String, username: String) = "${kind}_${username.trim().lowercase(Locale.ROOT).ifBlank { "account" }}"
    fun profileName(username: String, default: String) = prefs.getString(profileKey("profile_name", username), null)?.takeIf { it.isNotBlank() } ?: default
    fun saveProfileName(username: String, value: String) { prefs.edit().putString(profileKey("profile_name", username), value).apply() }
    fun profilePhoto(username: String) = prefs.getString(profileKey("profile_photo_path", username), null)
    fun saveProfilePhoto(username: String, path: String) { prefs.edit().putString(profileKey("profile_photo_path", username), path).apply() }
    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) { prefs.edit().putBoolean("dark_mode", value).apply() }
    fun detail(key: String, fallback: String) = prefs.getString("detail_$key", fallback) ?: fallback
    fun setDetail(key: String, value: String) { prefs.edit().putString("detail_$key", value).apply() }
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

@Composable
private fun MeetingSchedulerRoot(window: android.view.Window) {
    val context = LocalContext.current
    val store = remember(context) { LocalWorkspaceStore(context) }
    var darkMode by rememberSaveable { mutableStateOf(store.darkMode) }
    val scheme = if (darkMode) DarkPalette else LightPalette
    SideEffect {
        window.statusBarColor = scheme.background.toArgb()
        window.navigationBarColor = scheme.background.toArgb()
        window.decorView.systemUiVisibility = if (darkMode) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }
    MaterialTheme(colorScheme = scheme, shapes = Shapes(
        small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp)
    )) {
        MeetingSchedulerApp(darkMode = darkMode, onToggleTheme = { darkMode = !darkMode; store.darkMode = darkMode }, store = store)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingSchedulerApp(darkMode: Boolean, onToggleTheme: () -> Unit, store: LocalWorkspaceStore) {
    val context = LocalContext.current
    val api = remember(context) { SchedulerApi(context) }
    val authScope = rememberCoroutineScope()
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var authReady by remember { mutableStateOf(false) }
    var loginBusy by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordChangeRequired by rememberSaveable { mutableStateOf(false) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1_050); splashFinished = true }
    var identityName by rememberSaveable { mutableStateOf("") }
    var identityUsername by rememberSaveable { mutableStateOf("") }
    var identityRole by rememberSaveable { mutableStateOf("") }
    var profilePhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var profilePhotoDataUrl by remember { mutableStateOf("") }
    var profileDialogOpen by remember { mutableStateOf(false) }
    var profileNameDraft by remember { mutableStateOf("") }
    var profileError by remember { mutableStateOf<String?>(null) }
    var workbookStatus by rememberSaveable { mutableStateOf("Naghahanap ng workbook para sa napiling linggo…") }
    var workbookBusy by remember { mutableStateOf(false) }
    var workbookCaptureLog by remember { mutableStateOf(listOf("READY · waiting for the selected meeting week")) }
    var cloudStatus by rememberSaveable { mutableStateOf("Konektado sa website") }
    var participantAdmin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(api) {
        runCatching { api.currentUser() }.onSuccess {
            identityName = store.profileName(it.username, it.name); identityUsername = it.username
            profilePhotoPath = store.profilePhoto(it.username); profilePhotoDataUrl = it.photoDataUrl; identityRole = it.role
            if (it.mustChangePassword) passwordChangeRequired = true else signedIn = true
        }
        authReady = true
    }
    if (!splashFinished || !authReady) { MeetingSplashScreen(); return }
    if (!signedIn) {
        if (!authReady) {
            Surface(color = AppSurface, modifier = Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) } }
        } else LoginScreen(
            busy = loginBusy, error = loginError, mustChangePassword = passwordChangeRequired,
            onRequestAccess = { name, username, note -> api.requestAccess(name, username, note); Unit },
            onChangePassword = { currentPassword, newPassword ->
                authScope.launch {
                    loginBusy = true; loginError = null
                    runCatching { api.changePassword(currentPassword, newPassword) }
                        .onSuccess { passwordChangeRequired = false; signedIn = true }
                        .onFailure { loginError = it.message ?: "Hindi mapalitan ang password." }
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
                        .onFailure { loginError = it.message ?: "Hindi makapag-sign in." }
                    loginBusy = false
                }
            }
        )
        return
    }

    var current by rememberSaveable { mutableStateOf(Screen.HOME) }
    val officialPreviewWebView = remember { mutableStateOf<WebView?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) runCatching { api.currentUser() }
            .onSuccess { profilePhotoDataUrl = it.photoDataUrl }
    }
    val scope = rememberCoroutineScope()
    var weekStart by rememberSaveable { mutableStateOf(store.detail("week", LocalDate.of(2026, 11, 9).toString())) }
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
    val parts = remember(store) { mutableStateListOf<Assignment>().apply { addAll(store.loadAssignments()) } }
    val history = remember(store) { mutableStateListOf<SavedSchedule>().apply { addAll(store.loadHistory()) } }
    var participantsLoaded by rememberSaveable { mutableStateOf(false) }
    var participantsBusy by remember { mutableStateOf(false) }
    var rotationStats by remember { mutableStateOf<Map<Int, ParticipantRotation>>(emptyMap()) }
    var rotationBusy by remember { mutableStateOf(false) }
    var rotationError by remember { mutableStateOf<String?>(null) }
    var remoteRows by remember { mutableStateOf<List<RemoteSchedule>>(emptyList()) }
    var homeSchedules by remember { mutableStateOf<List<RemoteSchedule>>(emptyList()) }
    var homeLoaded by rememberSaveable { mutableStateOf(false) }
    var homeBusy by remember { mutableStateOf(false) }
    var homeError by remember { mutableStateOf<String?>(null) }
    var homePreviewHtml by remember { mutableStateOf<String?>(null) }
    var homePreviewWeek by remember { mutableStateOf<String?>(null) }
    var homePreviewLoading by remember { mutableStateOf(false) }
    var editingRemoteId by rememberSaveable { mutableIntStateOf(0) }
    var suppressWeekLookup by remember { mutableStateOf(false) }
    var remoteScheduleLoadedWeek by remember { mutableStateOf("") }
    var weekLoadNonce by remember { mutableIntStateOf(0) }
    var historyLoaded by rememberSaveable { mutableStateOf(false) }
    var remoteAccounts by remember { mutableStateOf<List<RemoteAccount>>(emptyList()) }
    var accountsLoaded by rememberSaveable { mutableStateOf(false) }
    var accountsBusy by remember { mutableStateOf(false) }
    var accountsError by remember { mutableStateOf<String?>(null) }
    var accountReloadTick by remember { mutableIntStateOf(0) }
    var accountPhotoTarget by remember { mutableStateOf<RemoteAccount?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var restrictedTapCount by rememberSaveable { mutableIntStateOf(0) }
    var accessNotice by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val week = remember(weekStart) { runCatching { LocalDate.parse(weekStart) }.getOrDefault(LocalDate.of(2026,11,9)) }
    val weekLabel = remember(week) { week.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + week.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy")) }
    val eligible = participants.filter { it.enabled }
    fun canAccess(screen: Screen): Boolean = when (identityRole.lowercase()) {
        "admin", "administrator" -> true
        "scheduler" -> screen != Screen.PARTICIPANTS && screen != Screen.ACCOUNTS
        "viewer" -> screen == Screen.HOME
        else -> false
    }
    fun navigateTo(screen: Screen) {
        if (canAccess(screen)) current = screen
        else {
            restrictedTapCount += 1
            accessNotice = if (restrictedTapCount >= 7)
                "Please Don't Spam, Just Message the Administrator for Access." to true
            else "Your Account Doesn't Have Access to This Tool." to false
        }
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

    LaunchedEffect(signedIn, participantsLoaded) {
        if(signedIn && !participantsLoaded && !participantsBusy) {
            participantsBusy=true
            runCatching { api.members(includeInactive=true) }.onSuccess { response ->
                participantAdmin=response.optBoolean("isAdmin",participantAdmin)
                val rows=response.optJSONArray("members") ?: JSONArray(); participants.clear()
                for(i in 0 until rows.length()) rows.optJSONObject(i)?.let { row -> participants.add(Participant(row.optString("name"),row.optString("appointment"),row.optString("serviceStatus"),row.optString("gender").ifBlank{"unspecified"},row.optBoolean("active",true),row.optInt("id"),row.optBoolean("unbaptized",false))) }
                store.saveParticipants(participants.toList()); participantsLoaded=true; cloudStatus="Nakuha ang participants mula sa website"
            }.onFailure { cloudStatus="Hindi makuha ang participants: ${it.message ?: "server error"}"; participantsLoaded=true }
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
    fun saveToCloud() { if (signedIn && identityRole == "admin") authScope.launch {
        cloudStatus = "Sine-save sa cloud…"
        runCatching { api.saveSchedule(weekLabel, weekStart, bible, openingSong, closingSong, parts.map { it.toJson() }, chairman, openingPrayer, adviser, audienceGroup, workbook, editingRemoteId) }
            .onSuccess { historySaved -> cloudStatus = if (historySaved) "Naka-save sa cloud" else "Na-save; hindi na-update ang assignment history"; toastMessage = cloudStatus; historyLoaded=false; editingRemoteId=0 }
            .onFailure { cloudStatus = "Hindi na-save: ${it.message ?: "server error"}"; toastMessage = cloudStatus }
    } else toastMessage = "Admin account lang ang puwedeng mag-save sa cloud" }
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
                persistDetails()
                store.saveAssignments(parts.toList())
                workbookStatus = "✓ ${result.parts.size} bahagi ang nakuha mula sa JW.ORG"
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
        val savedWeekStart=meta.optString("weekStart").ifBlank { scheduleStartDate(remote.weekLabel,remote.updatedAt)?.toString() ?: weekStart }
        if(savedWeekStart!=weekStart) suppressWeekLookup=true
        remoteScheduleLoadedWeek=savedWeekStart; weekLoadNonce++
        weekStart=savedWeekStart; bible=meta.optString("reading",remote.reading)
        openingSong=meta.optString("openingSong",openingSong); closingSong=meta.optString("closingSong",closingSong)
        workbook=row.optString("sourceUrl",workbook)
        chairman=meta.optString("chairman",""); openingPrayer=meta.optString("openingPrayer",""); adviser=meta.optString("adviser",""); audienceGroup=meta.optString("audienceGroup","")
        store.setDetail("chairman",chairman); store.setDetail("opening_prayer",openingPrayer); store.setDetail("adviser",adviser); store.setDetail("audience_group",audienceGroup)
        parts.clear(); for(i in 0 until raw.length()) raw.optJSONObject(i)?.let { parts.add(it.toAssignment()) }
        editingRemoteId=remote.id; persistDetails(); store.saveAssignments(parts.toList())
        workbookStatus="✓ Na-load ang saved cloud schedule"
        if(navigate) current=if(edit) Screen.DETAILS else Screen.PREVIEW
    }
    LaunchedEffect(weekStart, signedIn, weekLoadNonce) {
        if (suppressWeekLookup) { suppressWeekLookup=false; return@LaunchedEffect }
        if (remoteScheduleLoadedWeek==weekStart) return@LaunchedEffect
        if (signedIn) {
            workbookBusy=true; workbookStatus="Sinusuri kung may saved schedule sa napiling petsa…"
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
        val html = buildOfficialScheduleHtml(weekStart, bible, openingSong, closingSong, parts.toList(), chairman, openingPrayer, adviser, audienceGroup)
        val web = WebView(context)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print("Meeting schedule", view.createPrintDocumentAdapter("Meeting schedule"), android.print.PrintAttributes.Builder().build())
            }
        }
        web.loadDataWithBaseURL("file:///android_asset/", html, "text/HTML", "UTF-8", null)
    }
    fun shareSchedulePng() {
        val renderer=officialPreviewWebView.value
        if(renderer==null || renderer.width<=0 || renderer.height<=0){toastMessage="Hintaying mabuo ang preview bago mag-export.";return}
        renderer.evaluateJavascript("window.scrollTo(0,0)") {
            renderer.post {
                val bitmap=Bitmap.createBitmap(renderer.width,renderer.height,Bitmap.Config.ARGB_8888)
                renderer.draw(Canvas(bitmap))
                val file=File(context.cacheDir,"meeting-schedule.png")
                FileOutputStream(file).use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
                val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
                val intent=Intent(Intent.ACTION_SEND).apply{type="image/png";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}
                context.startActivity(Intent.createChooser(intent,"Share schedule image"))
            }
        }
    }
    fun openSaved(item: SavedSchedule) {
        if (item.weekStart.isNotBlank()) weekStart = item.weekStart
        bible = item.reading; openingSong = item.openingSong; closingSong = item.closingSong
        chairman=item.chairman; openingPrayer=item.openingPrayer; adviser=item.adviser; audienceGroup=item.audienceGroup
        if (item.assignments.isNotEmpty()) { parts.clear(); parts.addAll(item.assignments); store.saveAssignments(parts.toList()) }
        persistDetails(); current = Screen.DETAILS; toastMessage = "Na-open ang saved schedule"
    }
    fun duplicateSaved(item: SavedSchedule) {
        val sourceDate = runCatching { LocalDate.parse(item.weekStart) }.getOrDefault(week.plusDays(-7))
        weekStart = sourceDate.plusWeeks(1).toString()
        bible = item.reading; openingSong = item.openingSong; closingSong = item.closingSong
        chairman=item.chairman; openingPrayer=item.openingPrayer; adviser=item.adviser; audienceGroup=item.audienceGroup
        if (item.assignments.isNotEmpty()) { parts.clear(); parts.addAll(item.assignments); store.saveAssignments(parts.toList()) }
        persistDetails(); current = Screen.DETAILS; toastMessage = "Naduplicate para sa susunod na linggo"
    }
    fun loadRemoteSchedule(remote: RemoteSchedule, edit: Boolean) { authScope.launch {
        runCatching { applyRemoteSchedule(remote,navigate=true,edit=edit) }.onFailure { toastMessage=it.message ?: "Hindi mabuksan ang schedule" }
    } }

    fun openUpcomingPreview(remote: RemoteSchedule) {
        homePreviewWeek = remote.weekLabel
        homePreviewHtml = null
        homePreviewLoading = true
        authScope.launch {
            runCatching {
                val saved = api.schedule(remote.id)
                val meta = saved.optJSONObject("meta") ?: JSONObject()
                val rawParts = saved.optJSONArray("parts") ?: JSONArray()
                val assignments = (0 until rawParts.length()).mapNotNull { rawParts.optJSONObject(it)?.toAssignment() }
                val start = meta.optString("weekStart").ifBlank { scheduleStartDate(remote.weekLabel, remote.updatedAt)?.toString() ?: LocalDate.now().toString() }
                buildOfficialScheduleHtml(
                    start,
                    meta.optString("reading", remote.reading),
                    meta.optString("openingSong", "27"),
                    meta.optString("closingSong", "132"),
                    assignments,
                    meta.optString("chairman"),
                    meta.optString("openingPrayer"),
                    meta.optString("adviser"),
                    meta.optString("audienceGroup")
                )
            }.onSuccess { homePreviewHtml = it }
                .onFailure { toastMessage = it.message ?: "Hindi mabuksan ang preview."; homePreviewWeek = null }
            homePreviewLoading = false
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 340.dp), drawerContainerColor = AppSurface) {
                Column(Modifier.fillMaxHeight().padding(horizontal = 14.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, bottom = 16.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { profileNameDraft = identityName; profileError = null; profileDialogOpen = true }) { ProfileAvatar(identityName, profilePhotoPath, 48.dp, profilePhotoDataUrl) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text("Meeting Scheduler", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Midweek meeting workspace", color = Muted, fontSize = 12.sp) }
                    }
                    HorizontalDivider(color = SurfaceContainer)
                    DrawerSection("OVERVIEW")
                    DrawerEntry(Icons.TwoTone.Home, Screen.HOME, current, restricted = !canAccess(Screen.HOME)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerSection("SCHEDULE TOOLS")
                    DrawerEntry(Icons.TwoTone.CalendarMonth, Screen.DETAILS, current, restricted = !canAccess(Screen.DETAILS)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerEntry(Icons.TwoTone.Assignment, Screen.ASSIGNMENTS, current, restricted = !canAccess(Screen.ASSIGNMENTS)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerEntry(Icons.TwoTone.Preview, Screen.PREVIEW, current, restricted = !canAccess(Screen.PREVIEW)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerSection("CONGREGATION")
                    DrawerEntry(Icons.TwoTone.Groups, Screen.PARTICIPANTS, current, restricted = !canAccess(Screen.PARTICIPANTS)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerEntry(Icons.TwoTone.History, Screen.HISTORY, current, restricted = !canAccess(Screen.HISTORY)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    DrawerSection("ADMINISTRATION")
                    DrawerEntry(Icons.TwoTone.ManageAccounts, Screen.ACCOUNTS, current, restricted = !canAccess(Screen.ACCOUNTS)) { destination -> navigateTo(destination); scope.launch { drawerState.close() } }
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = SurfaceContainer, modifier = Modifier.padding(bottom = 8.dp))
                    Column(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp), horizontalAlignment = Alignment.Start) {
                        TextButton(onClick = onToggleTheme, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(if (darkMode) Icons.TwoTone.LightMode else Icons.TwoTone.DarkMode, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (darkMode) "Light Mode" else "Dark Mode", color = Forest, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            api.signOut()
                            signedIn = false
                            identityName = ""
                            identityUsername = ""
                            identityRole = ""
                            loginError = null
                            authReady = true
                            scope.launch { drawerState.close() }
                        }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(Icons.TwoTone.Logout, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign Out", color = Forest, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AppSurface,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Box(Modifier.size(36.dp).background(ForestContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.TwoTone.Menu, contentDescription = "Open navigation menu", tint = Forest)
                            }
                        }
                    },
                    actions = {

                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppSurface)
                )
            },
            snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) }
        ) { insets ->
            val scroll = Modifier.fillMaxSize().padding(insets)
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (fadeIn(tween(120)) + slideInHorizontally(tween(140)) { it / 22 }) togetherWith
                        (fadeOut(tween(80)) + slideOutHorizontally(tween(100)) { -it / 28 })
                },
                label = "screen transition"
            ) { destination -> when (destination) {
                Screen.HOME -> HomeScreen(
                    modifier = scroll,
                    accountName = identityName,
                    schedules = homeSchedules,
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
                    onNext = { navigateTo(Screen.ASSIGNMENTS) }
                )
                Screen.ASSIGNMENTS -> AssignmentsScreen(
                    modifier = scroll, parts = parts, eligible = eligible, rotation = rotationStats,
                    rotationBusy = rotationBusy, rotationError = rotationError,
                    chairman=chairman, openingPrayer=openingPrayer, adviser=adviser, audienceGroup=audienceGroup,
                    onRole={ role, value -> when(role) { "chairman" -> chairman=value; "opening_prayer" -> openingPrayer=value; "adviser" -> adviser=value; else -> audienceGroup=value }; store.setDetail(role, value) },
                    isAdmin = signedIn && identityRole == "admin",
                    onAssign = { index, slot, name -> parts[index] = when(slot) { "assistant" -> parts[index].copy(assistant=name); "secondary" -> parts[index].copy(secondaryAssignee=name); "secondaryAssistant" -> parts[index].copy(secondaryAssistant=name); else -> parts[index].copy(person=name) }; store.saveAssignments(parts.toList()) },
                    onSaveCloud = ::saveToCloud, onContinueExport = { navigateTo(Screen.PREVIEW) }
                )
                Screen.PREVIEW -> PreviewScreen(
                    modifier = scroll, weekStart = weekStart, bible = bible, openingSong = openingSong,
                    closingSong = closingSong, parts = parts, chairman=chairman, openingPrayer=openingPrayer,
                    adviser=adviser, audienceGroup=audienceGroup, previewWebView=officialPreviewWebView,
                    onExportText = {
                        val text = buildOfficialScheduleText(weekStart, bible, openingSong, closingSong, parts, chairman, openingPrayer, adviser, audienceGroup)
                        val file = File(context.cacheDir, "meeting-schedule-${weekStart}.txt")
                        file.writeText(text, Charsets.UTF_8)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Meeting schedule · $weekLabel")
                            putExtra(Intent.EXTRA_TEXT, "Iskedyul ng Pulong ng Gitnang Sanlinggo · $weekLabel")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = android.content.ClipData.newUri(context.contentResolver, "Meeting schedule text", uri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share schedule text"))
                    }, onPng = ::shareSchedulePng
                )
                Screen.PARTICIPANTS -> {
                    ParticipantsScreen(modifier = scroll, participants = participants,
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
                        onRefresh={participantsLoaded=false},
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
                    LaunchedEffect(current, signedIn, historyLoaded) { if (current == Screen.HISTORY && signedIn && !historyLoaded) runCatching { api.schedules() }.onSuccess { remoteRows = it; historyLoaded=true; cloudStatus = "History mula sa website" }.onFailure { cloudStatus = "Hindi makuha ang history: ${it.message}" } }
                    HistoryScreen(modifier = scroll, remoteSchedules = remoteRows, cloudStatus = cloudStatus, isAdmin=signedIn && identityRole=="admin", onView={loadRemoteSchedule(it,false)}, onEdit={loadRemoteSchedule(it,true)}, onDelete={remote -> authScope.launch { runCatching { api.deleteSchedule(remote.id) }.onSuccess { historyLoaded=false; toastMessage="Na-delete ang schedule" }.onFailure { toastMessage=it.message ?: "Hindi ma-delete" } }}, onNew = { editingRemoteId=0; navigateTo(Screen.DETAILS) })
                }
                Screen.ACCOUNTS -> {
                    LaunchedEffect(current, signedIn, accountsLoaded, accountReloadTick) {
                        if(current==Screen.ACCOUNTS && signedIn && !accountsLoaded && !accountsBusy) {
                            accountsBusy=true; accountsError=null
                            try {
                                runCatching { api.accounts() }.onSuccess { remoteAccounts=it;accountsLoaded=true }
                                    .onFailure { accountsError=it.message ?: "Hindi makuha ang accounts." }
                            } finally { accountsBusy=false }
                        }
                    }
                    AccountsScreen(
                        modifier=scroll, accounts=remoteAccounts, loading=accountsBusy, error=accountsError,
                        currentUsername=identityUsername,
                        onRefresh={accountsLoaded=false;accountReloadTick++},
                        onUploadPhoto={ account -> accountPhotoTarget=account; accountPhotoPicker.launch("image/*") },
                        onAction={ account,action,role,password -> authScope.launch {
                            accountsBusy=true;accountsError=null
                            runCatching { api.accountAction(account.id,action,role,password) }
                                .onSuccess { response -> toastMessage=response.optString("message","Account updated.");accountsLoaded=false;accountReloadTick++ }
                                .onFailure { accountsError=it.message ?: "Hindi na-update ang account." }
                            accountsBusy=false
                        } }
                    )
                }
            } }
        }
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
    if (homePreviewWeek != null) {
        Dialog(onDismissRequest = { homePreviewWeek = null; homePreviewHtml = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.84f), shape = RoundedCornerShape(24.dp), color = AppSurface) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(homePreviewWeek ?: "Schedule preview", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { homePreviewWeek = null; homePreviewHtml = null }) { Icon(Icons.TwoTone.Close, contentDescription = "Close preview", tint = Forest) }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (homePreviewLoading || homePreviewHtml == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
                    } else {
                        key(homePreviewHtml) {
                            AndroidView(factory = { viewContext -> WebView(viewContext).apply {
                                setBackgroundColor(android.graphics.Color.WHITE)
                                settings.javaScriptEnabled = true
                                loadDataWithBaseURL("file:///android_asset/", homePreviewHtml ?: "", "text/HTML", "UTF-8", null)
                            } }, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
    if (profileDialogOpen) {
        AlertDialog(
            onDismissRequest = { profileDialogOpen = false },
            containerColor = AppSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(identityName, profilePhotoPath, 76.dp, profilePhotoDataUrl)
                    OutlinedButton(onClick = { profilePhotoPicker.launch(arrayOf("image/*")) }, shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.TwoTone.AddAPhoto, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (profilePhotoPath.isNullOrBlank() && profilePhotoDataUrl.isBlank()) "Add Photo" else "Change Photo", color = Forest)
                    }
                    OutlinedTextField(
                        value = profileNameDraft,
                        onValueChange = { profileNameDraft = it; profileError = null },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Naka-save sa cloud ang profile photo. Sa device na ito naman naka-save ang pangalan.", color = Muted, fontSize = 11.sp)
                    profileError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
            },
            confirmButton = {
                TextButton(enabled = profileNameDraft.isNotBlank(), onClick = {
                    identityName = profileNameDraft.trim()
                    store.saveProfileName(identityUsername, identityName)
                    profileDialogOpen = false
                }) { Text("Save", color = Forest) }
            },
            dismissButton = { TextButton(onClick = { profileDialogOpen = false }) { Text("Cancel", color = Muted) } }
        )
    }
    if (datePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = week.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    remoteScheduleLoadedWeek=""; editingRemoteId=0
                    val selectedWeek=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
                    weekStart=selectedWeek; store.saveDetails(selectedWeek,bible,openingSong,closingSong,workbook)
                }
                datePicker = false
            }) { Text("Select", color = Forest) }
        }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("Cancel") } }) { DatePicker(state = pickerState) }
    }
    toastMessage?.let { msg -> LaunchedEffect(msg) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); toastMessage = null } }
    accessNotice?.let { (message, warning) ->
        LaunchedEffect(accessNotice) { delay(2_100); accessNotice = null }
        Dialog(onDismissRequest = { accessNotice = null }) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Icon(if (warning) Icons.TwoTone.Warning else Icons.TwoTone.ErrorOutline, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(30.dp))
                    Text(message, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun MeetingSplashScreen() {
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(140); reveal = true }
    Surface(color = Color(0xFF173522), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = reveal, enter = fadeIn(tween(500)) + scaleIn(tween(500))) {
                Box(
                    Modifier.size(104.dp).background(Color(0xFF304536), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Color(0xFFDDE9D9), modifier = Modifier.size(58.dp))
                    Icon(Icons.TwoTone.CheckCircle, contentDescription = null, tint = Color(0xFFD2AB36), modifier = Modifier.align(Alignment.BottomEnd).padding(17.dp).size(24.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(visible = reveal, enter = fadeIn(tween(420, delayMillis = 120)) + slideInVertically(tween(420, delayMillis = 120)) { it / 5 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Meeting Scheduler", color = Color(0xFFDDE9D9), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Midweek meeting workspace", color = Color(0xFFA8C3A4), fontSize = 13.sp)
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
    onRequestAccess: suspend (String, String, String) -> Unit
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = if (compact) 18.dp else 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(if (compact) 58.dp else 68.dp).background(ForestContainer, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(if (compact) 31.dp else 36.dp))
                }
                Spacer(Modifier.height(13.dp))
                Text("Meeting Scheduler", fontWeight = FontWeight.Bold, fontSize = 23.sp)
                Text("Midweek meeting workspace", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(if (compact) 22.dp else 30.dp))
                AnimatedVisibility(visible = true, enter = fadeIn(tween(520)) + slideInVertically(tween(520)) { it / 10 }) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(if (compact) 18.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            Text("ACCOUNT ACCESS", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                            Text("Welcome back", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Mag-sign in gamit ang account mo.", color = Muted, fontSize = 13.sp)
                            OutlinedTextField(
                                username,
                                { username = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.TwoTone.Person, contentDescription = null, tint = Forest) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                password,
                                { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.TwoTone.Lock, contentDescription = null, tint = Forest) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(if (showPassword) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility, contentDescription = if (showPassword) "Hide password" else "Show password", tint = Forest)
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
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Forest)
                            ) {
                                Text(if (busy) "Signing in…" else "Sign in", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Icon(Icons.TwoTone.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
                            }
                            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Text("Wala ka pang account?", color = Muted, fontSize = 12.sp)
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
            shape = RoundedCornerShape(28.dp),
            title = { Text("Set a new password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Temporary password ang gamit mo. Gumawa ng bagong password para ma-access ang app.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(currentTemporaryPassword, { currentTemporaryPassword = it; changePasswordError = null }, label = { Text("Temporary password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newPassword, { newPassword = it; changePasswordError = null }, label = { Text("New password") }, singleLine = true, visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showNewPassword = !showNewPassword }) { Icon(if (showNewPassword) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility, contentDescription = "Toggle password visibility", tint = Forest) } }, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(confirmNewPassword, { confirmNewPassword = it; changePasswordError = null }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    (changePasswordError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    if (newPassword.isNotBlank() && newPassword.length < 10) Text("Use at least 10 characters.", color = Muted, fontSize = 11.sp)
                    else if (confirmNewPassword.isNotBlank() && confirmNewPassword != newPassword) Text("Hindi magkapareho ang bagong password.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
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
            shape = RoundedCornerShape(28.dp),
            title = { Text("Request Access", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ipadala ang iyong pangalan, username, at mensahe sa administrator.", color = Muted, fontSize = 12.sp)
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
                    Icon(Icons.TwoTone.CheckCircle, contentDescription = "Success", tint = Forest, modifier = Modifier.size(28.dp))
                    Text("Success! wait for Administrator Approval.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, photoPath: String?, size: androidx.compose.ui.unit.Dp, photoDataUrl: String = "") {
    val bitmap = remember(photoPath, photoDataUrl) {
        val encoded = photoDataUrl.substringAfter("base64,", "").takeIf { it.isNotBlank() }
        val cloudBitmap = encoded?.let { value ->
            runCatching {
                val bytes = Base64.decode(value, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        cloudBitmap ?: photoPath?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(Modifier.size(size).background(ForestContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").ifBlank { "?" }.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = (size.value * .34f).sp)
        }
    }
}

@Composable
private fun DrawerSection(title: String) { Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 7.dp)) }

@Composable
private fun DrawerEntry(icon: ImageVector, screen: Screen, selected: Screen, restricted: Boolean = false, onClick: (Screen) -> Unit) {
    NavigationDrawerItem(
        modifier = if (restricted) Modifier.alpha(0.48f) else Modifier,
        label = { Text(screen.title, fontWeight = if (selected == screen) FontWeight.Bold else FontWeight.Medium, color = if (restricted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface) },
        icon = {
            Box(Modifier.size(36.dp).background(if (selected == screen) ForestContainer else SurfaceContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (restricted) MaterialTheme.colorScheme.outline else if (selected == screen) Forest else Muted)
            }
        },
        selected = selected == screen,
        onClick = { onClick(screen) },
        shape = RoundedCornerShape(28.dp),
        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = ForestContainer, selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer, unselectedTextColor = MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
private fun PageColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun PageHeading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow, color = Forest, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
        Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun M3Card(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun Label(text: String) { Text(text.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp) }

@Composable
private fun HomeScreen(
    modifier: Modifier,
    accountName: String,
    schedules: List<RemoteSchedule>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenSchedule: (RemoteSchedule) -> Unit,
    onCreateSchedule: () -> Unit
) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    val today = LocalDate.now()
    val upcoming = remember(schedules, today) {
        schedules.mapNotNull { remote ->
            val start = scheduleStartDate(remote.weekLabel, remote.updatedAt) ?: return@mapNotNull null
            if (start.plusDays(6).isBefore(today)) null else start to remote
        }.sortedBy { it.first }.take(6)
    }
    PageColumn(modifier) {
        PageHeading("MIDWEEK MEETING WORKSPACE", "Home Page", "Your congregation schedule at a glance.")
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = ForestContainer)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.TwoTone.Home, contentDescription = null, tint = Forest, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("$greeting,", color = Muted, fontSize = 14.sp)
                    Text(accountName.ifBlank { "there" }, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), color = Muted, fontSize = 12.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Upcoming Meetings", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Tap a meeting to preview its official schedule.", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.TwoTone.Refresh, contentDescription = "Refresh upcoming meetings", tint = Forest) }
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
            error != null -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            upcoming.isEmpty() -> Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(30.dp))
                    Text("No upcoming saved meetings", fontWeight = FontWeight.SemiBold)
                    Text("Create a workbook and save the schedule to the cloud to see it here.", color = Muted, fontSize = 12.sp)
                    FilledTonalButton(onClick = onCreateSchedule, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.TwoTone.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp)); Text("Open Workbook")
                    }
                }
            }
            else -> upcoming.forEach { (date, remote) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSchedule(remote) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(ForestContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(remote.weekLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(remote.reading, color = Muted, fontSize = 12.sp)
                            Text(date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)), color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.TwoTone.Preview, contentDescription = "Preview schedule", tint = Forest)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsScreen(modifier: Modifier, weekLabel: String, bible: String, onBible: (String) -> Unit, openingSong: String, onOpeningSong: (String) -> Unit, closingSong: String, onClosingSong: (String) -> Unit, workbookStatus: String, workbookBusy: Boolean, workbookCaptureLog: List<String>, canContinue:Boolean, onPickDate: () -> Unit, onImport: () -> Unit, onNext: () -> Unit) {
    PageColumn(modifier) {
        PageHeading("SCHEDULE BUILDER", "Workbook", "Pumili ng linggo. Kusang kukunin ang weekly program.")
        M3Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(ForestContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp)) }
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Label("MEETING WEEK")
                    Text("Pangunahing impormasyon", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(onClick = onPickDate, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Text(weekLabel, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Change", color = Forest, fontSize = 12.sp)
            }
            OutlinedTextField(bible, onBible, label = { Text("Bible reading") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(openingSong, onOpeningSong, label = { Text("Opening song") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(closingSong, onClosingSong, label = { Text("Closing song") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
        val fetchError = workbookStatus.contains("hindi", ignoreCase = true) || workbookStatus.contains("wala", ignoreCase = true) || workbookStatus.contains("permission", ignoreCase = true)
        M3Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(if (fetchError) MaterialTheme.colorScheme.error.copy(alpha = .12f) else ForestContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(if (workbookBusy) Icons.TwoTone.Refresh else if (workbookStatus.startsWith("✓")) Icons.TwoTone.CheckCircle else Icons.TwoTone.AutoStories, contentDescription = null, tint = if (fetchError) MaterialTheme.colorScheme.error else Forest, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Label("JW.ORG WORKBOOK")
                    Text("Weekly program", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onImport, enabled = !workbookBusy, modifier = Modifier.size(40.dp)) { if (workbookBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest) else Icon(Icons.TwoTone.Refresh, contentDescription = "I-refresh ang weekly program", tint = Forest, modifier = Modifier.size(23.dp)) }
            }
            Text("Awtomatikong naghahanap batay sa napiling linggo.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Surface(
                color = if (MaterialTheme.colorScheme.background == DarkPalette.background) Color(0xFF0D120E) else Color(0xFFE4ECE4),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (workbookBusy) "CAPTURE LOG · RUNNING" else "CAPTURE LOG", color = Forest, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                        if (workbookBusy) CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.5.dp, color = Forest)
                    }
                    workbookCaptureLog.takeLast(6).forEach { line ->
                        Text(
                            "› $line",
                            color = if (line.startsWith("ERROR")) MaterialTheme.colorScheme.error else if (MaterialTheme.colorScheme.background == DarkPalette.background) Color(0xFFB8D7B5) else Color(0xFF2D5638),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            maxLines = 2
                        )
                    }
                    if (!workbookBusy) Text(workbookStatus.removePrefix("✓ "), color = if (fetchError) MaterialTheme.colorScheme.error else Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
        Button(onClick = onNext, enabled=canContinue, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Forest), contentPadding=PaddingValues(vertical=15.dp)) {
            Text(if(workbookBusy) "Checking saved schedule…" else if(!canContinue) "Loading participants…" else "Continue to assignments")
            Spacer(Modifier.width(9.dp))
            Icon(Icons.TwoTone.ArrowForward, contentDescription = null, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun AssignmentsScreen(modifier: Modifier, parts: List<Assignment>, eligible: List<Participant>, rotation: Map<Int, ParticipantRotation>, rotationBusy: Boolean, rotationError: String?, chairman:String, openingPrayer:String, adviser:String, audienceGroup:String, isAdmin:Boolean, onRole:(String,String)->Unit, onAssign: (Int,String,String) -> Unit, onSaveCloud: () -> Unit, onContinueExport: () -> Unit) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,top=7.dp,bottom=16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Pumili ng participant para sa bawat bahagi ng pulong.", color=Muted, fontSize=12.sp, lineHeight=16.sp)
        if (rotationBusy || rotationError != null) {
            Text(
                if (rotationBusy) "Kinukuha ang participant rotation para sa linggong ito…" else "Hindi makuha ang rotation data: $rotationError",
                color = if (rotationError != null) MaterialTheme.colorScheme.error else Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant), border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Label("MEETING ROLES")
                    Text("Closing prayer · ${chairman.ifBlank { "Chairman" }}",color=Muted,fontSize=10.sp,maxLines=1)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    SlotPicker("Chairman",chairman,eligible.filter { it.isAppointedBrother() },Modifier.weight(1f),rotation){onRole("chairman",it)}
                    SlotPicker("Opening prayer",openingPrayer,eligible.filter { it.isBrother() && !it.unbaptized },Modifier.weight(1f),rotation){onRole("opening_prayer",it)}
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    SlotPicker("Adviser",adviser,eligible.filter { it.isAppointedBrother() },Modifier.weight(1f),rotation){onRole("adviser",it)}
                    SlotPicker("Audience group",audienceGroup,(1..9).map { Participant("Group $it") },Modifier.weight(1f),rotation){onRole("audience_group",it)}
                }
            }
        }
        Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant), border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Label("MEETING ASSIGNMENTS"); Text("Assignments", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp)) }
                    val done = parts.count { it.person.isNotBlank() }
                    AssistChip(onClick = {}, label = { Text("$done / ${parts.size} parts") })
                }
                Column(Modifier.fillMaxWidth().padding(top=6.dp)) {
                    parts.forEachIndexed { index, part ->
                        if(index>0) HorizontalDivider(Modifier.padding(vertical=6.dp), color=SurfaceContainer)
                        AssignmentPicker(index, part, eligible, rotation) { slot, name -> onAssign(index,slot,name) }
                    }
                }
            }
        }
        if (isAdmin) {
            OutlinedButton(
                onClick = onSaveCloud,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Icon(Icons.TwoTone.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text("Save to Cloud", fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onContinueExport,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest)
        ) {
            Text("Continue to Export", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(9.dp))
            Icon(Icons.TwoTone.ArrowForward, contentDescription = null, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun AssignmentPicker(index: Int, part: Assignment, eligible: List<Participant>, rotation: Map<Int, ParticipantRotation>, onAssign: (String,String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val darkBadges = MaterialTheme.colorScheme.background == DarkPalette.background
            val badgeColors = when (part.section) {
                "ministry" -> if (darkBadges) Color(0xFF5C4300) to Color(0xFFFFD782) else Color(0xFFFFD782) to Color(0xFF513A00)
                "living" -> if (darkBadges) Color(0xFF63342F) to Color(0xFFFFB8AE) else Color(0xFFFFB8AE) to Color(0xFF652F2A)
                else -> if (darkBadges) Color(0xFF164C50) to Color(0xFF9DE2E0) else Color(0xFFB7E7E4) to Color(0xFF12494B)
            }
            Box(Modifier.size(30.dp).background(badgeColors.first, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                Text("${part.id.takeIf { it>0 } ?: index+1}", color = badgeColors.second, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) { Text(part.title.substringAfter(". ",part.title), fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight=16.sp); Text(listOf(part.time,part.duration,part.detail).filter{it.isNotBlank()}.joinToString(" · "), color = Muted, fontSize = 10.sp, lineHeight = 13.sp) }
        }
        val title=part.title.lowercase(); val bibleReading=title.contains("pagbabasa ng bibliya"); val bibleStudy=title.contains("pag-aaral ng kongregasyon sa bibliya"); val what=title.contains("ano ang sasabihin mo")
        val qualified=part.section=="living" || (part.section=="treasures" && !bibleReading) || what
        val brothers=eligible.filter{it.isBrother() && !it.unbaptized}; val elders=brothers.filter{it.isAppointedBrother()}
        if(bibleStudy) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) { SlotPicker("Conductor",part.person,elders,Modifier.weight(1f),rotation){onAssign("main",it)}; SlotPicker("Reader",part.assistant,brothers,Modifier.weight(1f),rotation){onAssign("assistant",it)} }
        } else if((part.section=="ministry"&&!what)||bibleReading) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)){SlotPicker("Main Hall",part.person,if(qualified) elders else if(bibleReading) brothers else eligible,rotation=rotation){onAssign("main",it)}; if(part.hasAssistant) SlotPicker("Assistant",part.assistant,eligible,rotation=rotation){onAssign("assistant",it)} }
                Column(Modifier.weight(1f)){SlotPicker("Second Hall",part.secondaryAssignee,if(qualified) elders else if(bibleReading) brothers else eligible,rotation=rotation){onAssign("secondary",it)}; if(part.hasSecondaryAssistant) SlotPicker("Assistant",part.secondaryAssistant,eligible,rotation=rotation){onAssign("secondaryAssistant",it)} }
            }
        } else {
            SlotPicker(if(qualified) "Qualified brother" else "Participant",part.person,if(qualified) elders else eligible,rotation=rotation){onAssign("main",it)}
            if(part.hasAssistant&&!what) SlotPicker("Assistant",part.assistant,eligible,rotation=rotation){onAssign("assistant",it)}
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
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Participant?>(null) }
    Column(modifier.padding(top = 3.dp)) {
        Text(label, color = Muted, fontSize = 10.sp, lineHeight = 12.sp)
        Box(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(value.ifBlank { "Select" }, Modifier.weight(1f), fontSize = 11.sp, color = if (value.isBlank()) Muted else Ink, maxLines = 1)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 340.dp).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(text = { Text("Unassigned") }, onClick = { onPick(""); expanded = false })
                val orderedChoices = choices.sortedWith(
                    compareBy<Participant> { participantRotationRank(rotation[it.id]) }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
                orderedChoices.forEach { participant ->
                    val stat = rotation[participant.id]
                    DropdownMenuItem(
                        text = {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(participant.name, color = Ink, fontSize = 13.sp, maxLines = 1)
                                    Text(
                                        buildString {
                                            append(participant.appointment)
                                            if (stat != null) {
                                                append(" · Main ").append(stat.mainThisMonth)
                                                append(" · Assist ").append(stat.assistantThisMonth)
                                                append(" · ").append(stat.totalThisMonth).append(" this month")
                                                if (!stat.eligible && !stat.exempt) stat.nextEligibleDate?.let { append(" · After ").append(rotationShortDate(it)) }
                                            }
                                        },
                                        color = Muted,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
                                        maxLines = 2
                                    )
                                }
                                if (stat != null) RotationStatusTag(stat)
                            }
                        },
                        onClick = {
                            expanded = false
                            if (stat != null && !stat.exempt && (stat.totalThisMonth > 0 || !stat.eligible)) pending = participant
                            else onPick(participant.name)
                        }
                    )
                }
            }
        }
    }
    pending?.let { participant ->
        val stat = rotation[participant.id]
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Recent assignment") },
            text = {
                val message = when {
                    stat == null -> "Continue assigning ${participant.name}?"
                    stat.totalThisMonth > 0 -> "${participant.name} has ${stat.totalThisMonth} assignment(s) this month. Main: ${stat.mainThisMonth}; assistant: ${stat.assistantThisMonth}. Suggested next turn: ${stat.nextEligibleDate?.let(::rotationShortDate) ?: "not set"}."
                    else -> "${participant.name} is not yet eligible. Next eligible date: ${stat.nextEligibleDate?.let(::rotationShortDate) ?: "not set"}."
                }
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = { onPick(participant.name); pending = null }) { Text("Assign anyway") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Choose another") }
            }
        )
    }
}

private fun participantRotationRank(stat: ParticipantRotation?): Int = when {
    stat == null -> 9
    stat.exempt -> 8
    stat.neverAssigned -> 0
    stat.recommended -> 1
    stat.eligible && stat.totalThisMonth == 0 -> 2
    stat.totalThisMonth == 0 -> 3
    else -> 4 + minOf(stat.totalThisMonth, 3)
}

@Composable
private fun RotationStatusTag(stat: ParticipantRotation) {
    val label = when {
        stat.exempt -> "Exempt"
        stat.neverAssigned -> "Priority"
        stat.recommended -> "Recommended"
        stat.totalThisMonth > 0 -> "${stat.totalThisMonth} this month"
        stat.eligible -> "Available"
        else -> stat.nextEligibleDate?.let { "After ${rotationShortDate(it)}" } ?: "Wait"
    }
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when {
        stat.exempt -> scheme.surfaceVariant to scheme.onSurfaceVariant
        stat.neverAssigned -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        stat.recommended -> scheme.primaryContainer to scheme.onPrimaryContainer
        stat.eligible -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun rotationShortDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d", Locale("en", "PH")))
}.getOrDefault(value)

@Composable
private fun PreviewScreen(modifier: Modifier, weekStart:String, bible:String, openingSong:String, closingSong:String, parts:List<Assignment>, chairman:String, openingPrayer:String, adviser:String, audienceGroup:String, previewWebView:MutableState<WebView?>, onExportText:()->Unit, onPng:()->Unit) {
    val html=remember(weekStart,bible,openingSong,closingSong,parts.toList(),chairman,openingPrayer,adviser,audienceGroup) {
        buildOfficialScheduleHtml(weekStart,bible,openingSong,closingSong,parts,chairman,openingPrayer,adviser,audienceGroup)
    }
    var previewReady by remember(html){mutableStateOf(false)}
    PageColumn(modifier) {
        PageHeading("SCHEDULE TOOLS", "Preview & Export", "Official meeting schedule · tugma ang preview at PNG output.")
        Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick=onPng, enabled=previewReady, modifier=Modifier.weight(1f).height(48.dp),
                shape=RoundedCornerShape(16.dp), contentPadding=PaddingValues(horizontal=10.dp)
            ) {
                Icon(Icons.TwoTone.Share, contentDescription=null, modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share PNG",fontSize=13.sp,maxLines=1)
            }
            OutlinedButton(
                onClick=onExportText, modifier=Modifier.weight(1f).height(48.dp),
                shape=RoundedCornerShape(16.dp), contentPadding=PaddingValues(horizontal=10.dp)
            ) {
                Icon(Icons.TwoTone.Description, contentDescription=null, modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export text",fontSize=13.sp,maxLines=1)
            }
        }
        Surface(color=Color.White,contentColor=Color(0xFF171717),shape=RoundedCornerShape(6.dp),shadowElevation=4.dp,modifier=Modifier.fillMaxWidth()) {
            OfficialScheduleWebPreview(html,previewWebView,Modifier.fillMaxWidth()){previewReady=true}
        }
    }
}

@Composable
private fun OfficialScheduleWebPreview(html:String,previewWebView:MutableState<WebView?>,modifier:Modifier=Modifier,onPageRendered:()->Unit) {
    var pageHeight by remember(html){mutableStateOf(560.dp)}
    AndroidView(factory={context->WebView(context).apply{
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);setBackgroundColor(android.graphics.Color.WHITE);settings.javaScriptEnabled=true
        tag=html
        webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView,url:String?){view.evaluateJavascript("document.body.getBoundingClientRect().height.toString()") { raw->pageHeight=((raw.trim('"').toFloatOrNull()?:560f)+4f).dp;onPageRendered() }}}
        previewWebView.value=this
        loadDataWithBaseURL("file:///android_asset/",html,"text/html","UTF-8",null)
    }},modifier=modifier.height(pageHeight),update={view->previewWebView.value=view;if(view.tag!=html){view.tag=html;view.loadDataWithBaseURL("file:///android_asset/",html,"text/html","UTF-8",null)}})
}

private fun buildOfficialScheduleText(
    weekStart: String, bible: String, openingSong: String, closingSong: String, parts: List<Assignment>,
    chairman: String, openingPrayer: String, adviser: String, audienceGroup: String
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
                        append(part.time.ifBlank { "—" }).append("  •  ").append(number).append(". ").append(title)
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
                            val qualified = part.section == "living" || (part.section == "treasures" && !isBibleReading(part)) || isWhat(part)
                            appendLine("       ${if (qualified) "Qualified brother" else "Participant"} : ${part.person.ifBlank { "—" }}")
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
        appendLine()
        appendLine("Nabuo sa Meeting Scheduler")
    }
}

private fun buildOfficialScheduleHtml(weekStart:String,bible:String,openingSong:String,closingSong:String,parts:List<Assignment>,chairman:String,openingPrayer:String,adviser:String,audienceGroup:String):String {
    val start=runCatching{LocalDate.parse(weekStart)}.getOrDefault(LocalDate.now())
    val end=start.plusDays(6)
    val months=listOf("Enero","Pebrero","Marso","Abril","Mayo","Hunyo","Hulyo","Agosto","Setyembre","Oktubre","Nobyembre","Disyembre")
    val weekText=if(start.month==end.month) "${months[start.monthValue-1]} ${start.dayOfMonth}-${end.dayOfMonth}" else "${months[start.monthValue-1]} ${start.dayOfMonth}–${months[end.monthValue-1]} ${end.dayOfMonth}"
    fun minutes(time:String):Int { val p=time.split(":");return if(p.size==2) (p[0].toIntOrNull()?:0)*60+(p[1].toIntOrNull()?:0) else 0 }
    fun clock(value:Int):String { val hour=(value/60)%12; return "${if(hour==0)12 else hour}:${(value%60).toString().padStart(2,'0')}" }
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
        append("<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>")
        append("*{box-sizing:border-box}html,body{margin:0;background:#fff;color:#171717;font-family:'Google Sans','Google Sans Text',Roboto,Arial,sans-serif}body{padding:18px 20px 14px;font-size:6.4px;line-height:1.25}.title{font-size:9.5px;font-weight:700;padding-bottom:5px;border-bottom:1px solid #252525}.sub{margin:7px 0 14px;color:#397782;font-size:7.2px}.toprow{display:grid;grid-template-columns:30px minmax(0,1fr) minmax(95px,.9fr);gap:5px;align-items:start;margin:0 0 5px}.time{white-space:nowrap}.bullet{font-size:6px;margin-right:3px}.role strong{margin-right:3px}.advisor{margin:7px 0 16px}.section{display:flex;align-items:center;gap:6px;margin:16px 0 8px;font-size:7.8px;font-weight:600}.badge{width:17px;height:17px;border-radius:4px;overflow:hidden;flex:0 0 17px}.badge img{display:block;width:100%;height:100%}.program{display:grid;grid-template-columns:30px minmax(80px,1.55fr) minmax(62px,1fr) minmax(62px,1fr);gap:5px;align-items:start;margin:0 0 8px}.part{overflow-wrap:anywhere}.dot{font-weight:bold;margin-right:3px}.hall{font-size:5.6px;color:#555;text-align:left;margin-bottom:2px}.name{font-size:6.4px;font-weight:700;overflow-wrap:anywhere}.assistant{font-size:5.8px;font-weight:400;color:#777;margin-top:2px;overflow-wrap:anywhere}.single{grid-column:3/5}.footer{border-top:1px solid #ddd;margin-top:14px;padding-top:8px}.footnote{text-align:right;color:#999;font-size:5px;margin-top:20px}@media print{body{padding:18px 20px 14px}}")
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
                append("<div class=\"program\"><div class=\"time\">").append(p.time.ifBlank{"—"}.htmlSafe()).append("</div><div class=\"part\"><span class=\"dot\" style=\"color:").append(bulletColor).append("\">●</span>").append(p.title.replaceFirst(Regex("^\\s*\\d+\\.\\s*"),"").htmlSafe())
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
        append("<div class=\"footnote\">Nabuo sa Meeting Scheduler</div></body></html>")
    }
    return html
}

@Composable
private fun ParticipantsScreen(
    modifier: Modifier, participants: List<Participant>, onAdd: (String) -> Unit,
    onToggle: (Int) -> Unit, onEdit: (Participant, Participant) -> Unit,
    isAdmin: Boolean, onRefresh: () -> Unit,
    onSetActive: (Participant, Boolean) -> Unit, onDelete: (Participant) -> Unit
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
    val participantsSnapshot = participants.toList()
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
    Column(modifier.fillMaxSize().padding(PaddingValues(start=18.dp,end=18.dp,top=12.dp,bottom=20.dp)),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column { Text("CLOUD DATA",color=Forest,fontSize=10.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=1.3.sp);Text("Participants",fontSize=25.sp,fontWeight=FontWeight.Bold) }
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onRefresh,enabled=isAdmin,modifier=Modifier.size(40.dp)){Icon(Icons.TwoTone.Refresh,contentDescription="Refresh participants",tint=if(isAdmin) Forest else Muted,modifier=Modifier.size(20.dp))}
                IconButton(onClick={showAdd=true},modifier=Modifier.size(40.dp)){Icon(Icons.TwoTone.PersonAdd,contentDescription="Add participant",tint=Forest,modifier=Modifier.size(22.dp))}
            }
        }
        Text("${participants.size} participants · ${filtered.size} shown",color=Muted,fontSize=12.sp)
        Card(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=SurfaceContainer),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(query,{query=it},placeholder={Text("Search participant")},singleLine=true,shape=RoundedCornerShape(18.dp),modifier=Modifier.weight(1f),textStyle=MaterialTheme.typography.bodyMedium)
                    IconButton(onClick={showFilters=true},modifier=Modifier.size(40.dp)){Icon(Icons.TwoTone.Sort,contentDescription="Filter and sort participants",tint=Forest,modifier=Modifier.size(22.dp))}
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=6.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(filtered,key={_,entry->if(entry.value.id>0)entry.value.id.toLong() else entry.index.toLong()}) { _,entry ->
                        val (index,p)=entry
                        HorizontalDivider(color=SurfaceContainer)
                        Row(Modifier.fillMaxWidth().clickable { beginEdit(p) }.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(ForestContainer,CircleShape),contentAlignment=Alignment.Center){Text(p.name.split(" ").take(2).mapNotNull{it.firstOrNull()}.joinToString("").uppercase(),color=Forest,fontWeight=FontWeight.Bold,fontSize=11.sp)}
                            Column(Modifier.weight(1f).padding(start=10.dp)) {
                                Text(p.name,fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1)
                                Text(
                                    if (p.unbaptized) "Unbaptized · ${genderLabel(p.gender)}${if(!p.enabled) " · Inactive" else ""}"
                                    else "${appointmentLabel(p.appointment)} · ${serviceLabel(p.service)} · ${genderLabel(p.gender)}${if(!p.enabled) " · Inactive" else ""}",
                                    color=Muted,fontSize=10.sp,maxLines=1
                                )
                            }
                            IconButton(onClick={beginEdit(p)},modifier=Modifier.size(40.dp)){Icon(Icons.TwoTone.Edit,contentDescription="Edit participant",tint=Forest,modifier=Modifier.size(20.dp))}
                            if(isAdmin) {
                                var menu by remember(p.id){mutableStateOf(false)}
                                Box {
                                    TextButton(onClick={menu=true},contentPadding=PaddingValues(horizontal=5.dp)){Text("⋮",fontSize=20.sp,color=Forest)}
                                    DropdownMenu(expanded=menu,onDismissRequest={menu=false},modifier=Modifier.background(AppSurface)) {
                                        DropdownMenuItem(text={Text(if(p.enabled) "Set inactive" else "Set active")},onClick={menu=false;onSetActive(p,!p.enabled)})
                                        DropdownMenuItem(text={Text("Delete permanently",color=MaterialTheme.colorScheme.error)},onClick={menu=false;deleteTarget=p})
                                    }
                                }
                            }
                        }
                    }
                    if(filtered.isEmpty()) item { Text("Walang participant na tugma sa search o filters.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=12.dp)) }
                }
            }
        }
    }
    if(showAdd) AlertDialog(onDismissRequest={showAdd=false},containerColor=AppSurface,shape=RoundedCornerShape(28.dp),title={Text("Add participant")},text={OutlinedTextField(newName,{newName=it},label={Text("Full name")},singleLine=true)},confirmButton={TextButton(onClick={if(newName.isNotBlank())onAdd(newName.trim());newName="";showAdd=false}){Text("Add",color=Forest)}},dismissButton={TextButton(onClick={showAdd=false}){Text("Cancel",color=Forest)}})
    if(editing!=null) AlertDialog(
        onDismissRequest={editing=null},containerColor=AppSurface,shape=RoundedCornerShape(28.dp),title={Text("Edit participant",fontWeight=FontWeight.Bold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(editName,{editName=it},label={Text("Name")},singleLine=true,modifier=Modifier.fillMaxWidth())
            ParticipantChoice("Gender",editGender,listOf("brother" to "Brother","sister" to "Sister")){editGender=it}
            ParticipantChoice("Appointment",editAppointment,listOf("" to "","publisher" to "Publisher","ministerial" to "Ministerial servant","elder" to "Elder"),enabled=!editUnbaptized){editAppointment=it;if(it!="publisher")editUnbaptized=false}
            ParticipantChoice("Service status",editService,listOf("" to "","none" to "Publisher","regular_pioneer" to "Regular pioneer"),enabled=!editUnbaptized){editService=it;if(it=="regular_pioneer")editUnbaptized=false}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Unbaptized",fontSize=14.sp);Switch(checked=editUnbaptized,onCheckedChange={editUnbaptized=it;if(it){editAppointment="";editService=""}},colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Forest))}
        }},
        confirmButton={TextButton(onClick={editing?.let{old->if(editName.isNotBlank())onEdit(old,old.copy(name=editName.trim(),gender=editGender,appointment=editAppointment,service=editService,unbaptized=editUnbaptized))};editing=null}){Text("Save",color=Forest)}},
        dismissButton={TextButton(onClick={editing=null}){Text("Cancel",color=Forest)}}
    )
    if(showFilters) AlertDialog(
        onDismissRequest={showFilters=false},containerColor=AppSurface,shape=RoundedCornerShape(28.dp),title={Text("Filter and sort",fontWeight=FontWeight.Bold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
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
    if(deleteTarget!=null) AlertDialog(onDismissRequest={deleteTarget=null},containerColor=AppSurface,shape=RoundedCornerShape(28.dp),title={Text("Delete participant?")},text={Text("Permanent delete removes ${deleteTarget?.name} and their assignment history. This cannot be undone.")},confirmButton={TextButton(onClick={deleteTarget?.let(onDelete);deleteTarget=null}){Text("Delete",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleteTarget=null}){Text("Cancel",color=Forest)}})
}

private fun appointmentLabel(value: String) = when(value.lowercase()) { "" -> ""; "elder" -> "Elder"; "ministerial" -> "Ministerial"; "publisher" -> "Publisher"; else -> "Publisher" }
private fun serviceLabel(value: String) = when(value.lowercase()) { "" -> ""; "regular_pioneer" -> "Regular pioneer"; "none" -> "Publisher"; else -> "Publisher" }
private fun genderLabel(value: String) = when(value.lowercase()) { "sister" -> "Sister"; "brother" -> "Brother"; else -> "Unspecified" }

@Composable
private fun ParticipantChoice(label: String, value: String, options: List<Pair<String,String>>, enabled: Boolean = true, onSelect: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color=Muted, fontSize=11.sp)
        Box {
            OutlinedButton(onClick={expanded=true}, enabled=enabled, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp), contentPadding=PaddingValues(horizontal=13.dp, vertical=9.dp)) {
                Text(options.firstOrNull { it.first==value }?.second?.ifBlank { " " } ?: options.first().second.ifBlank { " " }, color=Ink, modifier=Modifier.weight(1f))
            }
            DropdownMenu(expanded=expanded, onDismissRequest={expanded=false}, modifier=Modifier.background(AppSurface)) {
                options.forEach { (key,title) -> DropdownMenuItem(text={Text(title.ifBlank { " " })}, enabled=enabled, onClick={onSelect(key);expanded=false}) }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Column(Modifier.padding(12.dp)) { Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = Forest, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun HistoryScreen(
    modifier: Modifier,
    remoteSchedules: List<RemoteSchedule>,
    cloudStatus: String,
    isAdmin: Boolean,
    onView: (RemoteSchedule) -> Unit,
    onEdit: (RemoteSchedule) -> Unit,
    onDelete: (RemoteSchedule) -> Unit,
    onNew: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<RemoteSchedule?>(null) }
    val sortedSchedules = remoteSchedules.sortedByDescending { scheduleStartDate(it.weekLabel) ?: LocalDate.MIN }
    PageColumn(modifier) {
        PageHeading("SAVED SCHEDULES", "Schedule History", "Mga schedule na naka-save sa cloud database.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${remoteSchedules.size} saved · newest first", color = Muted, fontSize = 12.sp)
            FilledTonalButton(onClick = onNew, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.TwoTone.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New schedule")
            }
        }
        if (cloudStatus.isNotBlank()) Text(cloudStatus, color = Muted, fontSize = 11.sp)
        M3Card {
            if (remoteSchedules.isEmpty()) {
                Text("Wala pang schedule sa cloud database.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))
            }
            sortedSchedules.forEach { remote ->
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = SurfaceContainer)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(ForestContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.TwoTone.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                        Text(remote.weekLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                        Text("${remote.reading} · ${remote.updatedAt.take(10)}", color = Muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = { onView(remote) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.TwoTone.Visibility, contentDescription = null, tint = Forest, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View", fontSize = 11.sp, color = Forest)
                    }
                    if (isAdmin) {
                        var menu by remember(remote.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.TwoTone.MoreVert, contentDescription = "Schedule actions", tint = Forest)
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(AppSurface)) {
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.TwoTone.Edit, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Edit") } },
                                    onClick = { menu = false; onEdit(remote) }
                                )
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.TwoTone.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Delete", color = MaterialTheme.colorScheme.error) } },
                                    onClick = { menu = false; deleteTarget = remote }
                                )
                            }
                        }
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
            text = { Text("${item.weekLabel} will be removed from schedule history. Assignment history will be archived safely.", color = Muted) },
            confirmButton = { TextButton(onClick = { onDelete(item); deleteTarget = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
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
            return when(normalized){"ene"->1;"feb","peb"->2;"mar"->3;"abr"->4;"may"->5;"jun","hun"->6;"jul","hul"->7;"ago"->8;"sep","set"->9;"oct","okt"->10;"nov","nob"->11;"dec","dic","dis"->12;else->Month.valueOf(normalized.uppercase(Locale.US)).value}
        }
        val startMonth=monthValue(match.groupValues[1]); val endMonth=monthValue(match.groupValues[3].ifBlank { match.groupValues[1] })
        val year=match.groupValues[5].toIntOrNull() ?: updatedAt.take(4).toIntOrNull() ?: LocalDate.now().year
        LocalDate.of(if(startMonth>endMonth) year-1 else year,startMonth,match.groupValues[2].toInt())
    }
}.getOrNull()

@Composable
private fun AccountsScreen(
    modifier: Modifier, accounts: List<RemoteAccount>, loading: Boolean, error: String?, currentUsername: String,
    onRefresh: () -> Unit, onUploadPhoto: (RemoteAccount) -> Unit,
    onAction: (RemoteAccount, String, String, String) -> Unit,
    onUpdateToolAccess: (RemoteAccount, Map<String, String>) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<RemoteAccount?>(null) }
    var actionTarget by remember { mutableStateOf<RemoteAccount?>(null) }
    var selectedAction by remember { mutableStateOf("") }
    var temporaryPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("viewer") }
    val filtered = accounts.sortedWith(compareBy<RemoteAccount>({ if (it.role == "admin") 0 else 1 }, { when (it.status) { "requested" -> 0; "active" -> 1; else -> 2 } }, { it.displayName.lowercase() }))
    fun beginAction(account: RemoteAccount, action: String) {
        selectedAccount = null
        actionTarget = account
        selectedAction = action
        temporaryPassword = ""
        selectedRole = account.role.ifBlank { "viewer" }
    }
    PageColumn(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("ADMINISTRATION", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp); Text("Accounts & Access", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(40.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest)
                else Icon(Icons.TwoTone.Refresh, contentDescription = "Refresh accounts", tint = Forest, modifier = Modifier.size(20.dp))
            }
        }
        Text("Tap an account photo to view details and actions.", color = Muted, fontSize = 12.sp)
        M3Card {
            if (loading && accounts.isEmpty()) Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest, modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            if (accounts.isEmpty() && !loading && error == null) Text("No accounts available.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            filtered.chunked(3).forEach { rowAccounts ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowAccounts.forEach { account ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { selectedAccount = account }.padding(vertical = 10.dp, horizontal = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box {
                                AccountPhotoAvatar(account, 68.dp)
                                if (account.status == "requested") Box(Modifier.align(Alignment.TopEnd).size(11.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                                if (account.role.equals("admin", true)) {
                                    Surface(Modifier.align(Alignment.BottomEnd).size(24.dp), shape = CircleShape, color = ForestContainer, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.TwoTone.AdminPanelSettings, contentDescription = "Administrator", tint = Forest, modifier = Modifier.size(15.dp)) }
                                    }
                                }
                            }
                        }
                    }
                    repeat(3 - rowAccounts.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (filtered.isEmpty() && accounts.isNotEmpty()) Text("No accounts available.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
        }
    }
    selectedAccount?.let { selected ->
        val account = accounts.firstOrNull { it.id == selected.id } ?: selected
        val roleName = when (account.role.lowercase()) { "admin" -> "Administrator"; "scheduler" -> "Scheduler"; "viewer" -> "Viewer"; else -> account.role.replaceFirstChar { it.uppercase() } }
        val statusName = when (account.status.lowercase()) { "requested" -> "Requested"; "active" -> "Active"; "disabled" -> "Disabled"; else -> account.status.replaceFirstChar { it.uppercase() } }
        val isSelf = account.username.equals(currentUsername, true) || account.role == "admin"
        Dialog(onDismissRequest = { selectedAccount = null }) {
            Surface(color = AppSurface, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp).heightIn(max = 660.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountPhotoAvatar(account, 64.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(account.displayName.ifBlank { account.username }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("@" + account.username, color = Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedAccount = null }, modifier = Modifier.size(40.dp)) { Icon(Icons.TwoTone.Close, contentDescription = "Close account details", tint = Forest, modifier = Modifier.size(20.dp)) }
                    }
                    HorizontalDivider(color = SurfaceContainer)
                    AccountInfoLine("Name", account.displayName.ifBlank { account.username })
                    AccountInfoLine("Username", "@" + account.username)
                    AccountInfoLine("Role", roleName)
                    AccountInfoLine("Status", statusName, if (account.status == "requested") MaterialTheme.colorScheme.error else Forest)
                    if (account.status == "requested") {
                        Text("Message for Administrator", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Surface(color = SurfaceContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(account.requestNote.ifBlank { "No message was included with this request." }, color = Ink, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    OutlinedButton(onClick = { onUploadPhoto(account) }, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(16.dp)) {
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
                        HorizontalDivider(color = SurfaceContainer)
                        Text("Tool access", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (account.role == "viewer") "Viewer access is always read-only." else "Set each tool to hidden, view only, or edit.",
                            color = Muted, fontSize = 11.sp
                        )
                        toolLabels.forEach { (key, label) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val modes = if (account.role == "viewer") listOf("hidden" to "Hidden", "view" to "View") else listOf("hidden" to "Hidden", "view" to "View", "edit" to "Edit")
                                    modes.forEach { (mode, caption) ->
                                        FilterChip(
                                            selected = toolAccessDraft[key] == mode,
                                            onClick = { toolAccessDraft = toolAccessDraft + (key to mode) },
                                            label = { Text(caption, fontSize = 9.sp, maxLines = 1) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { onUpdateToolAccess(account, toolAccessDraft) },
                            enabled = toolAccessDraft != account.toolAccess,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Icon(Icons.TwoTone.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Save tool access", fontSize = 12.sp)
                        }
                    }
                    if (!isSelf) {
                        HorizontalDivider(color = SurfaceContainer)
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
            onDismissRequest = { actionTarget = null }, containerColor = AppSurface, shape = RoundedCornerShape(28.dp),
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
    val bitmap = remember(account.photoDataUrl) {
        account.photoDataUrl.substringAfter(",", "").takeIf { it.isNotBlank() }?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
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
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 42.dp), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 11.sp, maxLines = 1)
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
