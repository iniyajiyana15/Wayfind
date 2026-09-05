package com.wayfind.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfind.app.data.model.*
import com.wayfind.app.domain.VoiceState
import com.wayfind.app.ui.components.*
import com.wayfind.app.ui.theme.*
import com.wayfind.app.ui.viewmodel.WayfindViewModel

@Composable
fun MapNavigationScreen(
    viewModel: WayfindViewModel,
    onBackToProfile: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val activeFloorId by viewModel.activeFloorId.collectAsState()
    val building by viewModel.building.collectAsState()
    val userX by viewModel.userX.collectAsState()
    val userY by viewModel.userY.collectAsState()
    val userFloorId by viewModel.userFloorId.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val currentRoute by viewModel.currentRoute.collectAsState()
    val isElevatorE1Blocked by viewModel.isElevatorE1Blocked.collectAsState()
    val isElevatorE2Blocked by viewModel.isElevatorE2Blocked.collectAsState()
    val closedDoors by viewModel.closedDoors.collectAsState()
    val hazardDumps by viewModel.hazardDumps.collectAsState()
    val isDevPanelVisible by viewModel.isDevPanelVisible.collectAsState()
    val activeAnnouncement by viewModel.announcementManager.activeAnnouncement.collectAsState()
    val voiceState by viewModel.voiceAssistantManager.voiceState.collectAsState()
    val voiceStatusMessage by viewModel.voiceAssistantManager.statusMessage.collectAsState()
    val recognizedText by viewModel.voiceAssistantManager.recognizedText.collectAsState()

    var showSearchSheet by remember { mutableStateOf(false) }
    val isBlindMode = selectedProfile == AccessibilityProfileType.BLIND_LOW_VISION
    val currentFloor = building.floors.find { it.id == activeFloorId } ?: building.floors.first()

    // Runtime permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onDoubleTapForVoice()
        } else {
            viewModel.onMicPermissionDenied()
        }
    }

    val handleDoubleTap: () -> Unit = {
        if (viewModel.voiceAssistantManager.hasRecordPermission()) {
            viewModel.onDoubleTapForVoice()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Greet blind user on entering map screen
    LaunchedEffect(isBlindMode) {
        if (isBlindMode) {
            viewModel.startVoiceNavigationForBlind()
        }
    }

    // Mic pulse animation (only active while listening)
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rootModifier = Modifier
        .fillMaxSize()
        .background(Slate900)
        .then(
            if (isBlindMode) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { handleDoubleTap() })
                }
            } else Modifier
        )

    Box(modifier = rootModifier) {
        // ── Layer 1: Map Canvas (always visible) ──────────────────────────────
        MapViewCanvas(
            floor = currentFloor,
            userX = if (userFloorId == activeFloorId) userX else -100f,
            userY = if (userFloorId == activeFloorId) userY else -100f,
            route = if (userFloorId == activeFloorId) currentRoute else null,
            destination = selectedDestination,
            closedDoors = closedDoors,
            hazardDumps = hazardDumps,
            textMeasurer = textMeasurer,
            modifier = Modifier.fillMaxSize()
        )

        // ── Layer 2: Top Header (always visible) ──────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Pill
                        Surface(
                            onClick = onBackToProfile,
                            color = PrimaryBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedProfile.title,
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.toggleDevPanel() },
                            modifier = Modifier.size(36.dp).background(Slate700, CircleShape)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Dev Controls", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Destination row — tap to search (sighted) or shows spoken destination (blind)
                    Surface(
                        onClick = { if (!isBlindMode) showSearchSheet = true },
                        color = Slate900,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = DestinationYellow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedDestination?.name
                                        ?: if (isBlindMode) "Double-tap & speak destination..." else "Select Destination Room...",
                                    color = if (selectedDestination != null) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (currentRoute != null) {
                                    Text(
                                        text = "Route: ${currentRoute?.totalDistanceMeters?.toInt()}m • ${currentRoute?.estimatedTimeMinutes} min • Accessible",
                                        color = AccessibleGreen,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floor Selector Tabs
            Row(
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                building.floors.forEach { fl ->
                    val isSelected = fl.id == activeFloorId
                    Button(
                        onClick = { viewModel.selectFloor(fl.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PrimaryBlue else Slate800),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("FLOOR ${fl.level}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Layer 3: Turn-by-Turn Step Banner ────────────────────────────────
        currentRoute?.steps?.firstOrNull()?.let { step ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 200.dp, end = 130.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(step.instruction, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (step.distanceMeters > 0) {
                            Text("In ${step.distanceMeters}m", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Layer 4: Bottom Controls Row (Joystick + optional Mic) ───────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Blind mode: Compact Mic panel on the left
            if (isBlindMode) {
                CompactVoicePanel(
                    voiceState = voiceState,
                    statusMessage = voiceStatusMessage,
                    recognizedText = recognizedText,
                    pulseScale = pulseScale,
                    onTap = handleDoubleTap,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Joystick (always on right)
            VirtualJoystick(onMove = { dx, dy -> viewModel.handleJoystickMove(dx, dy) })
        }

        // ── Common overlays ───────────────────────────────────────────────────

        AnnouncementBanner(
            announcement = activeAnnouncement,
            onDismiss = { viewModel.announcementManager.dismissAnnouncement() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (isDevPanelVisible) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                EnvironmentControlSheet(
                    isElevatorE1Blocked = isElevatorE1Blocked,
                    isElevatorE2Blocked = isElevatorE2Blocked,
                    closedDoors = closedDoors,
                    hazardDumps = hazardDumps,
                    onToggleElevatorE1 = { viewModel.toggleElevatorE1() },
                    onToggleElevatorE2 = { viewModel.toggleElevatorE2() },
                    onToggleDoor = { viewModel.toggleDoor(it) },
                    onToggleDump = { viewModel.toggleHazardDump(it) },
                    onRunDemo = { viewModel.toggleDevPanel(); viewModel.runDemoSequence() },
                    onDismiss = { viewModel.toggleDevPanel() }
                )
            }
        }

        if (showSearchSheet && !isBlindMode) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                DestinationSearchSheet(
                    rooms = viewModel.building.value.floors.flatMap { it.rooms },
                    onSelectDestination = { room ->
                        viewModel.setDestination(room)
                        showSearchSheet = false
                    },
                    onDismiss = { showSearchSheet = false }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact Voice Panel (Blind mode — lives beside the joystick)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CompactVoicePanel(
    voiceState: VoiceState,
    statusMessage: String,
    recognizedText: String,
    pulseScale: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val micScale = if (voiceState == VoiceState.LISTENING) pulseScale else 1f

    val bgColor = when (voiceState) {
        VoiceState.LISTENING  -> Color(0xFF0D2137)
        VoiceState.PROCESSING -> Color(0xFF0A1929)
        VoiceState.SPEAKING   -> Color(0xFF0A2318)
        VoiceState.PERMISSION_DENIED,
        VoiceState.UNAVAILABLE -> Color(0xFF1C0A0A)
        else                  -> Slate800.copy(alpha = 0.92f)
    }

    val micTint = when (voiceState) {
        VoiceState.LISTENING  -> AccentCyan
        VoiceState.SPEAKING   -> AccessibleGreen
        VoiceState.PROCESSING -> Color(0xFFFFD700)
        VoiceState.PERMISSION_DENIED,
        VoiceState.UNAVAILABLE -> HazardRed
        else                  -> Color.White
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onTap() })
                }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mic icon with pulse
            Box(
                modifier = Modifier
                    .scale(micScale)
                    .size(52.dp)
                    .background(micTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Navigation",
                    tint = micTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            // State label
            Text(
                text = when (voiceState) {
                    VoiceState.LISTENING  -> "Listening..."
                    VoiceState.PROCESSING -> "Processing..."
                    VoiceState.SPEAKING   -> "Speaking..."
                    VoiceState.PERMISSION_DENIED -> "No permission"
                    VoiceState.UNAVAILABLE -> "Unavailable"
                    else                  -> "Double-tap to speak"
                },
                color = micTint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Recognized text echo
            if (recognizedText.isNotBlank()) {
                Text(
                    text = recognizedText,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            // Hint
            if (voiceState == VoiceState.IDLE) {
                Text(
                    text = "Tap mic or double-tap anywhere",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
