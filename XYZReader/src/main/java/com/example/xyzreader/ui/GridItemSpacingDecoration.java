package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Provide grid item spacing
 *
 * This class is based on code found here:
 *     https://gist.github.com/yqritc/ccca77dc42f2364777e1
 */
class GridItemSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int mItemSpacing;

    /*
     * This constructor is only used internally by the app currently, but is a reasonable part of
     * the class interface. Thus, the "weaker access" lint warning is suppressed.
     */
    @SuppressWarnings("WeakerAccess")
    public GridItemSpacingDecoration(int itemSpacing) {
        mItemSpacing = itemSpacing;
    }

    @SuppressWarnings("WeakerAccess")
    public GridItemSpacingDecoration(final Context context, final int itemSpacingId) {
        this(context.getResources().getDimensionPixelOffset(itemSpacingId));
    }

    @Override
    public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent,
                               final RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(mItemSpacing, mItemSpacing, mItemSpacing, mItemSpacing);
    }
}
