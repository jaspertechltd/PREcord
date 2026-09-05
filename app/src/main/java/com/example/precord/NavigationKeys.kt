package com.example.precord

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Onboarding : NavKey
@Serializable data object Main : NavKey
@Serializable data object Captures : NavKey
@Serializable data class AudioPlayer(val filePath: String) : NavKey
