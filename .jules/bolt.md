## 2024-09-10 - Ring Buffer Snapshot Anti-Pattern
**Learning:** Frequent calls to a ring buffer's snapshot mechanism in a hot loop (like real-time UI updates for waveforms) can cause massive memory allocations and garbage collection pauses, leading to frame drops or OutOfMemoryErrors, especially with large buffer sizes (e.g., 4 hours of audio = 1.2GB). `RingBuffer.getAmplitudes()` is called frequently (e.g., up to 60fps) by the Compose UI to draw real-time waveforms.
**Action:** When deriving sparse metrics (like amplitudes) from a large circular buffer, calculate logical indices and map them directly to the underlying physical array using modulo arithmetic to avoid array copying and heap allocations entirely.

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
