package com.example.meetingscheduler

internal enum class LockAction { UNLOCK, ENABLE, DISABLE }

internal data class LockAttempt(val id: Long, val action: LockAction)
internal data class AppLockSnapshot(
    val enabled: Boolean,
    val locked: Boolean,
    val attempt: LockAttempt? = null
)

/** Only the enabled preference is persisted; an unlocked session never survives a restart. */
internal class AppLockSession(initiallyEnabled: Boolean) {
    var snapshot = AppLockSnapshot(initiallyEnabled, initiallyEnabled)
        private set
    private var nextAttempt = 0L

    fun begin(action: LockAction): LockAttempt? {
        if (snapshot.attempt != null) return null
        if (action == LockAction.ENABLE && snapshot.enabled) return null
        if (action != LockAction.ENABLE && !snapshot.enabled) return null
        return LockAttempt(++nextAttempt, action).also { snapshot = snapshot.copy(attempt = it) }
    }

    fun background() {
        if (snapshot.enabled) snapshot = snapshot.copy(locked = true)
    }

    fun failed(attempt: LockAttempt) {
        if (snapshot.attempt == attempt) snapshot = snapshot.copy(attempt = null)
    }

    fun authenticated(attempt: LockAttempt, foreground: Boolean): Boolean {
        if (snapshot.attempt != attempt) return false
        val enabled = when (attempt.action) {
            LockAction.ENABLE -> true
            LockAction.DISABLE -> false
            LockAction.UNLOCK -> snapshot.enabled
        }
        snapshot = AppLockSnapshot(enabled = enabled, locked = enabled && !foreground)
        return true
    }
}
