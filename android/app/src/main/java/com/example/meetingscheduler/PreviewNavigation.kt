package com.example.meetingscheduler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.ArrowForward
import androidx.compose.material.icons.twotone.Assignment
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.Groups
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.ManageAccounts
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Floating navigation preview; the center slot stays centered as role permissions change. */
@Composable
internal fun PreviewBottomNavigation(
    current: Screen,
    available: Set<Screen>,
    onNavigate: (Screen) -> Unit,
    onCenterClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tools = available.any { it in setOf(Screen.ASSIGNMENTS, Screen.PREVIEW, Screen.PARTICIPANTS) }
    val centerSelected = tools && current in setOf(Screen.ASSIGNMENTS, Screen.PREVIEW, Screen.PARTICIPANTS)
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var animating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.widthIn(max = 640.dp).fillMaxWidth().height(100.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(76.dp).align(Alignment.BottomCenter),
                shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
                shadowElevation = 3.dp
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f)) {
                        listOf(Screen.HOME, Screen.DETAILS).filter { it in available }.forEach { screen ->
                            PreviewNavItem(screen, current == screen, Modifier.weight(1f)) { onNavigate(screen) }
                        }
                    }
                    Spacer(Modifier.width(maxWidth / 5))
                    Row(Modifier.weight(1f)) {
                        listOf(Screen.HISTORY, Screen.ACCOUNTS).filter { it in available }.forEach { screen ->
                            PreviewNavItem(screen, current == screen, Modifier.weight(1f)) { onNavigate(screen) }
                        }
                    }
                }
            }
            Column(Modifier.align(Alignment.TopCenter).width(76.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(64.dp).graphicsLayer {
                        scaleX = scale.value; scaleY = scale.value
                        translationY = lift.value * density
                    },
                    shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary,
                    shadowElevation = 8.dp, border = BorderStroke(4.dp, colors.primaryContainer)
                ) {
                    Box(
                        Modifier.fillMaxSize().clip(CircleShape)
                            .clickable(enabled = !animating, role = Role.Button,
                                onClickLabel = if (tools) "Open schedule tools" else "Open profile") {
                                animating = true
                                scope.launch {
                                    try {
                                        launch {
                                            rotation.animateTo(135f, tween(170, easing = FastOutSlowInEasing))
                                            rotation.animateTo(0f, tween(190, easing = FastOutSlowInEasing))
                                        }
                                        launch {
                                            scale.animateTo(0.84f, tween(80))
                                            scale.animateTo(1.12f, tween(140))
                                            scale.animateTo(1f, tween(140))
                                        }
                                        launch {
                                            lift.animateTo(-7f, tween(160))
                                            lift.animateTo(0f, tween(200))
                                        }
                                        // Keep the tap animation visible before the sheet covers the bar.
                                        delay(370)
                                        onCenterClick()
                                    } finally { animating = false }
                                }
                            }.semantics { contentDescription = if (tools) "Schedule tools" else "Profile" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (tools) Icons.TwoTone.Add else Icons.TwoTone.Person,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = rotation.value })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(if (tools) "Tools" else "Profile", fontSize = 10.sp, lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold, color = if (centerSelected) colors.primary else colors.onSurfaceVariant)
                if (centerSelected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(colors.primary, CircleShape))
            }
        }
    }
}

@Composable
private fun PreviewNavItem(screen: Screen, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = when (screen) {
        Screen.HOME -> "Home"
        Screen.DETAILS -> "Workbook"
        Screen.HISTORY -> "History"
        else -> "Settings"
    }
    val icon = when (screen) {
        Screen.HOME -> Icons.TwoTone.Home
        Screen.DETAILS -> Icons.TwoTone.CalendarMonth
        Screen.HISTORY -> Icons.TwoTone.History
        else -> Icons.TwoTone.ManageAccounts
    }
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics { contentDescription = screen.title }.padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(32.dp).background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
            CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp),
                tint = if (selected) colors.primary else colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurfaceVariant)
        if (selected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(colors.primary, CircleShape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreviewToolsSheet(available: Set<Screen>, current: Screen, onDismiss: () -> Unit, onSelect: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Schedule tools", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            listOf(
                Triple(Screen.ASSIGNMENTS, Icons.TwoTone.Assignment, "Choose who will handle each part"),
                Triple(Screen.PREVIEW, Icons.TwoTone.Preview, "Review and share the meeting schedule"),
                Triple(Screen.PARTICIPANTS, Icons.TwoTone.Groups, "View your congregation participants")
            ).filter { it.first in available }.forEach { (screen, icon, description) ->
                Surface(onClick = { onSelect(screen) }, shape = RoundedCornerShape(18.dp),
                    color = if (current == screen) colors.primaryContainer else colors.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(25.dp))
                        Column(Modifier.weight(1f)) {
                            Text(screen.title, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Text(description, fontSize = 11.sp, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.TwoTone.ArrowForward, contentDescription = null,
                            tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
