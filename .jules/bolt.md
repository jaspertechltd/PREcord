## 2024-09-10 - Ring Buffer Snapshot Anti-Pattern
**Learning:** Frequent calls to a ring buffer's snapshot mechanism in a hot loop (like real-time UI updates for waveforms) can cause massive memory allocations and garbage collection pauses, leading to frame drops or OutOfMemoryErrors, especially with large buffer sizes (e.g., 4 hours of audio = 1.2GB).
**Action:** When deriving sparse metrics (like amplitudes) from a large circular buffer, calculate logical indices and map them directly to the underlying physical array using modulo arithmetic to avoid array copying and allocations entirely.
