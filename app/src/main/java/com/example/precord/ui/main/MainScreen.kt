package com.example.precord.ui.main

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.precord.data.PreferencesManager
import com.example.precord.ui.components.AnimatedWaveformVisualizer
import com.example.precord.ui.components.BufferIndicator
import com.example.precord.ui.components.CaptureButton

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
    var isPro by remember { mutableStateOf(prefs.isPro) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: App title & subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

            // Center: Animated waveform visualizer
            AnimatedWaveformVisualizer(
                amplitudes = uiState.amplitudes,
                isBuffering = uiState.isBuffering,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )

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
                        TextButton(
                            onClick = {
                                // Debug: simulate purchase
                                prefs.isPro = true
                                isPro = true
                                Toast.makeText(context, "✓ Pro Mode unlocked! (Debug)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
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
                    IconButton(onClick = onNavigateToCaptures) {
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

    if (showSettings) {
        com.example.precord.ui.screens.SettingsSheet(
            onDismiss = { showSettings = false }
        )
    }
}
