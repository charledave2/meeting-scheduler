package com.example.meetingscheduler

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.SecureRandom
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal data class ApiIdentity(val name: String, val username: String, val role: String, val mustChangePassword: Boolean, val photoDataUrl: String = "", val toolAccess: Map<String, String> = emptyMap())
internal data class WorkbookImport(val link: String, val meta: JSONObject, val parts: List<JSONObject>)
internal data class RemoteSchedule(
    val id: Int,
    val weekLabel: String,
    val reading: String,
    val updatedAt: String,
    val weekStart: String = "",
    val assignedNames: List<String> = emptyList()
)
internal data class SchedulePage(val schedules: List<RemoteSchedule>, val nextOffset: Int, val hasMore: Boolean, val years: List<Int>)
internal data class RemoteAccount(val id: Int, val displayName: String, val username: String, val status: String, val role: String, val createdAt: String, val requestNote: String, val photoDataUrl: String = "", val toolAccess: Map<String, String> = emptyMap(), val memberId: Int? = null, val memberName: String = "", val lastLoginAt: String = "")
internal data class AccountLinkParticipant(val id: Int, val name: String)
internal data class ParticipantRotation(
    val memberId: Int, val mainThisMonth: Int, val assistantThisMonth: Int, val totalThisMonth: Int,
    val lastAssignmentDate: String?, val nextEligibleDate: String?, val eligible: Boolean, val exempt: Boolean,
    val neverAssigned: Boolean, val recommended: Boolean
)

internal class SchedulerApi(private val context: Context) {
    private val vault = SessionVault(context)
    private val base = "https://meeting-scheduler.yelyahwilliams2005.workers.dev"

    suspend fun login(username: String, password: String): ApiIdentity = withContext(Dispatchers.IO) {
        val response = try {
            request("/api/auth/login", "POST", JSONObject().put("username", username).put("password", password), authorized = false, captureCookie = true)
        } catch (error: Exception) {
            val detail = error.message?.takeIf { it.isNotBlank() } ?: "No additional details"
            throw IllegalStateException("Could not complete sign-in (${error.javaClass.simpleName}): $detail", error)
        }
        if (!response.optString("redirectTo").startsWith("/")) throw IllegalStateException("The server did not confirm sign-in.")
        try {
            meBlocking()
        } catch (error: Exception) {
            val detail = error.message?.takeIf { it.isNotBlank() } ?: "No additional details"
            throw IllegalStateException("Could not verify the sign-in session (${error.javaClass.simpleName}): $detail", error)
        }
    }

    fun hasStoredSession(): Boolean = vault.read() != null

    suspend fun currentUser(): ApiIdentity = withContext(Dispatchers.IO) { meBlocking() }
    private fun digest(value: String) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun removalKey(cookie: String) = digest("remove-push-registration:" + cookie)
    fun deliveryBinding() = vault.read()?.let { digest(removalKey(it)) }

    suspend fun removeOldRegistration(key: String) = withContext(Dispatchers.IO) {
        request("/api/push-token", "DELETE", JSONObject().put("removalKey", key), authorized = false)
    }

    suspend fun registerPushToken(token: String) = withContext(Dispatchers.IO) {
        val cookie = vault.read() ?: return@withContext
        request("/api/push-token", "POST", JSONObject().put("token", token).put("provider", "fcm")
            .put("platform", "android").put("deliveryBinding", digest(removalKey(cookie))), sessionOverride = cookie)
        Unit
    }

    suspend fun sendTestNotification(): Int = withContext(Dispatchers.IO) {
        request("/api/push-token/test", "POST", JSONObject()).optInt("sent")
    }

    suspend fun unregisterPushToken(token: String, sessionOverride: String?) = withContext(Dispatchers.IO) {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        request("/api/push-token?token=$encodedToken&provider=fcm", "DELETE", null, authorized = false, sessionOverride = sessionOverride)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) = withContext(Dispatchers.IO) {
        request(
            "/api/auth/change-password",
            "POST",
            JSONObject().put("currentPassword", currentPassword).put("newPassword", newPassword),
            captureCookie = true
        )
    }
    suspend fun requestAccess(displayName: String, username: String, requestNote: String) = withContext(Dispatchers.IO) {
        request(
            "/api/auth/request",
            "POST",
            JSONObject().put("displayName", displayName).put("username", username).put("requestNote", requestNote),
            authorized = false
        )
    }


