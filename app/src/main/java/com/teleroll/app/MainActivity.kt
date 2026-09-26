package com.teleroll.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleroll.app.automation.AutomationBus
import com.teleroll.app.automation.AutomationSnapshot
import com.teleroll.app.automation.AutomationState
import com.teleroll.app.automation.DownloadState
import com.teleroll.app.automation.SettingsSnapshot
import com.teleroll.app.data.SettingsRepository
import com.teleroll.app.service.TelegramAccessibilityService

private val Ink = ComposeColor(0xFF0A0B14)
private val Panel = ComposeColor(0xFF141522)
private val PanelBright = ComposeColor(0xFF1B1C2D)
private val Violet = ComposeColor(0xFF9D8BFF)
private val Cyan = ComposeColor(0xFF62D7E4)
private val Pink = ComposeColor(0xFFFF87A2)
private val Muted = ComposeColor(0xFF9696AC)
private val Figtree = FontFamily(
    Font(R.font.figtree, FontWeight.Normal),
    Font(R.font.figtree, FontWeight.Medium),
    Font(R.font.figtree, FontWeight.SemiBold),
    Font(R.font.figtree, FontWeight.Bold),
)
private val Outfit = FontFamily(Font(R.font.outfit, FontWeight.Normal))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeleRollApp()
        }
    }
}

@Composable
private fun TeleRollApp() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsStateWithLifecycle()
    val snapshot by AutomationBus.snapshot.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var showAccessibilityInfo by remember { mutableStateOf(false) }

    TeleRollTheme(darkTheme = settings.darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Box(modifier = Modifier.fillMaxSize()) {
            AmbientBackground()
            Scaffold(
                containerColor = ComposeColor.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BrandMark()
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "teleRoll",
                                        fontFamily = Outfit,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = ComposeColor.White,
                                    )
                                    Text(
                                        "TELEGRAM MEDIA ASSISTANT",
                                        fontFamily = Figtree,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.5.sp,
                                        color = Violet,
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { screen = if (screen == "settings") "home" else "settings" }) {
                                Icon(
                                    if (screen == "settings") Icons.Outlined.PlayArrow else Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = ComposeColor.White,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor.Transparent),
                    )
                },
                bottomBar = {
                    BottomNav(
                        current = screen,
                        onChange = { screen = it },
                    )
                },
            ) { padding ->
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    label = "screen",
                ) { current ->
                    when (current) {
                        "settings" -> SettingsScreen(settings, repository::update)
                        "debug" -> DebugScreen(snapshot)
                        else -> HomeScreen(
                            snapshot = snapshot,
                            accessibilityEnabled = isAccessibilityEnabled(context),
                            overlayEnabled = Settings.canDrawOverlays(context),
                            onStart = {
                                when {
                                    !isAccessibilityEnabled(context) ->
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    !Settings.canDrawOverlays(context) ->
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}"),
                                            ),
                                        )
                                    else -> sendServiceAction(context, ACTION_START)
                                }
                            },
                            onPause = { sendServiceAction(context, ACTION_PAUSE) },
                            onStop = { sendServiceAction(context, ACTION_STOP) },
                            onAccessibility = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onOverlay = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            },
                            onExplainAccessibility = { showAccessibilityInfo = true },
                        )
                    }
                }
            }

            if (showAccessibilityInfo) {
                AccessibilityInfoDialog(onDismiss = { showAccessibilityInfo = false })
            }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    snapshot: AutomationSnapshot,
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onAccessibility: () -> Unit,
    onOverlay: () -> Unit,
    onExplainAccessibility: () -> Unit,
) {
    val stateColor = when (snapshot.state) {
        AutomationState.WAITING_FOR_DOWNLOAD, AutomationState.MEDIA_DETECTED -> Cyan
        AutomationState.FINISHED, AutomationState.DOWNLOAD_COMPLETE -> Cyan
        AutomationState.PAUSED -> Pink
        AutomationState.STOPPED -> Pink
        else -> Violet
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(10.dp))
            HeaderBlock(snapshot, stateColor)
        }
        item {
            PermissionCard(
                accessibilityEnabled = accessibilityEnabled,
                overlayEnabled = overlayEnabled,
                onAccessibility = onAccessibility,
                onOverlay = onOverlay,
                onExplainAccessibility = onExplainAccessibility,
            )
        }
        item { StatsGrid(snapshot) }
        item {
            ControlCard(
                snapshot = snapshot,
                onStart = onStart,
                onPause = onPause,
                onStop = onStop,
            )
        }
        item {
            SafetyNote()
        }
    }
}

