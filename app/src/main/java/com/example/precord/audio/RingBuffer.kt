package com.example.precord.audio

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class RingBuffer(val capacity: Int) {
    private val buffer = ByteArray(capacity)
    private var writePos = 0
    private var isFull = false
    private val lock = ReentrantLock()

    fun write(data: ByteArray, offset: Int, length: Int) {
        lock.withLock {
            var remaining = length
            var currentOffset = offset
            
            if (length >= capacity) {
                // If writing more than capacity, just keep the last 'capacity' bytes
                currentOffset = offset + length - capacity
                remaining = capacity
            }
            
            while (remaining > 0) {
                val spaceToTheEnd = capacity - writePos
                val chunk = min(remaining, spaceToTheEnd)
                System.arraycopy(data, currentOffset, buffer, writePos, chunk)
                writePos += chunk
                currentOffset += chunk
                remaining -= chunk
                
                if (writePos >= capacity) {
                    writePos = 0
                    isFull = true
                }
            }
        }
    }

    fun snapshot(): ByteArray {
        lock.withLock {
            if (!isFull) {
                val result = ByteArray(writePos)
                System.arraycopy(buffer, 0, result, 0, writePos)
                return result
            }
            
            val result = ByteArray(capacity)
            val spaceToTheEnd = capacity - writePos
            System.arraycopy(buffer, writePos, result, 0, spaceToTheEnd)
            System.arraycopy(buffer, 0, result, spaceToTheEnd, writePos)
            return result
        }
    }
    
    fun snapshotLast(bytes: Int): ByteArray {
        lock.withLock {
            val validBytes = if (isFull) capacity else writePos
            val toRead = min(bytes, validBytes)
            val result = ByteArray(toRead)
            
            if (toRead == 0) return result
            
            var readPos = writePos - toRead
            if (readPos < 0) {
                readPos += capacity
            }
            
            val spaceToTheEnd = capacity - readPos
            if (toRead <= spaceToTheEnd) {
                System.arraycopy(buffer, readPos, result, 0, toRead)
            } else {
                System.arraycopy(buffer, readPos, result, 0, spaceToTheEnd)
                System.arraycopy(buffer, 0, result, spaceToTheEnd, toRead - spaceToTheEnd)
            }
            
            return result
        }
    }

    fun getAmplitudes(numSamples: Int): FloatArray {
        lock.withLock {
            val result = FloatArray(numSamples)
            val validBytes = if (isFull) capacity else writePos
            val total16BitSamples = validBytes / 2
            
            if (total16BitSamples == 0) return result
            
            val step = total16BitSamples.toDouble() / numSamples
            val startOffset = if (isFull) writePos else 0
            
            for (i in 0 until numSamples) {
                val sampleIndex = (i * step).toInt()
                if (sampleIndex >= total16BitSamples) break
                val logicalByteIndex = sampleIndex * 2

                val physicalByteIndex = (startOffset + logicalByteIndex) % capacity
                val nextPhysicalByteIndex = (physicalByteIndex + 1) % capacity

                val low = buffer[physicalByteIndex].toInt() and 0xFF
                val high = buffer[nextPhysicalByteIndex].toInt()
                val sampleValue = (high shl 8) or low
                // Normalize -32768..32767 to -1.0..1.0
                result[i] = sampleValue / 32768.0f
            }
            
            return result
        }
    }

    fun clear() {
        lock.withLock {
            writePos = 0
            isFull = false
        }
    }
}
