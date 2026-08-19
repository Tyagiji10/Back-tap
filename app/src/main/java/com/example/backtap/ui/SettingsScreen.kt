package com.example.backtap.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.backtap.service.TapAccessibilityService
import java.util.Locale

val OledBlack = Color(0xFF000000)
val CardGrey = Color(0xFF202020)
val TextWhite = Color.White
val TextGray = Color.LightGray
val DimGray = Color.DarkGray

fun String.formatActionName(): String {
    if (this == "NONE") return "None"
    return this.lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

fun Modifier.bounceClickable(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        onClick()
                    }
                }
            }
        }
}

fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expectedComponentName = android.content.ComponentName(context, TapAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel = hiltViewModel()) {
    val isMasterToggleOn by viewModel.isMasterToggleOn.collectAsStateWithLifecycle()
    val sensitivityThreshold by viewModel.sensitivityThreshold.collectAsStateWithLifecycle()
    val pauseBelow10Percent by viewModel.pauseBelow10Percent.collectAsStateWithLifecycle()
    val doubleTapAction by viewModel.doubleTapAction.collectAsStateWithLifecycle()
    val tripleTapAction by viewModel.tripleTapAction.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showDoubleTapSheet by remember { mutableStateOf(false) }
    var showTripleTapSheet by remember { mutableStateOf(false) }

    val availableActions = listOf(
        "NONE", "HOME", "BACK", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS", "LOCK_SCREEN", "TAKE_SCREENSHOT",
        "PLAY_PAUSE", "NEXT_TRACK", "PREVIOUS_TRACK", "VOLUME_UP", "VOLUME_DOWN", "MUTE_UNMUTE"
    )

    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
    val hasAccelerometer = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val grayscaleSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = OledBlack,
        checkedTrackColor = TextWhite,
        uncheckedThumbColor = TextGray,
        uncheckedTrackColor = DimGray,
        checkedBorderColor = Color.Transparent,
        uncheckedBorderColor = Color.Transparent
    )

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Back Tap", color = TextWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = OledBlack,
                    scrolledContainerColor = OledBlack,
                    titleContentColor = TextWhite
                ),
                actions = {
                    Switch(
                        checked = isMasterToggleOn,
                        onCheckedChange = { viewModel.setMasterToggle(it) },
                        colors = grayscaleSwitchColors,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasAccelerometer) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF330000)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your device lacks the accelerometer sensor required to run this application.", 
                            color = Color(0xFFFF6666), 
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (!isAccessibilityEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGrey),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.bounceClickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enable Accessibility Service", 
                            color = TextWhite, 
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Group 1: Gestures
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGrey),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column {
                    PremiumSettingsRow(
                        icon = Icons.Default.TouchApp,
                        title = "Double Tap",
                        subtitle = doubleTapAction.formatActionName(),
                        onClick = { showDoubleTapSheet = true }
                    )
                    Divider(color = DimGray.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp))
                    PremiumSettingsRow(
                        icon = Icons.Default.TouchApp,
                        title = "Triple Tap",
                        subtitle = tripleTapAction.formatActionName(),
                        onClick = { showTripleTapSheet = true }
                    )
                }
            }

            // Group 2: Sensitivity
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGrey),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TextGray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Sensitivity", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            val sensitivityLabel = when (sensitivityThreshold) {
                                8.5f -> "Low (Hard Tap)"
                                3.5f -> "High (Light Tap)"
                                else -> "Medium"
                            }
                            Crossfade(targetState = sensitivityLabel, label = "sensitivity") { label ->
                                Text(label, color = TextGray, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = sensitivityThreshold,
                        onValueChange = { 
                            if (it != sensitivityThreshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.setSensitivity(it) 
                        },
                        valueRange = 3.5f..8.5f,
                        steps = 1,
                        colors = SliderDefaults.colors(
                            thumbColor = TextWhite,
                            activeTrackColor = TextWhite,
                            inactiveTrackColor = DimGray
                        )
                    )
                }
            }

            // Group 3: Battery
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGrey),
                shape = RoundedCornerShape(24.dp)
            ) {
                PremiumToggleRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "Pause Engine Below 10%",
                    checked = pauseBelow10Percent,
                    onCheckedChange = { viewModel.setPauseBelow10Percent(it) },
                    switchColors = grayscaleSwitchColors
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "© 2026 Shaurya Tyagi. All rights reserved.",
                    color = DimGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Designed & built with ",
                        color = DimGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Love",
                        tint = DimGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        " and a lot of coffee.",
                        color = DimGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDoubleTapSheet) {
        PremiumActionSheet(
            actions = availableActions,
            onDismiss = { showDoubleTapSheet = false },
            onActionSelected = {
                viewModel.setDoubleTapAction(it)
                showDoubleTapSheet = false
            }
        )
    }

    if (showTripleTapSheet) {
        PremiumActionSheet(
            actions = availableActions,
            onDismiss = { showTripleTapSheet = false },
            onActionSelected = {
                viewModel.setTripleTapAction(it)
                showTripleTapSheet = false
            }
        )
    }
}

@Composable
fun PremiumSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Crossfade(targetState = subtitle, label = "subtitle") { text ->
                Text(text, color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PremiumToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchColors: SwitchColors
) {
    val iconTint by animateColorAsState(targetValue = if (checked) TextWhite else TextGray, label = "iconTint")
    val iconRotation by animateFloatAsState(targetValue = if (checked) -15f else 0f, label = "iconRotation")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = iconTint,
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumActionSheet(
    actions: List<String>,
    onDismiss: () -> Unit,
    onActionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardGrey
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select Action", 
                color = TextWhite, 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
            actions.forEach { action ->
                Text(
                    text = action.formatActionName(),
                    color = TextWhite,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClickable { onActionSelected(action) }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}
