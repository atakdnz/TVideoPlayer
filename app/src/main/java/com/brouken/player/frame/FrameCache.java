package com.brouken.player.frame;

import android.graphics.Bitmap;
import android.util.LruCache;

public class FrameCache {
    private final long maxMemoryBytes;
    private final int maxEntries;
    private final LruCache<FrameCacheKey, Bitmap> cache;

    public FrameCache(long maxMemoryBytes, int maxEntries) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.maxEntries = maxEntries;
        int maxKb = (int) Math.max(1, maxMemoryBytes / 1024L);
        cache = new LruCache<FrameCacheKey, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(FrameCacheKey key, Bitmap value) {
                return Math.max(1, value.getByteCount() / 1024);
            }
        };
    }

    public Bitmap get(FrameCacheKey key) {
        return cache.get(key);
    }

    public void put(FrameCacheKey key, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        if (bitmap.getByteCount() > maxMemoryBytes) {
            return;
        }
        cache.put(key, bitmap);
        trimEntryCount();
    }

    public void clear() {
        cache.evictAll();
    }

    private void trimEntryCount() {
        while (cache.snapshot().size() > maxEntries) {
            FrameCacheKey firstKey = cache.snapshot().keySet().iterator().next();
            cache.remove(firstKey);
        }
    }
}
