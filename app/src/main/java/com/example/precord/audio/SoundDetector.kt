package com.example.precord.audio

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Analyzes PCM audio data for silence→sound transitions.
 * Returns bookmark timestamps where sound begins after a period of silence.
 */
object SoundDetector {

    data class SoundEvent(
        val offsetMs: Long,
        val label: String
    )

    /**
     * Scans PCM 16-bit data and detects transitions from silence to sound.
     *
     * @param pcmData raw 16-bit PCM audio data
     * @param sampleRate audio sample rate in Hz
     * @param channels number of audio channels
     * @param silenceThresholdDb RMS threshold in dB below which audio is considered silence (e.g., -40f)
     * @param windowMs analysis window size in milliseconds
     * @param minSilenceMs minimum silence duration before a transition counts
     * @return list of SoundEvents with millisecond offsets
     */
    fun detectTransitions(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        silenceThresholdDb: Float = -40f,
        windowMs: Int = 100,
        minSilenceMs: Long = 500
    ): List<SoundEvent> {
        val bytesPerSample = 2 // 16-bit
        val bytesPerFrame = bytesPerSample * channels
        val framesPerWindow = (sampleRate * windowMs) / 1000
        val bytesPerWindow = framesPerWindow * bytesPerFrame
        val totalFrames = pcmData.size / bytesPerFrame

        if (totalFrames < framesPerWindow) return emptyList()

        val events = mutableListOf<SoundEvent>()
        var wasSilent = true
        var silenceStartMs = 0L
        var eventCount = 0

        var offset = 0
        while (offset + bytesPerWindow <= pcmData.size) {
            val rmsDb = calculateRmsDb(pcmData, offset, bytesPerWindow)
            val currentTimeMs = (offset.toLong() / bytesPerFrame) * 1000 / sampleRate

            val isSilent = rmsDb < silenceThresholdDb

            if (wasSilent && !isSilent) {
                // Transition: silence → sound
                val silenceDuration = currentTimeMs - silenceStartMs
                if (silenceDuration >= minSilenceMs || currentTimeMs == 0L) {
                    eventCount++
                    events.add(SoundEvent(
                        offsetMs = currentTimeMs,
                        label = "Sound #$eventCount"
                    ))
                }
            }

            if (isSilent && !wasSilent) {
                silenceStartMs = currentTimeMs
            }

            wasSilent = isSilent
            offset += bytesPerWindow
        }

        return events
    }

    /**
     * Calculate RMS level in dB for a chunk of 16-bit PCM data.
     */
    private fun calculateRmsDb(data: ByteArray, offset: Int, length: Int): Float {
        var sumSquares = 0.0
        var sampleCount = 0

        var i = offset
        while (i + 1 < offset + length && i + 1 < data.size) {
            val low = data[i].toInt() and 0xFF
            val high = data[i + 1].toInt()
            val sample = (high shl 8) or low
            sumSquares += (sample * sample).toDouble()
            sampleCount++
            i += 2
        }

        if (sampleCount == 0) return -100f

        val rms = sqrt(sumSquares / sampleCount)
        // Convert to dB (0 dB = full scale = 32768)
        return if (rms > 0) (20 * log10(rms / 32768.0)).toFloat() else -100f
    }
}
