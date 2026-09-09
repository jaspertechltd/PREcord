package com.example.precord.audio

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioEncoder {

    enum class Format(val extension: String, val displayName: String, val mimeType: String?) {
        WAV("wav", "WAV (Lossless)", null),
        MP3("mp3", "MP3 (Compressed)", "audio/mpeg"),
        AIFF("aiff", "AIFF (Lossless)", null),
        OGG("ogg", "OGG Vorbis", "audio/ogg"),
        FLAC("flac", "FLAC (Lossless)", "audio/flac");

        companion object {
            fun fromString(name: String): Format =
                entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WAV
        }
    }

    fun encodeToFormat(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        format: Format,
        outputFile: File
    ): Boolean {
        return when (format) {
            Format.WAV -> encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)
            Format.AIFF -> encodeToAiff(pcmData, sampleRate, channels, bitsPerSample, outputFile)
            Format.FLAC -> encodeToFlac(pcmData, sampleRate, channels, bitsPerSample, outputFile)
            Format.MP3 -> encodeWithMediaCodec(pcmData, sampleRate, channels, bitsPerSample, outputFile, format)
            Format.OGG -> encodeWithMediaCodec(pcmData, sampleRate, channels, bitsPerSample, outputFile, format)
        }
    }

    fun encodeToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        outputFile: File
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { fos ->
                val totalAudioLen = pcmData.size
                val totalDataLen = totalAudioLen + 36
                val longSampleRate = sampleRate.toLong()
                val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

                val header = writeWavHeader(
                    totalAudioLen.toLong(),
                    totalDataLen.toLong(),
                    longSampleRate,
                    channels,
                    byteRate,
                    bitsPerSample
                )

                fos.write(header)
                fos.write(pcmData)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun encodeToAiff(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        outputFile: File
    ): Boolean {
        return try {
            // Use AIFF-C with 'sowt' compression to store little-endian PCM
            // This avoids byte-swapping and is widely supported
            FileOutputStream(outputFile).use { fos ->
                val numSampleFrames = pcmData.size / (channels * (bitsPerSample / 8))
                val header = writeAiffCHeader(
                    pcmData.size,
                    sampleRate,
                    channels,
                    bitsPerSample,
                    numSampleFrames
                )
                fos.write(header)
                fos.write(pcmData)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun encodeToFlac(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        outputFile: File
    ): Boolean {
        return try {
            val codecName = findEncoder("audio/flac")
            if (codecName == null) {
                // Fallback to WAV using outputFile directly
                return encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)
            }

            val codec = MediaCodec.createByCodecName(codecName)
            val format = MediaFormat.createAudioFormat("audio/flac", sampleRate, channels)
            format.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)
            format.setInteger(MediaFormat.KEY_BIT_RATE, sampleRate * channels * bitsPerSample)
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            FileOutputStream(outputFile).use { fos ->
                encodeWithCodec(codec, pcmData, fos)
            }

            codec.stop()
            codec.release()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to WAV using outputFile directly
            encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)
        }
    }

    private fun encodeWithMediaCodec(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        outputFile: File,
        format: Format
    ): Boolean {
        val mimeType = format.mimeType
            ?: return encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)

        return try {
            val codecName = findEncoder(mimeType)
                ?: return encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)

            val codec = MediaCodec.createByCodecName(codecName)
            val mediaFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channels)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            mediaFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)

            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            FileOutputStream(outputFile).use { fos ->
                encodeWithCodec(codec, pcmData, fos)
            }

            codec.stop()
            codec.release()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to WAV using outputFile directly
            encodeToWav(pcmData, sampleRate, channels, bitsPerSample, outputFile)
        }
    }

    private fun encodeWithCodec(codec: MediaCodec, pcmData: ByteArray, outputStream: FileOutputStream) {
        val bufferInfo = MediaCodec.BufferInfo()
        var inputOffset = 0
        var inputDone = false

        while (true) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val chunkSize = minOf(inputBuffer.remaining(), pcmData.size - inputOffset)

                    if (chunkSize > 0) {
                        inputBuffer.put(pcmData, inputOffset, chunkSize)
                        inputOffset += chunkSize
                        val flags = if (inputOffset >= pcmData.size) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        codec.queueInputBuffer(inputIndex, 0, chunkSize, 0, flags)
                        if (inputOffset >= pcmData.size) inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.get(chunk)
                outputStream.write(chunk)
                codec.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                // Give it a few more tries then bail
                val retryIndex = codec.dequeueOutputBuffer(bufferInfo, 100000)
                if (retryIndex < 0) break
                val ob = codec.getOutputBuffer(retryIndex) ?: break
                val ch = ByteArray(bufferInfo.size)
                ob.get(ch)
                outputStream.write(ch)
                codec.releaseOutputBuffer(retryIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    private fun findEncoder(mimeType: String): String? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            for (type in info.supportedTypes) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return info.name
                }
            }
        }
        return null
    }

    private fun writeWavHeader(
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long,
        bitsPerSample: Int
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }

    private fun writeAiffCHeader(
        pcmDataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        numSampleFrames: Int
    ): ByteArray {
        // AIFF-C header with 'sowt' compression (little-endian PCM)
        val buf = ByteBuffer.allocate(72)
        buf.order(ByteOrder.BIG_ENDIAN)

        // FORM chunk
        buf.put("FORM".toByteArray())
        buf.putInt(pcmDataSize + 72 - 8) // total file size - 8
        buf.put("AIFC".toByteArray())

        // FVER chunk (required for AIFF-C)
        buf.put("FVER".toByteArray())
        buf.putInt(4) // chunk size
        buf.putInt(0xA2805140.toInt()) // AIFF-C version 1 timestamp

        // COMM chunk for AIFF-C
        buf.put("COMM".toByteArray())
        buf.putInt(24) // chunk size: 2(channels) + 4(frames) + 2(bitdepth) + 10(sampleRate) + 4(compressionType) + 2(pascal string)
        buf.putShort(channels.toShort())
        buf.putInt(numSampleFrames)
        buf.putShort(bitsPerSample.toShort())
        // 80-bit extended float for sample rate
        buf.put(convertToIeee80(sampleRate.toDouble()))
        // Compression type: 'sowt' = little-endian PCM
        buf.put("sowt".toByteArray())
        buf.putShort(0) // Pascal string (empty)

        // SSND chunk header
        buf.put("SSND".toByteArray())
        buf.putInt(pcmDataSize + 8) // chunk size (data + 8 bytes for offset/blockSize)
        buf.putInt(0) // offset
        buf.putInt(0) // blockSize

        return buf.array()
    }

    private fun convertToIeee80(value: Double): ByteArray {
        val result = ByteArray(10)
        val v = value
        var exponent = 16383 + 63 // bias + 63 for the mantissa shift
        var mantissa = v.toLong()

        if (v == 0.0) return result

        // Normalize: find the position of the highest set bit
        var temp = mantissa
        var shift = 0
        while (temp != 0L && temp and (1L shl 63) == 0L) {
            temp = temp shl 1
            shift++
        }
        // For integer sample rates, simplified conversion
        val intValue = v.toInt()
        var exp = 16383 + 31
        var mant = intValue.toLong() shl 32

        result[0] = ((exp shr 8) and 0xFF).toByte()
        result[1] = (exp and 0xFF).toByte()
        result[2] = ((mant shr 56) and 0xFF).toByte()
        result[3] = ((mant shr 48) and 0xFF).toByte()
        result[4] = ((mant shr 40) and 0xFF).toByte()
        result[5] = ((mant shr 32) and 0xFF).toByte()
        result[6] = ((mant shr 24) and 0xFF).toByte()
        result[7] = ((mant shr 16) and 0xFF).toByte()
        result[8] = ((mant shr 8) and 0xFF).toByte()
        result[9] = (mant and 0xFF).toByte()

        return result
    }
}
