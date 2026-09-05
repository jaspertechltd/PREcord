package com.example.precord

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.precord.data.PreferencesManager
import com.example.precord.ui.main.MainScreen
import com.example.precord.ui.screens.AudioPlayerScreen
import com.example.precord.ui.screens.CapturesScreen
import com.example.precord.ui.screens.OnboardingScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)
    val startKey = if (prefs.hasCompletedOnboarding) Main else Onboarding
    val backStack = rememberNavBackStack(startKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        prefs.hasCompletedOnboarding = true
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    },
                    onOpenSettings = {
                        prefs.hasCompletedOnboarding = true
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    }
                )
            }
            entry<Main> {
                MainScreen(
                    onNavigateToCaptures = { backStack.add(Captures) },
                    modifier = Modifier.safeDrawingPadding().padding(16.dp)
                )
            }
            entry<Captures> {
                CapturesScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenPlayer = { filePath -> backStack.add(AudioPlayer(filePath)) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<AudioPlayer> { key ->
                AudioPlayerScreen(
                    filePath = key.filePath,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        },
    )
}
