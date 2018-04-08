package com.example.xyzreader.ui;

import android.app.Activity;
import android.util.DisplayMetrics;

public class UiUtils {
    /**
     * Get the number of article list card columns to display
     *
     * @return the number of article list card columns to display
     */
    static int getArticleListColumns(final Activity activity) {
        // Target card width in dp
        final int TARGET_CARD_WIDTH = 350;

        return getScreenWidthDp(activity) / TARGET_CARD_WIDTH;
    }

    /**
     * Get the screen width in DP
     *
     * @param activity {@link Activity} associated with caller
     *
     * @return screen width in DP
     */
    private static int getScreenWidthDp(final Activity activity) {
        // Get the display metrics
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Return the screen width in DP
        return (int) (displayMetrics.widthPixels / displayMetrics.density);
    }
}
