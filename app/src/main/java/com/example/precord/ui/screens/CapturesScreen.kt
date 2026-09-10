package com.example.precord.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.precord.audio.AudioEnhancer
import com.example.precord.data.CaptureMetadataStore
import com.example.precord.data.PreferencesManager
import com.example.precord.service.CapturedFile
import com.example.precord.service.TranscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val PRESET_TAGS = listOf("Melody", "Voice", "Sound FX", "Meeting", "Idea", "Practice")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturesScreen(
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onNavigateBack = onBack
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    
    var captures by remember { mutableStateOf(emptyList<CapturedFile>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Refresh captures
    fun refreshList() {
        captures = getCaptures(context)
    }
    
    LaunchedEffect(Unit) {
        refreshList()
    }
    
    // Stop playing when leaving
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    val filteredCaptures = captures.filter { capture ->
        val metadata = CaptureMetadataStore.load(capture.filePath)
        val matchesSearch = capture.fileName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "⭐ Favorites" -> metadata.isFavorite
            else -> metadata.tags.contains(selectedFilter)
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Captures") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by filename") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "⭐ Favorites") + PRESET_TAGS
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
            
            if (filteredCaptures.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (captures.isEmpty()) Icons.Default.MicNone else Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (captures.isEmpty()) "No Captures Yet" else "No Matches Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (captures.isEmpty())
                            "Tap the Capture button on the main screen to save a retroactive audio moment."
                        else
                            "Try adjusting your search query or filters to find what you're looking for.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCaptures) { capture ->
                        val metadata = CaptureMetadataStore.load(capture.filePath)
                    var isFavorite by remember(capture.filePath) { mutableStateOf(metadata.isFavorite) }
                    var tags by remember(capture.filePath) { mutableStateOf(metadata.tags.toList()) }
                    var isEnhanced by remember(capture.filePath) { mutableStateOf(metadata.isEnhanced) }
                    var isEnhancing by remember(capture.filePath) { mutableStateOf(false) }
                    
                    var showTagDialog by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showTranscriptDialog by remember { mutableStateOf(false) }
                    var showTranscript by remember { mutableStateOf(false) }
                    
                    val transcript = TranscriptionManager.getTranscript(capture.filePath)
                    
                    val isPlaying = currentlyPlaying == capture.filePath
                    
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Favorite toggle
                                IconButton(onClick = {
                                    isFavorite = CaptureMetadataStore.toggleFavorite(capture.filePath)
                                }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = capture.fileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(capture.timestamp))
                                    Text(
                                        text = "$dateStr • ${capture.durationMs / 1000}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Play / Stop
                                IconButton(onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        currentlyPlaying = null
                                    } else {
                                        mediaPlayer?.release()
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(capture.filePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                currentlyPlaying = null
                                                release()
                                                mediaPlayer = null
                                            }
                                        }
                                        currentlyPlaying = capture.filePath
                                    }
                                }) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop" else "Play"
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Tags
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTagDialog = true }
                                .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (tags.isEmpty()) {
                                    Text("Add tags...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    tags.forEach { tag ->
                                        AssistChip(
                                            onClick = { showTagDialog = true },
                                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Actions row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                // Enhance
                                if (isEnhancing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (!prefs.isPro) {
                                                Toast.makeText(context, "Enhancement is a Pro feature", Toast.LENGTH_SHORT).show()
                                            } else if (!isEnhanced) {
                                                isEnhancing = true
                                                coroutineScope.launch {
                                                    val success = withContext(Dispatchers.IO) {
                                                        AudioEnhancer.enhanceFile(
                                                            File(capture.filePath),
                                                            prefs.sampleRate,
                                                            prefs.channels,
                                                            16
                                                        )
                                                    }
                                                    if (success) {
                                                        CaptureMetadataStore.setEnhanced(capture.filePath)
                                                        isEnhanced = true
                                                        Toast.makeText(context, "✓ Enhanced!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isEnhancing = false
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.AutoFixHigh,
                                            contentDescription = "Enhance",
                                            tint = if (isEnhanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                // Transcribe
                                IconButton(onClick = {
                                    if (!prefs.isPro) {
                                        Toast.makeText(context, "Transcription is a Pro feature", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showTranscriptDialog = true
                                    }
                                }) {
                                    Icon(Icons.Default.Description, contentDescription = "Transcribe")
                                }
                                
                                // Open full player (A-B Loop)
                                IconButton(onClick = {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    currentlyPlaying = null
                                    onOpenPlayer(capture.filePath)
                                }) {
                                    Icon(Icons.Default.OpenInFull, contentDescription = "Open Player")
                                }

                                // Share
                                IconButton(onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(capture.filePath))
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Capture"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                                
                                // Delete
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                            
                            // Transcript display
                            if (transcript != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTranscript = !showTranscript },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (showTranscript) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle transcript"
                                    )
                                    Text("Transcript", style = MaterialTheme.typography.labelMedium)
                                }
                                AnimatedVisibility(visible = showTranscript) {
                                    Text(
                                        text = transcript,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Tag Dialog
                    if (showTagDialog) {
                        AlertDialog(
                            onDismissRequest = { showTagDialog = false },
                            title = { Text("Tags") },
                            text = {
                                Column {
                                    PRESET_TAGS.forEach { tag ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (tags.contains(tag)) {
                                                        CaptureMetadataStore.removeTag(capture.filePath, tag)
                                                        tags = tags - tag
                                                    } else {
                                                        CaptureMetadataStore.addTag(capture.filePath, tag)
                                                        tags = tags + tag
                                                    }
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Checkbox(
                                                checked = tags.contains(tag),
                                                onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tag)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTagDialog = false }) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                    
                    // Delete Dialog
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete capture?") },
                            text = { Text("Are you sure you want to delete ${capture.fileName}?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    CaptureMetadataStore.delete(capture.filePath)
                                    File(capture.filePath).delete()
                                    showDeleteDialog = false
                                    if (isPlaying) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        currentlyPlaying = null
                                    }
                                    refreshList()
                                }) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    // Transcript Dialog
                    if (showTranscriptDialog) {
                        AlertDialog(
                            onDismissRequest = { showTranscriptDialog = false },
                            title = { Text("Transcript") },
                            text = {
                                Column {
                                    if (transcript != null) {
                                        Text(transcript)
                                    } else {
                                        Text("No transcript available.")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showTranscriptDialog = false
                                    TranscriptionManager.recognizeViaMicrophone(
                                        context,
                                        capture.durationMs,
                                        onResult = { result ->
                                            TranscriptionManager.saveTranscript(capture.filePath, result)
                                            refreshList()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Transcription error: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }) {
                                    Text("Re-transcribe")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTranscriptDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

fun getCaptures(context: Context): List<CapturedFile> {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Precord")
    if (!dir.exists()) return emptyList()
    
    val extensions = listOf("wav", "mp4", "mp3", "aiff", "ogg", "flac")
    
    return dir.listFiles()?.filter { file ->
        file.isFile && extensions.any { file.name.endsWith(".$it", ignoreCase = true) }
    }?.map { file ->
        CapturedFile(
            filePath = file.absolutePath,
            fileName = file.name,
            timestamp = file.lastModified(),
            durationMs = 0L, // Need MediaMetadataRetriever for real duration if needed
            fileSizeBytes = file.length()
        )
    }?.sortedByDescending { it.timestamp } ?: emptyList()
}
