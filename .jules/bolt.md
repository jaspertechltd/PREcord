## 2024-05-24 - RingBuffer allocation bottleneck during UI rendering
**Learning:** `RingBuffer.getAmplitudes()` is called frequently (e.g., up to 60fps) by the Compose UI to draw real-time waveforms. The previous implementation called `snapshot()`, forcing an O(N) heap allocation (up to 4 hours of audio = massive MBs) and a full array copy on every single frame just to read 100 samples. This is a severe anti-pattern for Android audio/UI bridges, leading to massive GC thrashing.
**Action:** Always inspect data extraction methods used by UI loops (like waveforms). Compute direct logical-to-physical index mappings (e.g., using modulo arithmetic for circular buffers) to read directly from the source array instead of allocating intermediate copies.

## 2024-05-24 - GitHub Actions Action Name
**Learning:** Appetizeio GitHub action is named `appetizeio/github-action-appetize`, not `appetizeio/appetize-github-action`.
**Action:** Be careful about GitHub action names when setting up CI.