@Composable
private fun HeaderBlock(snapshot: AutomationSnapshot, stateColor: ComposeColor) {
    Column {
        Text(
            "TELEGRAM MEDIA AUTO-SCROLLER",
            color = Muted,
            fontFamily = Figtree,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.7.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scroll media.\nWait intelligently.",
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 37.sp,
            lineHeight = 41.sp,
            color = ComposeColor.White,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(stateColor, animated = snapshot.state == AutomationState.SCROLLING)
            Spacer(Modifier.width(8.dp))
            Text(
                snapshot.statusText,
                color = stateColor,
                fontFamily = Figtree,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "teleRoll observes only Telegram’s visible UI. It never skips past media while download state is uncertain.",
            color = Muted,
            fontFamily = Figtree,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun PermissionCard(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    onAccessibility: () -> Unit,
    onOverlay: () -> Unit,
    onExplainAccessibility: () -> Unit,
) {
    GlassCard {
        Text(
            "READY CHECK",
            color = Violet,
            fontFamily = Figtree,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Outlined.AccessibilityNew,
            title = "Accessibility service",
            subtitle = if (accessibilityEnabled) "Connected to Telegram UI" else "Required to observe and scroll",
            enabled = accessibilityEnabled,
            actionLabel = if (accessibilityEnabled) "Connected" else "Enable",
            onClick = if (accessibilityEnabled) onExplainAccessibility else onAccessibility,
        )
        Spacer(Modifier.height(9.dp))
        PermissionRow(
            icon = Icons.Outlined.Tune,
            title = "Floating overlay",
            subtitle = if (overlayEnabled) "Controls can float above Telegram" else "Required for in-Telegram controls",
            enabled = overlayEnabled,
            actionLabel = if (overlayEnabled) "Connected" else "Allow",
            onClick = onOverlay,
        )
        Spacer(Modifier.height(11.dp))
        Text(
            "teleRoll will pause safely when Telegram is not the foreground app.",
            color = Muted,
            fontFamily = Figtree,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) Cyan.copy(alpha = 0.15f) else ComposeColor.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) Cyan else Muted)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ComposeColor.White, fontFamily = Figtree, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontFamily = Figtree, fontSize = 12.sp)
        }
        Text(
            actionLabel,
            color = if (enabled) Cyan else Violet,
            fontFamily = Figtree,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
private fun StatsGrid(snapshot: AutomationSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            modifier = Modifier.weight(1f),
            value = snapshot.processedMediaCount.toString().padStart(2, '0'),
            label = "MEDIA PROCESSED",
            icon = Icons.Outlined.CloudDownload,
            color = Cyan,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value = snapshot.scrollCount.toString().padStart(2, '0'),
            label = "SCROLLS",
            icon = Icons.Outlined.KeyboardArrowRight,
            color = Violet,
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    color: ComposeColor,
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = ComposeColor.White)
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Muted, fontFamily = Figtree, fontSize = 10.sp, letterSpacing = 1.1.sp)
    }
}

