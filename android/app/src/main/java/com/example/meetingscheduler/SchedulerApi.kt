package com.example.meetingscheduler

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal data class ApiIdentity(val name: String, val username: String, val role: String, val mustChangePassword: Boolean)
internal data class WorkbookImport(val link: String, val meta: JSONObject, val parts: List<JSONObject>)
internal data class RemoteSchedule(val id: Int, val weekLabel: String, val reading: String, val updatedAt: String)
internal data class RemoteAccount(val id: Int, val displayName: String, val username: String, val status: String, val role: String, val createdAt: String, val requestNote: String, val photoDataUrl: String = "")
internal data class ParticipantRotation(
    val memberId: Int, val mainThisMonth: Int, val assistantThisMonth: Int, val totalThisMonth: Int,
    val lastAssignmentDate: String?, val nextEligibleDate: String?, val eligible: Boolean, val exempt: Boolean,
    val neverAssigned: Boolean, val recommended: Boolean
)

internal class SchedulerApi(context: Context) {
    private val vault = SessionVault(context)
    private val base = "https://meeting-scheduler.yelyahwilliams2005.workers.dev"

    suspend fun login(username: String, password: String): ApiIdentity = withContext(Dispatchers.IO) {
        val response = try {
            request("/api/auth/login", "POST", JSONObject().put("username", username).put("password", password), authorized = false, captureCookie = true)
        } catch (error: Exception) {
            val detail = error.message?.takeIf { it.isNotBlank() } ?: "walang karagdagang detalye"
            throw IllegalStateException("Hindi natapos ang login request (${error.javaClass.simpleName}): $detail", error)
        }
        if (!response.optString("redirectTo").startsWith("/")) throw IllegalStateException("Tinanggap ng server ang request pero walang login confirmation.")
        try {
            meBlocking()
        } catch (error: Exception) {
            val detail = error.message?.takeIf { it.isNotBlank() } ?: "walang karagdagang detalye"
            throw IllegalStateException("Tinanggap ang login pero pumalya ang session check (${error.javaClass.simpleName}): $detail", error)
        }
    }

    suspend fun currentUser(): ApiIdentity = withContext(Dispatchers.IO) { meBlocking() }
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
                             sourceUrl: String, id: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val meta = JSONObject().put("date", weekLabel).put("weekStart", weekStart).put("reading", reading)
            .put("openingSong", openingSong).put("closingSong", closingSong).put("chairman", chairman)
            .put("openingPrayer", openingPrayer).put("adviser", adviser).put("audienceGroup", audienceGroup)
        val body = JSONObject().put("meta", meta).put("parts", JSONArray(parts)).put("sourceUrl", sourceUrl)
        if (id > 0) body.put("id", id)
        val result = request("/api/schedules", "POST", body)
        result.optBoolean("historySaved", true)
    }

    suspend fun schedules(): List<RemoteSchedule> = withContext(Dispatchers.IO) {
        val rows = request("/api/schedules", "GET", null).optJSONArray("schedules") ?: JSONArray()
        (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { RemoteSchedule(it.optInt("id"), it.optString("weekLabel"), it.optString("bibleReading"), it.optString("updatedAt")) } }
    }

    suspend fun schedule(id: Int): JSONObject = withContext(Dispatchers.IO) { request("/api/schedules?id=$id", "GET", null).optJSONObject("schedule") ?: throw IllegalStateException("Hindi mabasa ang schedule.") }
    suspend fun deleteSchedule(id: Int): JSONObject = withContext(Dispatchers.IO) { request("/api/schedules?id=$id", "DELETE", null) }

    suspend fun members(includeInactive: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        request("/api/members${if (includeInactive) "?includeInactive=true" else ""}", "GET", null)
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

    suspend fun accounts(): List<RemoteAccount> = withContext(Dispatchers.IO) {
        val rows = request("/api/accounts", "GET", null).optJSONArray("accounts") ?: JSONArray()
        (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { row ->
            RemoteAccount(row.optInt("id"), row.optString("displayName"), row.optString("username"), row.optString("status"), row.optString("role", "viewer"), row.optString("createdAt"), row.optString("requestNote"), row.optString("photoDataUrl"))
        } }
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
        return ApiIdentity(user.optString("displayName"), user.optString("username"), user.optString("role"), user.optBoolean("mustChangePassword"))
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

    fun signOut() { vault.clear() }

    private fun request(path: String, method: String, body: JSONObject?, authorized: Boolean = true, captureCookie: Boolean = false): JSONObject {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 18_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            if (method != "GET") setRequestProperty("Origin", base)
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8") }
            if (authorized) vault.read()?.let { setRequestProperty("Cookie", it) }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (captureCookie) connection.getHeaderField("Set-Cookie")?.substringBefore(';')?.takeIf { it.startsWith("meeting_session=") }?.let(vault::write)
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                if (code == 401) vault.clear()
                throw IllegalStateException(json.optString("error").ifBlank { "Server error ($code)." })
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
    private val fallbackPrefs = context.getSharedPreferences("scheduler_api_session_key_fallback", Context.MODE_PRIVATE)
    private val alias = "meeting_scheduler_session_v1"
    fun read(): String? = runCatching {
        val encrypted = prefs.getString("cookie", null) ?: return null
        val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), javax.crypto.spec.GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }.getOrNull()
    fun write(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("cookie", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }
    fun clear() { prefs.edit().remove("cookie").apply() }
    private fun key(): SecretKey {
        try {
            val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey(alias, null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            return generator.generateKey()
        } catch (_: Exception) {
            // Some Android devices cannot provision AndroidKeyStore AES keys. Keep the session encrypted
            // with a per-install key held in the app-private sandbox so sign-in can still complete.
            val encoded = fallbackPrefs.getString("aes_key", null)
            if (encoded != null) {
                val bytes = Base64.decode(encoded, Base64.NO_WRAP)
                if (bytes.size == 32) return javax.crypto.spec.SecretKeySpec(bytes, KeyProperties.KEY_ALGORITHM_AES)
            }
            val bytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(bytes)
            fallbackPrefs.edit().putString("aes_key", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()
            return javax.crypto.spec.SecretKeySpec(bytes, KeyProperties.KEY_ALGORITHM_AES)
        }
    }
}
