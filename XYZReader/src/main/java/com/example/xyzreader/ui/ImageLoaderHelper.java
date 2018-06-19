package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Volley image loader helper
 *
 * This code is based on:
 *     https://developer.android.com/training/volley/requestqueue
 */
public class ImageLoaderHelper {
    @SuppressLint("StaticFieldLeak")
    private static ImageLoaderHelper sInstance;

    private RequestQueue mRequestQueue;
    private final ImageLoader mImageLoader;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private ImageLoaderHelper(final Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {

            private final int LRU_CACHE_MAX_SIZE = 20;

            private final LruCache<String, Bitmap> cache = new LruCache<>(LRU_CACHE_MAX_SIZE);

            @Override
            public Bitmap getBitmap(final String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(final String url, final Bitmap value) {
                cache.put(url, value);
            }
        });
    }

    public static synchronized ImageLoaderHelper getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoaderHelper(context);
        }

        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    /*
     * Though this method is unused in the app currently, it is a reasonable part of this class's
     * interface. Thus, the method is retained and the "unused" lint warning is suppressed.
     */
    @SuppressWarnings("unused")
    public <T> void addToRequestQueue(final Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
