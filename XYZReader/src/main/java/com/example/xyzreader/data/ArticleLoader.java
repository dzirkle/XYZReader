package com.example.xyzreader.data;

import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.net.Uri;

/**
 * Helper for loading a list of articles or a single article.
 */
public class ArticleLoader extends CursorLoader {
    public static ArticleLoader newAllArticlesInstance(final Context context) {
        return new ArticleLoader(context, ItemsContract.Items.buildDirUri());
    }

    public static ArticleLoader newInstanceForItemId(final Context context, final long itemId) {
        return new ArticleLoader(context, ItemsContract.Items.buildItemUri(itemId));
    }

    private ArticleLoader(final Context context, final Uri uri) {
        super(context, uri, Query.PROJECTION, null, null,
                ItemsContract.Items.DEFAULT_SORT);
    }

    public interface Query {
        String[] PROJECTION = {
                ItemsContract.Items._ID,
                ItemsContract.Items.TITLE,
                ItemsContract.Items.PUBLISHED_DATE,
                ItemsContract.Items.AUTHOR,
                ItemsContract.Items.THUMB_URL,
                ItemsContract.Items.PHOTO_URL,
                ItemsContract.Items.ASPECT_RATIO,
                ItemsContract.Items.BODY,
        };

        int _ID = 0;
        int TITLE = 1;
        int PUBLISHED_DATE = 2;
        int AUTHOR = 3;
        int THUMB_URL = 4;
        int PHOTO_URL = 5;

        /*
         * This field is currently unused, but is retained since it's part of the contract. The
         * "unused" warning is suppressed.
         */
        @SuppressWarnings("unused")
        int ASPECT_RATIO = 6;

        int BODY = 7;
    }
}