@Composable
private fun ControlCard(
    snapshot: AutomationSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("AUTOMATION", color = Violet, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (snapshot.telegramForeground) "Telegram detected" else "Waiting for Telegram",
                    color = ComposeColor.White,
                    fontFamily = Figtree,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TelegramPill(active = snapshot.telegramForeground)
        }
        if (snapshot.detectedPercent != null && snapshot.state == AutomationState.WAITING_FOR_DOWNLOAD) {
            Spacer(Modifier.height(17.dp))
            LinearProgressIndicator(
                progress = { snapshot.detectedPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = Cyan,
                trackColor = ComposeColor.White.copy(alpha = 0.08f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(6.dp))
            Text("${snapshot.detectedPercent}% observed by Telegram", color = Cyan, fontFamily = Figtree, fontSize = 12.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryAction(
                modifier = Modifier.weight(1f),
                label = "START",
                icon = Icons.Outlined.PlayArrow,
                color = Violet,
                onClick = onStart,
            )
            SecondaryAction(
                modifier = Modifier.weight(1f),
                label = "PAUSE",
                icon = Icons.Outlined.Pause,
                onClick = onPause,
            )
            SecondaryAction(
                modifier = Modifier.weight(1f),
                label = "STOP",
                icon = Icons.Outlined.StopCircle,
                onClick = onStop,
                tint = Pink,
            )
        }
    }
}

@Composable
private fun SafetyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeColor.White.copy(alpha = 0.045f))
            .border(1.dp, ComposeColor.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Cyan, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Safe by design. teleRoll only performs upward scroll gestures and observes visible accessibility nodes inside Telegram.",
            color = Muted,
            fontFamily = Figtree,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SettingsScreen(settings: SettingsSnapshot, update: ((SettingsSnapshot) -> SettingsSnapshot) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Control room", color = ComposeColor.White, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 31.sp)
        Text("Tune how teleRoll moves through Telegram.", color = Muted, fontFamily = Figtree, fontSize = 14.sp)
        SettingsSection("SCROLLING") {
            SliderSetting("Scroll distance", "${settings.scrollDistancePercent}%", settings.scrollDistancePercent.toFloat(), 35f..88f) {
                value -> update { current -> current.copy(scrollDistancePercent = value.toInt()) }
            }
            SliderSetting("Scroll speed", "${settings.scrollDurationMs} ms", settings.scrollDurationMs.toFloat(), 180f..1600f) {
                value -> update { current -> current.copy(scrollDurationMs = value.toLong()) }
            }
            SliderSetting("Completion delay", "${settings.completionDelayMs} ms", settings.completionDelayMs.toFloat(), 0f..5000f) {
                value -> update { current -> current.copy(completionDelayMs = value.toLong()) }
            }
        }
        SettingsSection("SESSION LIMITS") {
            SliderSetting("Maximum scrolls", settings.maxScrollCount.toString(), settings.maxScrollCount.toFloat(), 25f..500f) {
                value -> update { current -> current.copy(maxScrollCount = value.toInt()) }
            }
            SliderSetting("Maximum duration", "${settings.maxSessionDurationMinutes} min", settings.maxSessionDurationMinutes.toFloat(), 5f..120f) {
                value -> update { current -> current.copy(maxSessionDurationMinutes = value.toInt()) }
            }
            ToggleSetting("Stop at end", "Finish after repeated screens show no new content", settings.stopAtEnd) {
                enabled -> update { current -> current.copy(stopAtEnd = enabled) }
            }
        }
        SettingsSection("BEHAVIOR") {
            ToggleSetting("Pause when Telegram loses focus", "Never scroll in another app", settings.pauseWhenTelegramLosesFocus) {
                enabled -> update { current -> current.copy(pauseWhenTelegramLosesFocus = enabled) }
            }
            ToggleSetting("Keep screen awake", "Useful for long media queues", settings.keepScreenAwake) {
                enabled -> update { current -> current.copy(keepScreenAwake = enabled) }
            }
            ToggleSetting("Show floating overlay", "Keep compact controls above Telegram", settings.showOverlay) {
                enabled -> update { current -> current.copy(showOverlay = enabled) }
            }
            ToggleSetting("Debug logging", "Write accessibility decisions to logcat", settings.debugLogging) {
                enabled -> update { current -> current.copy(debugLogging = enabled) }
            }
        }
        SettingsSection("APPEARANCE") {
            ToggleSetting("Dark theme", "Use the deep-space teleRoll interface", settings.darkTheme) {
                enabled -> update { current -> current.copy(darkTheme = enabled) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Violet, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
        GlassCard(content = content)
    }
}

@Composable
private fun SliderSetting(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = ComposeColor.White, fontFamily = Figtree, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, color = Cyan, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan, inactiveTrackColor = ComposeColor.White.copy(alpha = 0.12f)))
    }
}

@Composable
private fun ToggleSetting(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ComposeColor.White, fontFamily = Figtree, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontFamily = Figtree, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun DebugScreen(snapshot: AutomationSnapshot) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Developer view", color = ComposeColor.White, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 31.sp)
            Text("Live accessibility signals. Logging stays local to this device.", color = Muted, fontFamily = Figtree, fontSize = 14.sp)
        }
        item {
            GlassCard {
                DebugValue("Current package", snapshot.currentPackage ?: "—")
                DebugValue("Automation state", snapshot.state.name)
                DebugValue("Detected media", snapshot.mediaVisibleCount.toString())
                DebugValue("Detected percentage", snapshot.detectedPercent?.let { "$it%" } ?: "—")
                DebugValue("Download state", snapshot.downloadState.name)
                DebugValue("Processed media", snapshot.processedMediaCount.toString())
                DebugValue("Content fingerprint", snapshot.lastContentFingerprint ?: "—")
            }
        }
        item {
            Text("RELEVANT ACCESSIBILITY NODES", color = Violet, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp)
        }
        if (snapshot.relevantNodes.isEmpty()) {
            item { Text("No media or download nodes observed yet.", color = Muted, fontFamily = Figtree, fontSize = 13.sp) }
        } else {
            items(snapshot.relevantNodes) { line ->
                Text(line, color = ComposeColor.White.copy(alpha = 0.85f), fontFamily = Figtree, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun DebugValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontFamily = Figtree, fontSize = 13.sp)
        Text(value, color = ComposeColor.White, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun BottomNav(current: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("home", "Monitor", Icons.Outlined.Send, current, onChange)
        NavItem("settings", "Settings", Icons.Outlined.Settings, current, onChange)
        NavItem("debug", "Debug", Icons.Outlined.BugReport, current, onChange)
    }
}

@Composable
private fun NavItem(id: String, label: String, icon: ImageVector, current: String, onChange: (String) -> Unit) {
    val active = current == id
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onChange(id) }
            .padding(horizontal = 17.dp, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Violet else Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (active) ComposeColor.White else Muted, fontFamily = Figtree, fontSize = 10.sp)
    }
}

@Composable
private fun TelegramPill(active: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) Cyan.copy(alpha = 0.14f) else ComposeColor.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(if (active) Cyan else Muted, animated = active)
        Spacer(Modifier.width(6.dp))
        Text(if (active) "LIVE" else "OFFLINE", color = if (active) Cyan else Muted, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun PrimaryAction(modifier: Modifier, label: String, icon: ImageVector, color: ComposeColor, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Ink),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun SecondaryAction(modifier: Modifier, label: String, icon: ImageVector, onClick: () -> Unit, tint: ComposeColor = ComposeColor.White) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.14f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
        contentPadding = PaddingValues(horizontal = 6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun StatusDot(color: ComposeColor, animated: Boolean) {
    val transition = rememberInfiniteTransition(label = "status-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot-alpha",
    )
    Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = if (animated) alpha else 1f)))
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = ComposeColor.Black.copy(alpha = 0.18f), spotColor = ComposeColor.Black.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.84f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(17.dp), content = content)
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Violet, Cyan))),
        contentAlignment = Alignment.Center,
    ) {
        Text("t", color = Ink, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun AmbientBackground() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient-shift",
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.verticalGradient(listOf(Ink, ComposeColor(0xFF0D0F1B), Ink)))
        drawCircle(
            brush = Brush.radialGradient(listOf(Violet.copy(alpha = 0.15f), ComposeColor.Transparent)),
            radius = size.width * 0.78f,
            center = Offset(size.width * (0.08f + shift * 0.1f), size.height * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Cyan.copy(alpha = 0.10f), ComposeColor.Transparent)),
            radius = size.width * 0.65f,
            center = Offset(size.width * (0.95f - shift * 0.12f), size.height * 0.57f),
        )
    }
}

