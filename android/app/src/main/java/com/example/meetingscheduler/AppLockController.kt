package com.example.meetingscheduler

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

/** Android owns the fingerprint/face templates and screen-lock credential. */
internal class AppLockController(private val activity: FragmentActivity) {
    private val preferences = activity.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    private val session = AppLockSession(preferences.getBoolean("enabled", false))
    var state by mutableStateOf(session.snapshot)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var prompt: BiometricPrompt? = null

    // WEAK includes supported face sensors and is compatible with credential fallback on API 24–29.
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun onBackground() {
        session.background()
        publish()
        if (state.enabled) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun onForeground() {
        updatePrivacy()
        activity.window.decorView.post {
            if (state.locked && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                authenticate(LockAction.UNLOCK)
            }
        }
    }

    fun changeEnabled(enabled: Boolean) = authenticate(if (enabled) LockAction.ENABLE else LockAction.DISABLE)
    fun unlock() = authenticate(LockAction.UNLOCK)

    fun openSecuritySettings() {
        runCatching { activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            .onFailure { error = "Hindi mabuksan ang Android security settings." }
    }

    fun updatePrivacy() {
        if (Build.VERSION.SDK_INT >= 33) activity.setRecentsScreenshotEnabled(!state.enabled)
        if (state.enabled && (state.locked || Build.VERSION.SDK_INT < 33)) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun publish() { state = session.snapshot }

    private fun authenticate(action: LockAction) {
        if (activity.isFinishing || activity.isDestroyed ||
            activity.lifecycle.currentState != Lifecycle.State.RESUMED) return
        if (state.attempt != null) return
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            error = "Magtakda muna ng PIN, pattern, o password sa Android security settings."
            return
        }
        val availability = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            error = "Hindi magamit ang authentication ngayon. Subukan ulit o tingnan ang Android security settings."
            return
        }
        val attempt = session.begin(action) ?: return
        error = null
        publish()
        val newPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val foreground = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    if (session.authenticated(attempt, foreground)) {
                        preferences.edit().putBoolean("enabled", session.snapshot.enabled).apply()
                        error = null
                        publish()
                        updatePrivacy()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (state.attempt != attempt) return
                    session.failed(attempt)
                    publish()
                    error = when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> if (state.locked) "Naka-lock pa rin ang app. Pindutin ang Unlock para subukan ulit." else null
                        else -> errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    if (state.attempt != attempt) return
                    // A mismatch is never treated as success; Android keeps the prompt available.
                    error = "Hindi nakilala. Subukan muli ang fingerprint, face, o screen lock."
                }
            })
        prompt = newPrompt
        runCatching {
            newPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("JW Meeting Scheduler")
                .setSubtitle(when (action) {
                    LockAction.ENABLE -> "Confirm to enable biometric lock"
                    LockAction.DISABLE -> "Confirm to turn off biometric lock"
                    LockAction.UNLOCK -> "Unlock your meeting workspace"
                })
                .setAllowedAuthenticators(authenticators)
                .setConfirmationRequired(true)
                .build())
        }.onFailure {
            session.failed(attempt)
            publish()
            error = "Hindi mabuksan ang authentication. Subukan ulit."
        }
    }
}
