package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.xyzreader.BuildConfig;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 *
 * The shared element transition approach is based on Alex Lockwood's work here:
 *     https://github.com/alexjlockwood/adp-activity-transitions
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, ArticleAdapter.ArticleClickListener {
    // todo document
    static final String EXTRA_STARTING_ARTICLE_POSITION =
            "com.example.xyzreader.ui.extra_starting_article_position";
    static final String EXTRA_CURRENT_ARTICLE_POSITION =
            "com.example.xyzreader.ui.extra_current_article_position";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private ArticleAdapter mArticleAdapter;

    // todo document
    private Bundle mTmpReenterState;

    // todo document
    private boolean mIsDetailsActivityStarted;

    private boolean mIsRefreshing = false;

    // todo document
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                Timber.d("mTmpReenterState != null");

                int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
                int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
                if (startingPosition != currentPosition) {
                    // If startingPosition != currentPosition the user must have swiped to a
                    // different page in the DetailsActivity. We must update the shared element
                    // so that the correct one falls into place.
//                    String newTransitionName = ALBUM_NAMES[currentPosition];
                    final ArticleAdapter.ViewHolder viewHolder = (ArticleAdapter.ViewHolder)
                            mRecyclerView.findViewHolderForAdapterPosition(currentPosition);
                    String newTransitionName = viewHolder.thumbnailView.getTransitionName();
                    Timber.d("New transition name: " + newTransitionName);

                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                mTmpReenterState = null;
            } else {
                Timber.d("mTmpReenterState == null");

                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        // Set up Timber. This app won't be released, so only debug build configs are supported.
        if (BuildConfig.DEBUG) {
            // Use the default Timber DebugTree for debug builds
            Timber.plant(new Timber.DebugTree() {
                // Override createStackElementTag in order to add the line number to the tag
                @Override
                protected String createStackElementTag(@NonNull final StackTraceElement element) {
                    // Add the line number to the default tag
                    return super.createStackElementTag(element) + ":" + element.getLineNumber();
                }
            });
        }

        // todo document
        setExitSharedElementCallback(mCallback);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        // Get the recycler view
        mRecyclerView = findViewById(R.id.recycler_view);

        // Create and configure the article adapter, then set it on the recycler view
        mArticleAdapter = new ArticleAdapter(this, null, this);
        mArticleAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mArticleAdapter);

        // Create and configure the layout manager, then set it on the recycler view
        final int columnCount = UiUtils.getArticleListColumns(this);
        mLayoutManager = new GridLayoutManager(this, columnCount);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Create the the item spacing decoration and set it on the recycler view
        final GridItemSpacingDecoration itemDecoration =
                new GridItemSpacingDecoration(this, R.dimen.grid_item_spacing);
        mRecyclerView.addItemDecoration(itemDecoration);

        // Initiate the data load
        getSupportLoaderManager().initLoader(0, null, this);

        // Start the updater service if this is the initial activity creation
        if (savedInstanceState == null) {
            startUpdaterService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    // todo document
    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    // todo document
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);

        Timber.d("onActivityReenter");

        mTmpReenterState = new Bundle(data.getExtras());
        final int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
        final int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);

        Timber.d("startingPosition: " + startingPosition);
        Timber.d("currentPosition: " + currentPosition);

        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
            Timber.d("scrolled to position: " + currentPosition);
        }

        postponeEnterTransition();
        Timber.d("postponed enter transition");


        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                Timber.d("started the postponed enter transition");
                return true;
            }
        });
    }

    private void startUpdaterService() {
        startService(new Intent(this, UpdaterService.class));
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Refresh the adapter data with the just-loaded cursor
        mArticleAdapter.refreshData(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    /**
     * Handle article list item clicks
     *
     * @param position       clicked item adapter position
     * @param transitionView shared element transition view
     */
    @Override
    public void onArticleClick(final int position, final View transitionView) {
        // Create an intent to launch the details activity
        final Intent intent = new Intent(Intent.ACTION_VIEW,
                ItemsContract.Items.buildItemUri(mArticleAdapter.getItemId(position)));
        intent.putExtra(EXTRA_STARTING_ARTICLE_POSITION, position);

        // todo document
        // todo is the mIsDetailsActivityStarted guard needed?
        if (!mIsDetailsActivityStarted) {
            mIsDetailsActivityStarted = true;

            // Set up a shared element transition for the photo if the build version >= 21.
            // Otherwise, simply start the details activity without the transition.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(
                        ArticleListActivity.this, transitionView,
                        transitionView.getTransitionName()).toBundle();
                startActivity(intent, bundle);
            } else {
                startActivity(intent);
            }
        }
    }
}
