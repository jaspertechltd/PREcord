package com.example.precord.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages JSON sidecar metadata files alongside audio captures.
 * Each capture gets a .json file with tags, transcript, bookmarks, favorite status.
 *
 * PERFORMANCE OPTIMIZATION:
 * Implemented in-memory caching using ConcurrentHashMap to solve a severe N+1 I/O
 * bottleneck when reading metadata synchronously from disk during Jetpack Compose
 * LazyColumn rendering. Caches both existing and negative/empty states to avoid
 * persistent I/O blocking. Always returns deep copies to prevent implicit cache
 * mutation and missed recompositions. Expected Impact: Eliminates N+1 disk reads,
 * significantly reducing UI jank during scrolling.
 */
object CaptureMetadataStore {

    data class CaptureMetadata(
        val tags: MutableList<String> = mutableListOf(),
        val isFavorite: Boolean = false,
        val transcript: String? = null,
        val bookmarks: List<BookmarkEntry> = emptyList(),
        val isEnhanced: Boolean = false
    ) {
        fun deepCopy(): CaptureMetadata = this.copy(
            tags = tags.toMutableList(),
            bookmarks = bookmarks.toList()
        )
    }

    data class BookmarkEntry(
        val offsetMs: Long,
        val label: String
    )

    private val cache = ConcurrentHashMap<String, CaptureMetadata>()

    private fun metadataFile(audioFilePath: String): File {
        val audioFile = File(audioFilePath)
        return File(audioFile.parent, audioFile.nameWithoutExtension + ".meta.json")
    }

    fun load(audioFilePath: String): CaptureMetadata {
        cache[audioFilePath]?.let { return it.deepCopy() }

        val file = metadataFile(audioFilePath)
        if (!file.exists()) {
            val emptyMeta = CaptureMetadata()
            cache[audioFilePath] = emptyMeta
            return emptyMeta.deepCopy()
        }

        return try {
            val json = JSONObject(file.readText())
            val tags = mutableListOf<String>()
            json.optJSONArray("tags")?.let { arr ->
                for (i in 0 until arr.length()) tags.add(arr.getString(i))
            }
            val bookmarks = mutableListOf<BookmarkEntry>()
            json.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val bm = arr.getJSONObject(i)
                    bookmarks.add(BookmarkEntry(bm.getLong("offsetMs"), bm.optString("label", "")))
                }
            }
            val metadata = CaptureMetadata(
                tags = tags,
                isFavorite = json.optBoolean("isFavorite", false),
                transcript = if (json.has("transcript")) json.getString("transcript") else null,
                bookmarks = bookmarks,
                isEnhanced = json.optBoolean("isEnhanced", false)
            )
            cache[audioFilePath] = metadata
            metadata.deepCopy()
        } catch (_: Exception) {
            val fallback = CaptureMetadata()
            cache[audioFilePath] = fallback
            fallback.deepCopy()
        }
    }

    fun save(audioFilePath: String, metadata: CaptureMetadata) {
        val copyToSave = metadata.deepCopy()
        cache[audioFilePath] = copyToSave

        val file = metadataFile(audioFilePath)
        try {
            val json = JSONObject().apply {
                put("tags", JSONArray(copyToSave.tags))
                put("isFavorite", copyToSave.isFavorite)
                copyToSave.transcript?.let { put("transcript", it) }
                put("isEnhanced", copyToSave.isEnhanced)
                val bmArray = JSONArray()
                copyToSave.bookmarks.forEach { bm ->
                    bmArray.put(JSONObject().apply {
                        put("offsetMs", bm.offsetMs)
                        put("label", bm.label)
                    })
                }
                put("bookmarks", bmArray)
            }
            file.writeText(json.toString(2))
        } catch (_: Exception) { }
    }

    fun toggleFavorite(audioFilePath: String): Boolean {
        val meta = load(audioFilePath)
        val newFav = !meta.isFavorite
        save(audioFilePath, meta.copy(isFavorite = newFav))
        return newFav
    }

    fun addTag(audioFilePath: String, tag: String) {
        val meta = load(audioFilePath)
        if (!meta.tags.contains(tag)) {
            meta.tags.add(tag)
            save(audioFilePath, meta)
        }
    }

    fun removeTag(audioFilePath: String, tag: String) {
        val meta = load(audioFilePath)
        meta.tags.remove(tag)
        save(audioFilePath, meta)
    }

    fun setTranscript(audioFilePath: String, transcript: String) {
        val meta = load(audioFilePath)
        save(audioFilePath, meta.copy(transcript = transcript))
    }

    fun setBookmarks(audioFilePath: String, bookmarks: List<BookmarkEntry>) {
        val meta = load(audioFilePath)
        save(audioFilePath, meta.copy(bookmarks = bookmarks))
    }

    fun setEnhanced(audioFilePath: String) {
        val meta = load(audioFilePath)
        save(audioFilePath, meta.copy(isEnhanced = true))
    }

    fun delete(audioFilePath: String) {
        cache.remove(audioFilePath)
        metadataFile(audioFilePath).delete()
    }

    /** Get all unique tags used across all captures */
    fun getAllTags(context: Context): Set<String> {
        val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        val precordDir = File(musicDir, "Precord")
        if (!precordDir.exists()) return emptySet()

        val tags = mutableSetOf<String>()
        precordDir.listFiles { _, name -> name.endsWith(".meta.json") }?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                json.optJSONArray("tags")?.let { arr ->
                    for (i in 0 until arr.length()) tags.add(arr.getString(i))
                }
            } catch (_: Exception) { }
        }
        return tags
    }
}
