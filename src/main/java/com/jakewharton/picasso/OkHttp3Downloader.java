package com.jakewharton.picasso;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.NetworkPolicy;
import java.io.File;
import java.io.IOException;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

public final class OkHttp3Downloader implements Downloader {
  private static final String PICASSO_CACHE = "picasso-cache";
  private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
  private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

  private static File defaultCacheDir(Context context) {
    File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
    if (!cache.exists()) {
      //noinspection ResultOfMethodCallIgnored
      cache.mkdirs();
    }
    return cache;
  }

  private static long calculateDiskCacheSize(File dir) {
    long size = MIN_DISK_CACHE_SIZE;

    try {
      StatFs statFs = new StatFs(dir.getAbsolutePath());

      long available;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
      } else {
        available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
      }

      // Target 2% of the total space.
      size = available / 50;
    } catch (IllegalArgumentException ignored) {
    }

    // Bound inside min/max size for disk cache.
    return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
  }

  /**
   * Creates a {@link Cache} that would have otherwise been created by calling
   * {@link #OkHttp3Downloader(Context)}. This allows you to build your own {@link OkHttpClient}
   * while still getting the default disk cache.
   */
  public static Cache createDefaultCache(Context context) {
    File dir = defaultCacheDir(context);
    return new Cache(dir, calculateDiskCacheSize(dir));
  }

  private static OkHttpClient createOkHttpClient(File cacheDir, long maxSize) {
    return new OkHttpClient.Builder()
        .cache(new Cache(cacheDir, maxSize))
        .build();
  }

  private final Call.Factory client;
  private final Cache cache;

  /**
   * Create new downloader that uses OkHttp. This will install an image cache into your application
   * cache directory.
   */
  public OkHttp3Downloader(Context context) {
    this(defaultCacheDir(context));
  }

  /**
   * Create new downloader that uses OkHttp. This will install an image cache into the specified
   * directory.
   *
   * @param cacheDir The directory in which the cache should be stored
   */
  public OkHttp3Downloader(File cacheDir) {
    this(cacheDir, calculateDiskCacheSize(cacheDir));
  }

  /**
   * Create new downloader that uses OkHttp. This will install an image cache into your application
   * cache directory.
   *
   * @param maxSize The size limit for the cache.
   */
  public OkHttp3Downloader(final Context context, final long maxSize) {
    this(defaultCacheDir(context), maxSize);
  }

  /**
   * Create new downloader that uses OkHttp. This will install an image cache into the specified
   * directory.
   *
   * @param cacheDir The directory in which the cache should be stored
   * @param maxSize The size limit for the cache.
   */
  public OkHttp3Downloader(File cacheDir, long maxSize) {
    this(createOkHttpClient(cacheDir, maxSize));
  }

  public OkHttp3Downloader(OkHttpClient client) {
    this.client = client;
    this.cache = client.cache();
  }

  public OkHttp3Downloader(Call.Factory client) {
    this.client = client;
    this.cache = null;
  }

  @Override public Response load(Uri uri, int networkPolicy) throws IOException {
    CacheControl cacheControl = null;
    if (networkPolicy != 0) {
      if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
        cacheControl = CacheControl.FORCE_CACHE;
      } else {
        CacheControl.Builder builder = new CacheControl.Builder();
        if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
          builder.noCache();
        }
        if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
          builder.noStore();
        }
        cacheControl = builder.build();
      }
    }

    Request.Builder builder = new Request.Builder().url(uri.toString());
    if (cacheControl != null) {
      builder.cacheControl(cacheControl);
    }

    okhttp3.Response response = client.newCall(builder.build()).execute();
    int responseCode = response.code();
    if (responseCode >= 300) {
      response.body().close();
      throw new ResponseException(responseCode + " " + response.message(), networkPolicy,
          responseCode);
    }

    boolean fromCache = response.cacheResponse() != null;

    ResponseBody responseBody = response.body();
    return new Response(responseBody.byteStream(), fromCache, responseBody.contentLength());
  }

  @Override public void shutdown() {
    if (cache != null) {
      try {
        cache.close();
      } catch (IOException ignored) {
      }
    }
  }
}
