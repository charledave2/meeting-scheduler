package com.example.meetingscheduler

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Fingerprint
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BiometricLockPreference(lock: AppLockController) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = colors.primaryContainer, shape = RoundedCornerShape(12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.TwoTone.Fingerprint, contentDescription = null,
                    tint = colors.primary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text("Biometric lock", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = colors.onPrimaryContainer)
                    Text("Locks when you leave the app", fontSize = 11.sp,
                        color = colors.onPrimaryContainer)
                }
                Switch(checked = lock.state.enabled, enabled = lock.state.attempt == null,
                    onCheckedChange = { lock.changeEnabled(it) })
            }
        }
        lock.error?.let {
            Text(it, fontSize = 11.sp, color = colors.error)
            TextButton(onClick = { lock.openSecuritySettings() }) {
                Icon(Icons.TwoTone.Settings, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Security settings", fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun AppLockScreen(lock: AppLockController) {
    val activity = LocalActivity.current as MainActivity
    val colors = MaterialTheme.colorScheme
    BackHandler { activity.moveTaskToBack(true) }
    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(painterResource(R.drawable.ic_meeting_scheduler),
                    contentDescription = "JW Meeting Scheduler", modifier = Modifier.size(72.dp))
                Text("JW Meeting Scheduler", fontSize = 22.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = colors.onSurface)
                Surface(shape = RoundedCornerShape(18.dp), color = colors.primaryContainer) {
                    Icon(Icons.TwoTone.Lock, contentDescription = null,
                        tint = colors.primary, modifier = Modifier.padding(16.dp).size(28.dp))
                }
                Text("Naka-lock ang iyong workspace", textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text("Gamitin ang fingerprint, face, o PIN / pattern / password ng phone para magpatuloy.",
                    fontSize = 13.sp, textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                lock.error?.let {
                    Text(it, fontSize = 12.sp, color = colors.error, textAlign = TextAlign.Center)
                }
                Button(onClick = { lock.unlock() }, enabled = lock.state.attempt == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    if (lock.state.attempt != null) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = colors.onPrimary)
                    } else Icon(Icons.TwoTone.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (lock.state.attempt != null) "Authenticating…" else "Unlock")
                }
                if (lock.error != null) TextButton(onClick = { lock.openSecuritySettings() }) {
                    Icon(Icons.TwoTone.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Android security settings")
                }
            }
        }
    }
}
