package com.example.xyzreader.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.date.ArticleDateUtils;

import timber.log.Timber;

/**
 * todo verify
 * A fragment representing a single Article detail screen. This fragment is either contained in a
 * {@link ArticleListActivity} in two-pane mode (on tablets) or a {@link ArticleDetailActivity} on
 * handsets.
 *
 * The shared element transition approach is based on Alex Lockwood's work here:
 *     https://github.com/alexjlockwood/adp-activity-transitions
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    // todo document
    private static final String ARG_ARTICLE_POSITION =
            "com.example.xyzreader.ui.arg_article_position";
    private static final String ARG_STARTING_ARTICLE_POSITION =
            "com.example.xyzreader.ui.arg_starting_article_position";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ColorDrawable mStatusBarColorDrawable;

    // todo remove when up button is fixed
    private int mScrollY;

    // todo document
    private int mStartingPosition;
    private int mPosition;
    private boolean mIsTransitioning;
    private long mBackgroundImageFadeMillis;

    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);

        // todo document
        arguments.putInt(ARG_ARTICLE_POSITION, position);
        arguments.putInt(ARG_STARTING_ARTICLE_POSITION, startingPosition);

        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        // todo document
        // todo handle missing keys, exceptions, etc.
        mStartingPosition = getArguments().getInt(ARG_STARTING_ARTICLE_POSITION);
        mPosition = getArguments().getInt(ARG_ARTICLE_POSITION);
        mIsTransitioning = savedInstanceState == null && mStartingPosition == mPosition;
        // todo clean up
//        mBackgroundImageFadeMillis = getResources().getInteger(
//                R.integer.fragment_details_background_image_fade_millis);
        mBackgroundImageFadeMillis = 1000;

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = mRootView.findViewById(R.id.photo);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        // Set the up button click listener
        final ImageButton mUpButton = mRootView.findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AppCompatActivity) getActivity()).supportFinishAfterTransition();
            }
        });

        // Set the share FAB click listener
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        // todo this stuff was in Lockwood's code
//        if (mIsTransitioning) {
//            albumImageRequest.noFade();
//            backgroundImageRequest.noFade();
//            backgroundImage.setAlpha(0f);
//            getActivity().getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
//                @Override
//                public void onTransitionEnd(Transition transition) {
//                    backgroundImage.animate().setDuration(mBackgroundImageFadeMillis).alpha(1f);
//                }
//            });
//        }

        bindViews();

        return mRootView;
    }

    // todo document
    public void startPostponedEnterTransition() {
        if (mPosition == mStartingPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    // todo document
    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    ImageView getArticleImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    // todo document
    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            bylineView.setText(Html.fromHtml(ArticleDateUtils.outputDateString(mCursor)
                    + " by <font color='#ffffff'>" + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</font>"));

            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                final Palette p = new Palette.Builder(bitmap)
                                        .maximumColorCount(12)
                                        .generate();
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);

                                // todo document
                                final String transitionName = mCursor.getString(ArticleLoader.Query.TITLE);
                                mPhotoView.setTransitionName(transitionName);
                                // todo remove
                                Timber.d("dzdbg " + ": <" + transitionName + ">");

                                // Set up a pre-draw listener to start the shared element transition as the view is drawn
                                final View sharedElementTransitionView = mRootView.findViewById(R.id.photo);
                                sharedElementTransitionView.getViewTreeObserver()
                                        .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                                            @Override
                                            public boolean onPreDraw() {
                                                sharedElementTransitionView.getViewTreeObserver().removeOnPreDrawListener(this);
                                                startPostponedEnterTransition();
                                                return true;
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Timber.e("Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }
}
