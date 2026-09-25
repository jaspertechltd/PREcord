## 2024-05-24 - Cached Metadata Store
**Learning:** Reading JSON metadata synchronously from disk (e.g., using CaptureMetadataStore.load) during Jetpack Compose LazyColumn rendering causes severe N+1 performance jank.
**Action:** Always use an in-memory cache (like ConcurrentHashMap) for frequently accessed metadata. Ensure negative/empty states are cached to avoid persistent I/O blocking, and always return deep copies of objects containing collections (using e.g., .toMutableList() or .toList()) to prevent implicit cache mutation and missed recompositions.
