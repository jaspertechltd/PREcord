package com.example.precord.ui.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val application = mock(Application::class.java)
    val sharedPrefs = mock(SharedPreferences::class.java)
    `when`(application.getSharedPreferences(any(), any())).thenReturn(sharedPrefs)
    `when`(sharedPrefs.getBoolean(any(), any())).thenReturn(false)
    `when`(sharedPrefs.getInt(any(), any())).thenReturn(60)
    `when`(application.applicationContext).thenReturn(application)

    val viewModel = MainViewModel(application)
    val state = viewModel.uiState.first()
    assertEquals(false, state.isBuffering)
    assertEquals(0, state.bufferElapsedSeconds)
    assertEquals(60, state.bufferMaxSeconds)
    assertEquals(false, state.useExternalMic)
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest {
    val application = mock(Application::class.java)
    val sharedPrefs = mock(SharedPreferences::class.java)
    `when`(application.getSharedPreferences(any(), any())).thenReturn(sharedPrefs)
    `when`(sharedPrefs.getBoolean(any(), any())).thenReturn(false)
    `when`(sharedPrefs.getInt(any(), any())).thenReturn(60)
    `when`(application.applicationContext).thenReturn(application)

    val viewModel = MainViewModel(application)
    val state = viewModel.uiState.first()
    assertEquals(false, state.isBuffering)
  }
}
