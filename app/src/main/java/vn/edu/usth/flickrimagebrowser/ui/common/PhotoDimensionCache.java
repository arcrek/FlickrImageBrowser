package vn.edu.usth.flickrimagebrowser.ui.common;

import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fast, non-blocking in-memory cache storing resolved photo aspect ratios (width / height)
 * keyed by thumbnail URL to avoid changed-source pollution.
 * All aspect ratios are populated asynchronously from DTOs or Glide decoders;
 * performs zero disk or network I/O on the main thread.
 */
public final class PhotoDimensionCache {

    private static final int MAX_CACHE_ENTRIES = 500;
    private static final LruCache<String, Float> RATIO_CACHE = new LruCache<>(MAX_CACHE_ENTRIES);

    private PhotoDimensionCache() {
    }

    @Nullable
    public static Float getAspectRatio(@Nullable String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            return null;
        }
        return RATIO_CACHE.get(thumbnailUrl);
    }

    public static void putAspectRatio(@Nullable String thumbnailUrl, float aspectRatio) {
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty() && aspectRatio > 0f && Float.isFinite(aspectRatio)) {
            RATIO_CACHE.put(thumbnailUrl, aspectRatio);
        }
    }

    public static void clear() {
        RATIO_CACHE.evictAll();
    }
}
