package com.example.meetingscheduler

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Logout
import androidx.compose.material.icons.twotone.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/** Floating navigation preview; the center slot stays centered as role permissions change. */
@Composable
internal fun PreviewBottomNavigation(
    current: Screen,
    available: Set<Screen>,
    onNavigate: (Screen) -> Unit,
    onCenterClick: () -> Unit,
    profileName: String,
    profileRole: String,
    profileAvatar: @Composable (Dp) -> Unit,
    profileMenuOpen: Boolean,
    onProfileMenuToggle: () -> Unit,
    onProfileMenuDismiss: () -> Unit,
    onProfileSettings: () -> Unit,
    onProfileSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val labelGrowth = ((LocalDensity.current.fontScale - 1f).coerceAtLeast(0f) * 32f).dp
    val bodyHeight = 76.dp + labelGrowth
    val tools = available.any { it in setOf(Screen.ASSIGNMENTS, Screen.PREVIEW, Screen.PARTICIPANTS) }
    val centerSelected = tools && current in setOf(Screen.ASSIGNMENTS, Screen.PREVIEW, Screen.PARTICIPANTS)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.widthIn(max = 640.dp).fillMaxWidth().height(bodyHeight + 24.dp)) {
            if (tools) {
                val centerSlotWidth = maxWidth / 5
                Surface(
                    modifier = Modifier.fillMaxWidth().height(bodyHeight).align(Alignment.BottomCenter),
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
                        Spacer(Modifier.width(centerSlotWidth))
                        Row(Modifier.weight(1f)) {
                            if (Screen.HISTORY in available) {
                                PreviewNavItem(Screen.HISTORY, current == Screen.HISTORY, Modifier.weight(1f)) {
                                    onNavigate(Screen.HISTORY)
                                }
                            }
                            ProfileNavItem(
                                selected = current == Screen.ACCOUNTS,
                                name = profileName,
                                role = profileRole,
                                avatar = profileAvatar,
                                expanded = profileMenuOpen,
                                modifier = Modifier.weight(1f),
                                onClick = onProfileMenuToggle,
                                onDismiss = onProfileMenuDismiss,
                                onSettings = onProfileSettings,
                                onSignOut = onProfileSignOut
                            )
                        }
                    }
                }
                Column(Modifier.align(Alignment.TopCenter).width(76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        onClick = onCenterClick,
                        modifier = Modifier.size(64.dp).semantics { contentDescription = "Open schedule tools" },
                        shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary,
                        shadowElevation = 6.dp, border = BorderStroke(4.dp, colors.primaryContainer)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.TwoTone.Add, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Tools", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = if (centerSelected) colors.primary else colors.onSurfaceVariant)
                    if (centerSelected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(colors.primary, CircleShape))
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(bodyHeight).align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
                    shadowElevation = 3.dp
                ) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        listOf(Screen.HOME, Screen.DETAILS, Screen.HISTORY).filter { it in available }.forEach { screen ->
                            PreviewNavItem(screen, current == screen, Modifier.weight(1f)) { onNavigate(screen) }
                        }
                        ProfileNavItem(
                            selected = current == Screen.ACCOUNTS,
                            name = profileName,
                            role = profileRole,
                            avatar = profileAvatar,
                            expanded = profileMenuOpen,
                            modifier = Modifier.weight(1f),
                            onClick = onProfileMenuToggle,
                            onDismiss = onProfileMenuDismiss,
                            onSettings = onProfileSettings,
                            onSignOut = onProfileSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNavItem(
    selected: Boolean,
    name: String,
    role: String,
    avatar: @Composable (Dp) -> Unit,
    expanded: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val roleLabel = when (role.lowercase()) {
        "admin", "administrator" -> "ADMINISTRATOR"
        else -> role.uppercase().ifBlank { "ACCOUNT" }
    }
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)
                .selectable(selected = selected || expanded, role = Role.Tab, onClick = onClick)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(32.dp).background(
                    if (selected || expanded) colors.primaryContainer else colors.surfaceContainerHigh,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) { avatar(26.dp) }
            Spacer(Modifier.height(4.dp))
            Text(
                "Profile", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                fontWeight = if (selected || expanded) FontWeight.Bold else FontWeight.Medium,
                color = if (selected || expanded) colors.primary else colors.onSurfaceVariant
            )
            if (selected || expanded) Box(Modifier.padding(top = 4.dp).size(4.dp).background(colors.primary, CircleShape))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(276.dp).heightIn(max = 320.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                avatar(44.dp)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(roleLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = colors.primary, maxLines = 1)
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.TwoTone.Settings, contentDescription = null, tint = colors.primary) },
                onClick = onSettings
            )
            DropdownMenuItem(
                text = { Text("Sign out", color = colors.error) },
                leadingIcon = { Icon(Icons.TwoTone.Logout, contentDescription = null, tint = colors.error) },
                onClick = onSignOut
            )
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
        Screen.ACCOUNTS -> "Profile"
        else -> screen.title
    }
    val icon = when (screen) {
        Screen.HOME -> Icons.TwoTone.Home
        Screen.DETAILS -> Icons.TwoTone.CalendarMonth
        Screen.HISTORY -> Icons.TwoTone.History
        Screen.ACCOUNTS -> Icons.TwoTone.Person
        else -> Icons.TwoTone.Person
    }
    Column(
        modifier.fillMaxHeight().clip(MaterialTheme.shapes.large)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(32.dp).background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
            CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) colors.primary else colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.TwoTone.ArrowForward, contentDescription = null,
                            tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