@Composable
private fun AccessibilityInfoDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelBright,
        title = { Text("Why Accessibility access?", color = ComposeColor.White, fontFamily = Outfit, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "teleRoll uses Android AccessibilityService to inspect only the visible Telegram window and dispatch scroll gestures. It does not access Telegram APIs, read your account, send messages, react, delete, join, or change settings. If Telegram’s state is unclear, teleRoll waits.",
                color = Muted,
                fontFamily = Figtree,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            Text("Got it", color = Violet, fontFamily = Figtree, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
}

@Composable
private fun TeleRollTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            androidx.compose.material3.darkColorScheme(
                primary = Violet,
                secondary = Cyan,
                background = Ink,
                surface = Panel,
                onSurface = ComposeColor.White,
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = ComposeColor(0xFF6750A4),
                secondary = ComposeColor(0xFF006874),
                background = ComposeColor(0xFFF8F7FF),
                surface = ComposeColor(0xFFF8F7FF),
                onSurface = ComposeColor(0xFF1A1B20),
            )
        },
        typography = androidx.compose.material3.Typography(
            bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontFamily = Figtree),
            bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = Figtree),
            titleLarge = androidx.compose.material3.Typography().titleLarge.copy(fontFamily = Outfit),
        ),
        content = content,
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        ?.any { info ->
            info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name == TelegramAccessibilityService::class.java.name
        } == true
}

private fun sendServiceAction(context: Context, action: String) {
    androidx.core.content.ContextCompat.startForegroundService(
        context,
        Intent(context, TelegramAccessibilityService::class.java).setAction(action),
    )
}

private const val ACTION_START = "com.teleroll.app.action.START"
private const val ACTION_PAUSE = "com.teleroll.app.action.PAUSE"
private const val ACTION_STOP = "com.teleroll.app.action.STOP"