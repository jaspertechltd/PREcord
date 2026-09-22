## 2024-05-24 - RingBuffer allocation bottleneck during UI rendering
**Learning:** `RingBuffer.getAmplitudes()` is called frequently (e.g., up to 60fps) by the Compose UI to draw real-time waveforms. The previous implementation called `snapshot()`, forcing an O(N) heap allocation (up to 4 hours of audio = massive MBs) and a full array copy on every single frame just to read 100 samples. This is a severe anti-pattern for Android audio/UI bridges, leading to massive GC thrashing.
**Action:** Always inspect data extraction methods used by UI loops (like waveforms). Compute direct logical-to-physical index mappings (e.g., using modulo arithmetic for circular buffers) to read directly from the source array instead of allocating intermediate copies.

## 2024-05-24 - GitHub Actions Action Name
**Learning:** Appetizeio GitHub action is named `appetizeio/github-action-appetize`, not `appetizeio/appetize-github-action`.
**Action:** Be careful about GitHub action names when setting up CI.

## 2024-05-24 - GitHub Actions Action Version
**Learning:** `appetizeio/github-action-appetize` does not have a `v1` tag. Must use a specific version like `v1.1.0`.
**Action:** Always check available tags for GitHub Actions if a short version tag like `v1` fails.

## 2024-05-24 - GitHub Actions Appetize Parameters
**Learning:** `appetizeio/github-action-appetize` uses `apiToken` and `appFile` rather than `api-token` and `file-path`.
**Action:** Always check the action inputs definition or valid inputs error.

## 2024-05-25 - Avoid allocations inside Compose Canvas draw phase
**Learning:** In Compose, the `Canvas` drawing scope runs very frequently, potentially at 60-120 frames per second. Allocating memory inside this block (e.g., using `sliceArray` or implicit object creation like `IntProgression` via `.reversed()`) triggers rapid garbage collection, which leads to UI jank or dropped frames.
**Action:** When rendering data structures continuously (like waveforms), extract only primitive values using direct array indexing and manual iteration (e.g., `downTo`) to achieve zero-allocation draw loops.
## $(date +%Y-%m-%d) - Jetpack Compose N+1 I/O Blocking

**Learning:** Reading JSON metadata synchronously from disk using `CaptureMetadataStore.load` inside Jetpack Compose `LazyColumn` iterations causes severe N+1 performance jank due to repeated file I/O on the main thread for each item in a long list.
**Action:** Implemented an in-memory `ConcurrentHashMap` cache in `CaptureMetadataStore`. Ensured the cache also handles negative/empty states (like file not found) to prevent repeated failed reads, and implemented deep copying of data structures (`toMutableList()`, `toList()`) to prevent implicit cache mutations and subsequent missed Compose recompositions.

## $(date +%Y-%m-%d) - LruCache for File Data

**Learning:** When using in-memory caches to prevent file I/O blocking (e.g. metadata for Jetpack Compose lists), it is critical to use an eviction-based cache like `android.util.LruCache` rather than an unbounded `ConcurrentHashMap`. Unbounded caches for file data can easily lead to memory bloat and OutOfMemory errors in Android applications with large numbers of media files.
**Action:** Replaced `ConcurrentHashMap` with `LruCache` in `CaptureMetadataStore`.
