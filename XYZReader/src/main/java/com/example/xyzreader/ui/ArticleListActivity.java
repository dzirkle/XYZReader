package com.example.xyzreader.ui;

import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.example.xyzreader.BuildConfig;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.io.IOException;

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

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;

    private boolean mIsRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        // Disable the CollapsingToolbarLayout title so the title may be set on the Toolbar
        final CollapsingToolbarLayout collapsingToolbarLayout
                = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitleEnabled(false);

        // Set the toolbar title
        final Toolbar toolbar = findViewById(R.id.toolbar_list);
        toolbar.setTitle(R.string.app_name);

        // Set the toolbar as the action bar
        setSupportActionBar(toolbar);

        // Whitelist this app's PID for logcat
        // todo remove when debugged
        try {
            int pid = android.os.Process.myPid();
            String whiteList = "logcat -P '" + pid + "'";
            Runtime.getRuntime().exec(whiteList).waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

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

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Get the recycler view
        mRecyclerView = findViewById(R.id.recycler_view);

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
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
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

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        // Create and configure the article adapter, then set it on the recycler view
        final ArticleAdapter articleAdapter = new ArticleAdapter(this, cursor, this);
        articleAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(articleAdapter);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
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
                ItemsContract.Items.buildItemUri(mRecyclerView.getAdapter().getItemId(position)));
        startActivity(intent);
    }

}
