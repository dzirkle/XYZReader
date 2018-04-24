package com.example.xyzreader.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.date.ArticleDateUtils;

import timber.log.Timber;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    // Context
    private Context mContext;

    // Cursor
    private Cursor mCursor;

    // Article click listener
    private ArticleClickListener mArticleClickListener;

    public ArticleAdapter(final Context context, final Cursor cursor,
                          final ArticleClickListener articleClickListener) {
        mContext = context;
        mCursor = cursor;
        mArticleClickListener =  articleClickListener;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View view = inflater.inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mCursor, position);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    // todo document
    public void refreshData(final Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    /**
     * Article click listener interface
     */
    public interface ArticleClickListener {
        void onArticleClick(final int position, final View transitionView);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public NetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        private int mPosition;

        public ViewHolder(View view) {
            super(view);

            // Set the click listener on the view
            view.setOnClickListener(this);

            // Cache the views
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }

        // todo document
        public void bind(final Cursor cursor, final int position) {
            // ...
            mPosition = position;
            cursor.moveToPosition(position);

            titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));

            subtitleView.setText(Html.fromHtml(ArticleDateUtils.outputDateString(cursor)
                    + "<br/>" + " by " + cursor.getString(ArticleLoader.Query.AUTHOR)));

            thumbnailView.setImageUrl(
                    cursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(mContext).getImageLoader());

            // todo document
            final String transitionName = mCursor.getString(ArticleLoader.Query.TITLE);
            thumbnailView.setTransitionName(transitionName);
            thumbnailView.setTag(transitionName);
            // todo remove
            Timber.d("position " + position + ": <" + transitionName + ">");
            // ...
            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            // todo need to flesh out
            mArticleClickListener.onArticleClick(mPosition, thumbnailView);
        }
    }
}

