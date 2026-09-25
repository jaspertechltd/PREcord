1. **Implement in-memory cache in CaptureMetadataStore**
   - Add a `ConcurrentHashMap<String, CaptureMetadata>` to cache metadata.
   - Update `load()` to read from the cache. On a cache miss, read from the disk, store in cache, and return. To ensure Jetpack Compose recomposes correctly and to prevent implicit mutation, it will return a deep copy (copying the collections).
   - Update `save()` to update the cache and then save to disk.
   - Update `delete()` to remove from the cache.
   - Update `getAllTags()` to potentially use the cache for loaded files? Actually, it reads all files. The current implementation is okay, or we can leave it as is if it's not called often, or update it. The main bottleneck is `load()` inside LazyColumn.

2. **Add performance comment**
   - Add a comment explaining the cache, the deep copy requirement, and the N+1 I/O issue it solves.

3. **Complete pre-commit steps**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

4. **Submit PR**
   - Create PR titled "⚡ Bolt: [performance improvement]" with What, Why, Impact, and Measurement.