    suspend fun saveSchedule(weekLabel: String, weekStart: String, reading: String, openingSong: String, closingSong: String,
                             parts: List<JSONObject>, chairman: String, openingPrayer: String, adviser: String, audienceGroup: String,
                             sourceUrl: String, id: Int = 0, expectedUpdatedAt: String = ""): JSONObject = withContext(Dispatchers.IO) {
        val meta = JSONObject().put("date", weekLabel).put("weekStart", weekStart).put("reading", reading)
            .put("openingSong", openingSong).put("closingSong", closingSong).put("chairman", chairman)
            .put("openingPrayer", openingPrayer).put("adviser", adviser).put("audienceGroup", audienceGroup)
        val body = JSONObject().put("meta", meta).put("parts", JSONArray(parts)).put("sourceUrl", sourceUrl)
        if (id > 0) body.put("id", id).put("expectedUpdatedAt", expectedUpdatedAt)
        request("/api/schedules", "POST", body)
    }

    suspend fun schedules(): List<RemoteSchedule> = schedulePage(limit = 100).schedules

    suspend fun schedulePage(offset: Int = 0, limit: Int = 30): SchedulePage = withContext(Dispatchers.IO) {
        val response = request("/api/schedules?offset=${offset.coerceAtLeast(0)}&limit=${limit.coerceIn(1, 100)}", "GET", null)
        val rows = response.optJSONArray("schedules") ?: JSONArray()
        val schedules = (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { row ->
            val names = row.optJSONArray("assignedNames")
            RemoteSchedule(
                row.optInt("id"), row.optString("weekLabel"), row.optString("bibleReading"), row.optString("updatedAt"),
                row.optString("weekStart"),
                (0 until (names?.length() ?: 0)).mapNotNull { index -> names?.optString(index)?.trim()?.takeIf { it.isNotBlank() } }
            )
        } }
        val years = response.optJSONArray("years")?.let { values ->
            (0 until values.length()).mapNotNull { index -> values.optInt(index).takeIf { it > 0 } }
        }.orEmpty()
        SchedulePage(schedules, response.optInt("nextOffset", offset + rows.length()), response.optBoolean("hasMore"), years)
    }

    suspend fun schedule(id: Int): JSONObject = withContext(Dispatchers.IO) { request("/api/schedules?id=$id", "GET", null).optJSONObject("schedule") ?: throw IllegalStateException("Hindi mabasa ang schedule.") }
    suspend fun deleteSchedule(id: Int): JSONObject = withContext(Dispatchers.IO) { request("/api/schedules?id=$id", "DELETE", null) }

    suspend fun members(includeInactive: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members${if (includeInactive) "?includeInactive=true" else ""}", "GET", null)
    }

    suspend fun addMember(name: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members", "POST", JSONObject().put("name", name)).optJSONObject("member")
            ?.takeIf { it.optInt("id") > 0 }
            ?: throw IllegalStateException("Walang participant ID mula sa website.")
    }

    suspend fun participantRotation(date: String): List<ParticipantRotation> = withContext(Dispatchers.IO) {
        val rows = request("/api/rotation?date=$date", "GET", null).optJSONArray("stats") ?: JSONArray()
        (0 until rows.length()).mapNotNull { index -> rows.optJSONObject(index)?.let { row ->
            fun optionalDate(key: String) = row.optString(key).takeIf { it.isNotBlank() && it != "null" }
            ParticipantRotation(
                memberId = row.optInt("memberId"),
                mainThisMonth = row.optInt("mainThisMonth"),
                assistantThisMonth = row.optInt("assistantThisMonth"),
                totalThisMonth = row.optInt("totalThisMonth"),
                lastAssignmentDate = optionalDate("lastAssignmentDate"),
                nextEligibleDate = optionalDate("nextEligibleDate"),
                eligible = row.optBoolean("eligible"),
                exempt = row.optBoolean("exempt"),
                neverAssigned = row.optBoolean("neverAssigned"),
                recommended = row.optBoolean("recommended")
            )
        } }
    }

