package com.example.precord.audio

import android.media.AudioFormat

data class AudioConfig(
    val sampleRate: Int = 44100,
    val channels: Int = 1,
    val bitsPerSample: Int = 16,
    val bufferDurationSeconds: Int = 60
) {
    val bytesPerSample: Int
        get() = bitsPerSample / 8
        
    val bytesPerSecond: Int
        get() = sampleRate * channels * bytesPerSample

    val totalBufferBytes: Int
        get() = bytesPerSecond * bufferDurationSeconds

    val audioFormat: Int
        get() = if (bitsPerSample == 16) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT

    val channelConfig: Int
        get() = if (channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
}
