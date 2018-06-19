package com.example.xyzreader.ui;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.date.ArticleDateUtils;

import timber.log.Timber;

/**
 * A fragment representing a single Article detail screen.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_ITEM_ID = "item_id";

    // Associated activity
    private Activity mActivity;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private ImageView mPhotoView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(final long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);

        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the arguments
        final Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalArgumentException(
                    getString(R.string.error_missing_details_fragment_args));
        }

        if (arguments.containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        final Toolbar toolbar = mRootView.findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.finish();
            }
        });

        mPhotoView = mRootView.findViewById(R.id.photo);

        // Set the share FAB click listener
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(mActivity)
                        .setType("text/plain")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        return mRootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Cache the associated activity
        mActivity = getActivity();

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        bodyView.setTypeface(Typeface.createFromAsset(
                getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            bylineView.setText(Html.fromHtml(
                    ArticleDateUtils.outputDateString(mActivity, mCursor)
                    + " by <font color='#ffffff'>" + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</font>"));

            bodyView.setText(Html.fromHtml(
                    mCursor.getString(ArticleLoader.Query.BODY).replaceAll(
                            "(\r\n|\n)", "<br />")));

            ImageLoaderHelper.getInstance(mActivity).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                            new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(final ImageLoader.ImageContainer imageContainer,
                                               final boolean b) {
                            final Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(final VolleyError volleyError) {
                            Timber.e(mActivity.getString(R.string.error_getting_article_image));
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(final int i, final Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(mActivity, mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<Cursor> cursorLoader, final Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Timber.e(mActivity.getString(R.string.error_reading_item_detail_cursor));
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