    suspend fun updateMember(id: Int, name: String, appointment: String, serviceStatus: String, gender: String, unbaptized: Boolean): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members", "PATCH", JSONObject().put("id", id).put("name", name).put("appointment", appointment)
            .put("serviceStatus", serviceStatus).put("gender", gender).put("unbaptized", unbaptized)).optJSONObject("member")
            ?: throw IllegalStateException("Walang participant response mula sa website.")
    }

    suspend fun setMemberActive(id: Int, active: Boolean): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members", "PATCH", JSONObject().put("id", id).put("active", active))
    }

    suspend fun deleteMember(id: Int): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members?id=$id", "DELETE", null)
    }

    suspend fun accountsWithParticipants(): Pair<List<RemoteAccount>, List<AccountLinkParticipant>> = withContext(Dispatchers.IO) {
        val response = request("/api/accounts", "GET", null)
        val rows = response.optJSONArray("accounts") ?: JSONArray()
        val accounts = (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { row ->
            RemoteAccount(row.optInt("id"), row.optString("displayName"), row.optString("username"), row.optString("status"), row.optString("role", "viewer"), row.optString("createdAt"), row.optString("requestNote"), row.optString("photoDataUrl"), row.optJSONObject("toolAccess")?.let { access -> buildMap { access.keys().forEach { key -> put(key, access.optString(key, "hidden")) } } } ?: emptyMap(), row.optInt("memberId").takeIf { it > 0 }, row.optString("memberName"), row.optString("lastLoginAt"))
        } }
        val members = response.optJSONArray("activeMembers") ?: JSONArray()
        val participants = (0 until members.length()).mapNotNull { index ->
            members.optJSONObject(index)?.let { row -> AccountLinkParticipant(row.optInt("id"), row.optString("name")) }
        }
        accounts to participants
    }

    suspend fun accounts(): List<RemoteAccount> = accountsWithParticipants().first

    suspend fun linkAccountParticipant(accountId: Int, memberId: Int?): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().put("id", accountId).put("action", "link_member")
        if (memberId != null) body.put("memberId", memberId) else body.put("memberId", JSONObject.NULL)
        request("/api/accounts", "PATCH", body)
    }

    suspend fun updateAccountToolAccess(id: Int, toolAccess: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val access = JSONObject().apply { toolAccess.forEach { (key, value) -> put(key, value) } }
        request("/api/accounts", "PATCH", JSONObject().put("id", id).put("action", "update_tool_access").put("toolAccess", access))
    }

    suspend fun uploadAccountPhoto(id: Int, photoDataUrl: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/accounts", "PATCH", JSONObject().put("id", id).put("action", "upload_photo").put("photoDataUrl", photoDataUrl))
    }

    suspend fun accountAction(id: Int, action: String, role: String = "", temporaryPassword: String = ""): JSONObject = withContext(Dispatchers.IO) {
        if (action == "delete") return@withContext request("/api/accounts?id=$id", "DELETE", null)
        val body = JSONObject().put("id", id).put("action", action)
        if (role.isNotBlank()) body.put("role", role)
        if (temporaryPassword.isNotBlank()) body.put("temporaryPassword", temporaryPassword)
        request("/api/accounts", "PATCH", body)
    }

    private fun meBlocking(): ApiIdentity {
        val data = request("/api/auth/me", "GET", null)
        val user = data.optJSONObject("user") ?: throw IllegalStateException("Hindi mabasa ang account.")
        val access = user.optJSONObject("toolAccess")?.let { values -> buildMap { values.keys().forEach { key -> put(key, values.optString(key, "hidden")) } } } ?: emptyMap()
        return ApiIdentity(user.optString("displayName"), user.optString("username"), user.optString("role"), user.optBoolean("mustChangePassword"), user.optString("photoDataUrl"), access)
    }

    suspend fun uploadOwnProfilePhoto(photoDataUrl: String) = withContext(Dispatchers.IO) {
        request("/api/auth/me", "PATCH", JSONObject().put("photoDataUrl", photoDataUrl))
    }

    suspend fun importForWeek(weekStart: String, onProgress: suspend (String) -> Unit = {}): WorkbookImport = withContext(Dispatchers.IO) {
        val date = LocalDate.parse(weekStart).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val wolUrl = "https://wol.jw.org/tl/wol/dt/r27/lp-tg/${date.year}/${date.monthValue}/${date.dayOfMonth}"
        onProgress("GET · WOL meeting page · ${date}")
        val html = fetchText(wolUrl)
        onProgress("PARSE · looking for workbook document IDs")
        val candidates = workbookDocumentIds(html, date)
        onProgress("FOUND · ${candidates.size} workbook candidate(s)")
        if (candidates.isEmpty()) throw IllegalStateException("Walang nailathalang workbook para sa linggong ito.")
        var lastError: Throwable? = null
        for (docId in candidates) {
            val link = "https://www.jw.org/finder?srcid=jwlshare&wtlocale=TG&prefer=lang&docid=$docId"
            try {
                onProgress("POST · /api/import-workbook · doc $docId")
                val result = request("/api/import-workbook", "POST", JSONObject().put("url", link))
                val meta = result.optJSONObject("meta") ?: throw IllegalStateException("Hindi kumpleto ang workbook response.")
                val rawParts = result.optJSONArray("parts") ?: throw IllegalStateException("Walang meeting parts sa workbook.")
                val parts = (0 until rawParts.length()).mapNotNull { rawParts.optJSONObject(it) }
                if (parts.isEmpty()) throw IllegalStateException("Walang meeting parts sa workbook.")
                onProgress("PARSED · metadata plus ${parts.size} meeting parts")
                return@withContext WorkbookImport(link, meta, parts)
            } catch (error: Throwable) {
                lastError = error
                if (error.message?.contains("format was not recognized", ignoreCase = true) != true) throw error
            }
        }
        throw IllegalStateException(lastError?.message ?: "Hindi makuha ang tamang workbook.")
    }


    suspend fun capturePublicWorkbook(
        weekStart: String,
        onProgress: suspend (String) -> Unit
    ): WorkbookImport = withContext(Dispatchers.IO) {
        val date = LocalDate.parse(weekStart)
        val basePath = "https://wol.jw.org/tl/wol"
        onProgress("Opening the JW.ORG meeting page…")
        val page = fetchText("$basePath/dt/r27/lp-tg/${date.year}/${date.monthValue}/${date.dayOfMonth}")
        val candidates = workbookDocumentIds(page, date)
        onProgress("Found ${candidates.size} workbook candidates for this week.")
        for (document in candidates) {
            onProgress("Checking workbook $document…")
            val url = "$basePath/d/r27/lp-tg/$document"
            val html = try { fetchText(url) } catch (_: java.io.IOException) { continue }
            val workbook = parsePublicWorkbook(url, html, date) ?: continue
            onProgress("Captured ${workbook.parts.size} meeting parts.")
            return@withContext workbook
        }
        error("No matching workbook found for this week.")
    }

    private fun parsePublicWorkbook(url: String, html: String, date: LocalDate): WorkbookImport? {
        fun clean(value: String): String = android.text.Html.fromHtml(value, android.text.Html.FROM_HTML_MODE_COMPACT)
            .toString().replace(Regex("""[\s\u00a0]+"""), " ").trim()
        fun section(heading: String): String? = when {
            heading.contains("KAYAMANAN MULA", true) -> "treasures"
            heading.contains("MAGING MAHUSAY", true) -> "ministry"
            heading.contains("PAMUMUHAY BILANG", true) -> "living"
            else -> null
        }
        val title = Regex("""<h1\b[^>]*>([\s\S]*?)</h1>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.let(::clean) ?: return null
        val last = date.plusDays(6)
        val months = listOf("enero", "pebrero", "marso", "abril", "mayo", "hunyo", "hulyo",
            "agosto", "setyembre", "oktubre", "nobyembre", "disyembre")
        val firstMonth = months[date.monthValue - 1]
        val lastMonth = months[last.monthValue - 1]
        val range = if (firstMonth == lastMonth) {
            """\b$firstMonth\s+${date.dayOfMonth}\s*[-–—]\s*${last.dayOfMonth}\b"""
        } else {
            """\b$firstMonth\s+${date.dayOfMonth}\s*[-–—]\s*$lastMonth\s+${last.dayOfMonth}\b"""
        }
        if (!Regex(range, RegexOption.IGNORE_CASE).containsMatchIn(title)) return null

        val headers = Regex("""<h([23])\b[^>]*>([\s\S]*?)</h\1>""", RegexOption.IGNORE_CASE)
            .findAll(html).toList()
        val numberPattern = Regex("""^(\d+)\.\s*(.+)$""")
        val paragraphPattern = Regex("""<p\b[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
        val durationPattern = Regex("""^\((\d+)\s*min\.?\)\s*(.*)$""", RegexOption.IGNORE_CASE)
        val reading = headers.firstNotNullOfOrNull { match ->
            val heading = clean(match.groupValues[2])
            heading.takeIf { match.groupValues[1] == "2" && section(it) == null && it.isNotBlank() }
        } ?: return null
        var currentSection: String? = null
        val entries = mutableListOf<Pair<JSONObject, Int>>()
        for ((index, match) in headers.withIndex()) {
            val heading = clean(match.groupValues[2])
            if (match.groupValues[1] == "2") {
                currentSection = section(heading) ?: currentSection
                continue
            }
            val group = currentSection ?: continue
            val number = numberPattern.matchEntire(heading)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val until = headers.getOrNull(index + 1)?.range?.first ?: html.length
            val paragraph = paragraphPattern.find(html.substring(match.range.last + 1, until))
                ?.groupValues?.get(1)?.let(::clean).orEmpty()
            val details = durationPattern.matchEntire(paragraph)
            val minutes = details?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 180) ?: 0
            val ministry = group == "ministry" && !heading.contains("ano ang sasabihin mo", true)
            val bibleReading = heading.contains("pagbabasa ng bibliya", true)
            val bibleStudy = heading.contains("pag-aaral ng kongregasyon sa bibliya", true)
            entries += JSONObject().put("id", number).put("title", heading).put("section", group)
                .put("detail", details?.groupValues?.get(2).orEmpty())
                .put("duration", if (minutes > 0) "$minutes min." else "")
                .put("hasAssistant", ministry || bibleStudy).put("hasSecondary", ministry || bibleReading)
                .put("hasSecondaryAssistant", ministry) to minutes
        }
        if (entries.isEmpty()) return null

        val lastMinistry = entries.indexOfLast { (part, _) -> part.optString("section") == "ministry" }
        var minute = 18 * 60 + 6
        val parts = entries.mapIndexed { index, (part, duration) ->
            part.put("time", "%02d:%02d".format(Locale.ROOT, minute / 60 % 24, minute % 60))
            minute += duration + (if (part.optInt("id") >= 3) 1 else 0) + (if (index == lastMinistry) 5 else 0)
            part
        }
        val songs = Regex("""Awit(?:\s+Blg\.)?\s*(\d+)""", RegexOption.IGNORE_CASE)
            .findAll(clean(html)).map { it.groupValues[1] }.toList()
        val meta = JSONObject().put("reading", reading.uppercase(Locale.ROOT))
            .put("openingSong", songs.firstOrNull().orEmpty()).put("closingSong", songs.lastOrNull().orEmpty())
        return WorkbookImport(url, meta, parts)
    }

    private fun workbookDocumentIds(html: String, date: LocalDate): List<String> {
        val month = date.format(DateTimeFormatter.ofPattern("MMMM", Locale("tl", "PH"))).lowercase(Locale.ROOT)
        val lastDay = date.plusDays(6)
        val endMonth = lastDay.format(DateTimeFormatter.ofPattern("MMMM", Locale("tl", "PH"))).lowercase(Locale.ROOT)
        val range = Regex("\\b${date.dayOfMonth}\\s*[-–—]\\s*${lastDay.dayOfMonth}\\b")
        val anchors = Regex("<a\\b[^>]*href\\s*=\\s*[\\\"']([^\\\"']*/wol/d/r27/lp-tg/(\\d+)[^\\\"']*)[\\\"'][^>]*>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE)
            .findAll(html).mapNotNull { match ->
                val label = match.groupValues[3].replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ").replace("&#160;", " ").replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
                val hasRange = range.containsMatchIn(label)
                val hasStartMonth = label.contains(month)
                val hasEndMonth = endMonth == month || label.contains(endMonth)
                val hasWorkbookLabel = listOf("workbook", "buhay at ministeryo", "life and ministry").any(label::contains)
                val score = (if (hasRange && hasStartMonth && hasEndMonth) 8 else 0) + (if (hasWorkbookLabel) 3 else 0)
                match.groupValues[2] to score
            }.toList()
        val direct = anchors.sortedByDescending { it.second }.map { it.first }
        val fallback = Regex("(?:/wol/d/r27/lp-tg/|wol/d/r27/lp-tg/)(\\d{8,})").findAll(html).map { it.groupValues[1] }.toList()
        return (direct + fallback).distinct().take(6)
    }

    fun signOut(): String? {
        val oldSession = vault.read()
        if (oldSession != null) PushCleanupWorker.schedule(context, removalKey(oldSession))
        vault.clear()
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        return oldSession
    }

    suspend fun revokeSession(oldSession: String?) = withContext(Dispatchers.IO) {
        if (oldSession != null) runCatching {
            request("/api/auth/logout", "POST", null, authorized = false, sessionOverride = oldSession)
        }
        Unit
    }

    private fun request(path: String, method: String, body: JSONObject?, authorized: Boolean = true, captureCookie: Boolean = false, sessionOverride: String? = null): JSONObject {
        val cookie = sessionOverride ?: if (authorized) vault.read() else null
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = method
            connectTimeout = 18_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            if (method != "GET") setRequestProperty("Origin", base)
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8") }
            cookie?.let { setRequestProperty("Cookie", it) }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                if (code == 401 && cookie != null) vault.clearIfCurrent(cookie)
                throw IllegalStateException(json.optString("error").ifBlank { "Server error ($code)." })
            }
            if (captureCookie) {
                connection.headerFields.entries.asSequence()
                    .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
                    .flatMap { it.value.asSequence() }
                    .map { it.substringBefore(';') }
                    .firstOrNull { it.startsWith("meeting_session=") && it.substringAfter('=').isNotBlank() }
                    ?.let(vault::write)
            }
            return json
        } finally { connection.disconnect() }
    }

    private fun fetchText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 18_000; readTimeout = 25_000
            setRequestProperty("User-Agent", "Meeting Scheduler Android")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Hindi maabot ang JW.ORG meeting page.")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }
}

