package com.example.precord.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.precord.data.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button or placeholder
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous page"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { index ->
                        val color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Forward button or placeholder
                if (pagerState.currentPage < 3) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next page"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                when (page) {
                    0 -> Page1Welcome()
                    1 -> Page2LegalWarning()
                    2 -> Page3ProMode()
                    3 -> Page4VolumeButtonCombo(onOpenSettings, onComplete)
                }
            }
        }
    }
}

@Composable
fun Page1Welcome() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Precord!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "The world's first retrospective audio recorder for Android. Precord continuously listens through your microphone, keeping a rolling buffer of audio. When inspiration strikes — or something worth saving just happened — simply tap Capture to save the audio you didn't know you'd need.\n\nNo more missed moments. No more 'I wish I was recording.'",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }) {
            Text("Disable Battery Optimization")
        }
        Text(
            text = "Recommended for continuous background recording",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page2LegalWarning() {
    val context = LocalContext.current
    val countries = listOf(
        "Canada", "United States", "United Kingdom", "Australia", 
        "Germany", "France", "Japan", "Brazil", "India", "Mexico", 
        "South Korea", "Sweden", "Netherlands", "South Africa", "New Zealand"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countries[0]) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please Use Responsibly",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "The recording of others can have different legalities in public and private settings in different countries. Make sure you know the laws for your situation.\n\nWe are not responsible for any misuse of this app.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCountry,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Country") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    countries.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country) },
                            onClick = {
                                selectedCountry = country
                                expanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = {
                val query = Uri.encode("Audio recording laws public private $selectedCountry")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                context.startActivity(intent)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Search laws"
                )
            }
        }
    }
}

@Composable
fun Page3ProMode() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var isPro by remember { mutableStateOf(prefs.isPro) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Pro Mode",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
        )
        Text(
            text = "Unlock Pro Mode!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Supercharge your retrospective recording experience.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Free: up to 10 seconds\nPro: up to 4 hours of retrospective recording",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$4.99 CAD — One-time purchase",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isPro) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Pro Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Pro Mode Active",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Button(
                onClick = {
                    prefs.isPro = true
                    isPro = true
                    Toast.makeText(context, "✓ Pro Mode unlocked! (Debug)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Buy Pro — $4.99")
            }
            Text(
                text = "DEBUG: This button simulates a purchase. Replace with Google Play Billing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun Page4VolumeButtonCombo(
    onOpenSettings: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Capture Shortcuts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Capture audio without even looking at your screen!\n\n" +
                "📱 Volume Button Combo — Press a sequence of volume buttons to trigger a capture. " +
                "Enable this in Settings and customize your combo.\n\n" +
                "🤳 Shake-to-Capture (Pro) — Simply shake your phone to save the audio buffer instantly.",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "These shortcuts work even when your screen is off (with background buffering enabled). Volume buttons still control your media volume normally — the combo is detected from a specific sequence.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(onClick = onOpenSettings) {
            Text("Configure in Settings →")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Get Started")
        }
    }
}
