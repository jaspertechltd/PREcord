package com.example.precord.audio

import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Post-capture audio enhancement: normalization and high-pass filter.
 * Operates on raw PCM 16-bit data.
 */
object AudioEnhancer {

    /**
     * Apply all enhancements to a PCM audio buffer.
     * Returns a new enhanced PCM byte array.
     */
    fun enhance(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        normalize: Boolean = true,
        highPassHz: Float = 80f
    ): ByteArray {
        // Convert to float samples for processing
        var samples = pcmToFloat(pcmData)

        // Apply high-pass filter to remove low-frequency rumble
        if (highPassHz > 0f) {
            samples = applyHighPass(samples, sampleRate.toFloat(), highPassHz)
        }

        // Normalize volume
        if (normalize) {
            samples = normalizePeak(samples, targetPeak = 0.95f)
        }

        return floatToPcm(samples)
    }

    /**
     * Enhance an audio file in-place.
     * Reads the file, enhances PCM data, and writes it back.
     */
    fun enhanceFile(
        audioFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): Boolean {
        return try {
            val bytes = audioFile.readBytes()
            // Determine PCM data offset based on format
            val (pcmOffset, pcmLength) = detectPcmRegion(bytes, audioFile.extension)
            if (pcmOffset < 0) return false

            val pcmData = bytes.copyOfRange(pcmOffset, pcmOffset + pcmLength)
            val enhanced = enhance(pcmData, sampleRate, channels)

            // Write enhanced data back, preserving the header
            FileOutputStream(audioFile).use { fos ->
                fos.write(bytes, 0, pcmOffset)
                fos.write(enhanced)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun detectPcmRegion(fileBytes: ByteArray, extension: String): Pair<Int, Int> {
        return when (extension.lowercase()) {
            "wav" -> {
                // WAV header is typically 44 bytes, data starts at the "data" chunk
                val dataOffset = findWavDataChunk(fileBytes)
                if (dataOffset >= 0) {
                    Pair(dataOffset, fileBytes.size - dataOffset)
                } else {
                    Pair(44, fileBytes.size - 44) // fallback
                }
            }
            "aiff" -> Pair(72, fileBytes.size - 72) // AIFF-C header
            else -> Pair(-1, 0) // unsupported for in-place enhancement
        }
    }

    private fun findWavDataChunk(bytes: ByteArray): Int {
        // Search for "data" marker in WAV file
        for (i in 0 until bytes.size - 4) {
            if (bytes[i] == 'd'.code.toByte() &&
                bytes[i + 1] == 'a'.code.toByte() &&
                bytes[i + 2] == 't'.code.toByte() &&
                bytes[i + 3] == 'a'.code.toByte()
            ) {
                return i + 8 // skip "data" + 4 bytes chunk size
            }
        }
        return -1
    }

    // ═══════════════════════════════════════
    // DSP Processing
    // ═══════════════════════════════════════

    private fun pcmToFloat(pcm: ByteArray): FloatArray {
        val samples = FloatArray(pcm.size / 2)
        for (i in samples.indices) {
            val low = pcm[i * 2].toInt() and 0xFF
            val high = pcm[i * 2 + 1].toInt()
            samples[i] = ((high shl 8) or low) / 32768f
        }
        return samples
    }

    private fun floatToPcm(samples: FloatArray): ByteArray {
        val pcm = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val s = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt()
            pcm[i * 2] = (s and 0xFF).toByte()
            pcm[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return pcm
    }

    /**
     * Peak normalization: scale all samples so the peak reaches targetPeak.
     */
    private fun normalizePeak(samples: FloatArray, targetPeak: Float): FloatArray {
        var maxAbs = 0f
        for (s in samples) {
            maxAbs = max(maxAbs, abs(s))
        }
        if (maxAbs < 0.001f) return samples // silence, don't amplify noise

        val gain = targetPeak / maxAbs
        return FloatArray(samples.size) { (samples[it] * gain).coerceIn(-1f, 1f) }
    }

    /**
     * Simple single-pole IIR high-pass filter.
     * Removes frequencies below cutoffHz.
     */
    private fun applyHighPass(samples: FloatArray, sampleRate: Float, cutoffHz: Float): FloatArray {
        // RC high-pass filter coefficient
        val rc = 1f / (2f * Math.PI.toFloat() * cutoffHz)
        val dt = 1f / sampleRate
        val alpha = rc / (rc + dt)

        val output = FloatArray(samples.size)
        output[0] = samples[0]

        for (i in 1 until samples.size) {
            output[i] = alpha * (output[i - 1] + samples[i] - samples[i - 1])
        }

        return output
    }
}