private class SessionVault(context: Context) {
    private val prefs = context.getSharedPreferences("scheduler_api_session", Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences("scheduler_api_session_key_fallback", Context.MODE_PRIVATE)

    // Requests and FCM can use separate SchedulerApi instances in the same process.
    companion object {
        private const val ALIAS = "meeting_scheduler_session_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_BYTES = 12
        private val lock = Any()
        private var inMemoryCookie: String? = null
    }

    fun read(): String? = synchronized(lock) {
        inMemoryCookie?.let { return@synchronized it }
        val encrypted = prefs.getString("cookie", null) ?: return@synchronized null
        try {
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            val encodedKey = prefs.getString("software_key", null)
                ?: if (!prefs.contains("session_format")) legacyPrefs.getString("aes_key", null) else null
            val softwareKey = encodedKey?.let { encoded ->
                Base64.decode(encoded, Base64.NO_WRAP).takeIf { it.size == 32 }
                    ?.let { SecretKeySpec(it, KeyProperties.KEY_ALGORITHM_AES) }
            }
            val key = softwareKey ?: keyStore().getKey(ALIAS, null) as? SecretKey ?: return@synchronized null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, NONCE_BYTES)))
            val cookie = String(cipher.doFinal(bytes.copyOfRange(NONCE_BYTES, bytes.size)), Charsets.UTF_8)
            cookie
        } catch (_: Exception) {
            null
        }
    }

    fun write(cookie: String) {
        synchronized(lock) {
            val encrypted = tryEncrypt(cookie, replaceKey = false)
                ?: tryEncrypt(cookie, replaceKey = true)
            val stored = if (encrypted != null) {
                prefs.edit().putString("cookie", encrypted).putInt("session_format", 2).remove("software_key").commit()
            } else {
                // Store the session and its per-install key in one private, non-backed-up record.
                // On affected devices, Android's app sandbox is the protection for this record.
                try {
                    val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
                    val key = SecretKeySpec(keyBytes, KeyProperties.KEY_ALGORITHM_AES)
                    prefs.edit().putString("cookie", encrypt(cookie, key)).putInt("session_format", 2)
                        .putString("software_key", Base64.encodeToString(keyBytes, Base64.NO_WRAP))
                        .commit()
                } catch (error: Exception) {
                    Log.w("SchedulerSession", "Could not save private session storage (${error.javaClass.simpleName}).")
                    false
                }
            }
            if (stored) {
                legacyPrefs.edit().remove("aes_key").apply()
                inMemoryCookie = null
            } else {
                clearStoredCookie()
                inMemoryCookie = cookie
            }
        }
    }

    fun clearIfCurrent(cookie: String) = synchronized(lock) { if (read() == cookie) clear() }

    fun clear() {
        synchronized(lock) {
            inMemoryCookie = null
            clearStoredCookie()
        }
    }

    private fun clearStoredCookie() {
        prefs.edit().remove("cookie").remove("software_key").remove("session_format").apply()
        legacyPrefs.edit().remove("aes_key").apply()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun tryEncrypt(cookie: String, replaceKey: Boolean): String? = try {
        val store = keyStore()
        if (replaceKey) store.deleteEntry(ALIAS)
        val key = (store.getKey(ALIAS, null) as? SecretKey) ?: generateKey()
        encrypt(cookie, key)
    } catch (error: Exception) {
        Log.w("SchedulerSession", "Secure session storage unavailable (${error.javaClass.simpleName}).")
        null
    }

    private fun encrypt(cookie: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        check(cipher.iv.size == NONCE_BYTES)
        val encrypted = cipher.iv + cipher.doFinal(cookie.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
