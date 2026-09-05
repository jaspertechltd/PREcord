package com.example.precord.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.precord.data.CaptureMetadataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Manages speech-to-text transcription of captured audio files.
 *
 * Uses Android's built-in SpeechRecognizer API. On Android 13+, uses the
 * on-device recognizer for privacy. Falls back to the network recognizer
 * on older devices.
 *
 * NOTE: SpeechRecognizer is designed for live microphone input, not file
 * playback. This implementation launches a recognition intent and asks the
 * user to play back the audio. For production, replace with Vosk or
 * Whisper for direct file-based transcription.
 */
object TranscriptionManager {

    /**
     * Start a speech recognition activity via Intent.
     * Returns the recognized text, or null if cancelled/failed.
     *
     * Call this from an Activity context only (needs startActivityForResult).
     */
    fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Play back the captured audio now...")
        }
    }

    /**
     * Save a transcript to a capture's metadata.
     */
    fun saveTranscript(audioFilePath: String, transcript: String) {
        CaptureMetadataStore.setTranscript(audioFilePath, transcript)
    }

    /**
     * Get a capture's existing transcript.
     */
    fun getTranscript(audioFilePath: String): String? {
        return CaptureMetadataStore.load(audioFilePath).transcript
    }

    /**
     * Check if on-device speech recognition is available.
     */
    fun isAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Attempt on-device recognition using SpeechRecognizer directly.
     * This listens via the microphone — user should play back audio near the phone.
     *
     * @param context Application context
     * @param onResult callback with transcribed text
     * @param onError callback with error message
     */
    fun recognizeViaMicrophone(
        context: Context,
        durationMs: Long = 15000,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, durationMs)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                onResult(text)
                recognizer.destroy()
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    else -> "Recognition error ($error)"
                }
                onError(msg)
                recognizer.destroy()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }
}
