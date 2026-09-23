package com.example.precord.ui.main

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.precord.data.PreferencesManager
import com.example.precord.ui.components.AnimatedWaveformVisualizer
import com.example.precord.ui.components.BufferIndicator
import com.example.precord.ui.components.CaptureButton
import com.example.precord.ui.components.ProUpgradeSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCaptures: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showProUpgrade by remember { mutableStateOf(false) }
    var isPro by remember { mutableStateOf(prefs.isPro) }
    val captureAnimTrigger by viewModel.captureAnimationTrigger.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Capture animation state
    val captureAnimScale = remember { Animatable(1f) }
    val captureAnimAlpha = remember { Animatable(0f) }
    val captureAnimOffsetY = remember { Animatable(0f) }
    var folderButtonPosition by remember { mutableStateOf(Offset.Zero) }
    var waveformPosition by remember { mutableStateOf(Offset.Zero) }

    // Trigger capture animation when captureAnimTrigger changes
    LaunchedEffect(captureAnimTrigger) {
        if (captureAnimTrigger > 0) {
            // Start visible
            captureAnimAlpha.snapTo(0.8f)
            captureAnimScale.snapTo(1f)
            captureAnimOffsetY.snapTo(0f)

            // Animate: shrink + move down + fade out
            launch { captureAnimScale.animateTo(0.1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { captureAnimAlpha.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
            launch { captureAnimOffsetY.animateTo(folderButtonPosition.y - waveformPosition.y, tween(600, easing = FastOutSlowInEasing)) }
        }
    }

    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (!uiState.isPermissionGranted && !uiState.showPermissionRationale) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Kill button + App title & subtitle
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Kill process button - top left
                    IconButton(
                        onClick = {
                            viewModel.stopBuffering()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 200)
                        },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kill Process",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Precord",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Retrospective Recorder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        BufferIndicator(
                            isBuffering = uiState.isBuffering,
                            elapsedSeconds = uiState.bufferElapsedSeconds,
                            maxBufferSeconds = uiState.bufferMaxSeconds
                        )
                    }
                }

                // Center: Animated waveform visualizer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .onGloballyPositioned { coords ->
                            waveformPosition = coords.positionInRoot()
                        }
                ) {
                    AnimatedWaveformVisualizer(
                        amplitudes = uiState.amplitudes,
                        isBuffering = uiState.isBuffering,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Capture animation overlay (ghost of waveform shrinking to folder)
                    if (captureAnimAlpha.value > 0.01f) {
                        AnimatedWaveformVisualizer(
                            amplitudes = uiState.amplitudes,
                            isBuffering = uiState.isBuffering,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(captureAnimScale.value)
                                .alpha(captureAnimAlpha.value)
                                .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) {
                                    captureAnimOffsetY.value.toDp()
                                })
                        )
                    }
                }

                // Bottom: Capture button & action bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.showPermissionRationale) {
                        Text(
                            text = "Precord needs microphone access to continuously buffer audio.\nNothing is saved until you tap Capture.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { permissionLauncher.launch(permissions.toTypedArray()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Grant Permissions")
                        }
                    } else {
                        CaptureButton(
                            onClick = { viewModel.capture() },
                            isBuffering = uiState.isBuffering,
                            enabled = uiState.isBuffering
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // External mic quick toggle
                        IconButton(onClick = { viewModel.toggleExternalMic() }) {
                            Icon(
                                imageVector = if (uiState.useExternalMic) Icons.Default.Headset else Icons.Default.HeadsetOff,
                                contentDescription = if (uiState.useExternalMic) "External Mic On" else "External Mic Off",
                                tint = if (uiState.useExternalMic) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Buy Pro / Pro Mode indicator
                        if (isPro) {
                            Text(
                                text = "Pro Mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else {
                            TextButton(onClick = { showProUpgrade = true }) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Buy Pro",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Settings
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Captures list
                        IconButton(
                            onClick = onNavigateToCaptures,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                folderButtonPosition = coords.positionInRoot()
                            }
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Captures",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showSettings) {
        com.example.precord.ui.screens.SettingsSheet(
            onDismiss = { showSettings = false }
        )
    }

    if (showProUpgrade) {
        ProUpgradeSheet(
            onDismiss = { showProUpgrade = false },
            onPurchased = {
                prefs.isPro = true
                isPro = true
                showProUpgrade = false
                Toast.makeText(context, "✓ Pro Mode unlocked!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
