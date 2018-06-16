package com.example.xyzreader.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.date.ArticleDateUtils;

import timber.log.Timber;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    // Context
    private final Context mContext;

    // Cursor
    private final Cursor mCursor;

    // Article click listener
    private final ArticleClickListener mArticleClickListener;

    @SuppressWarnings("WeakerAccess")
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
        return mCursor != null ? mCursor.getCount() : 0;
    }

    /**
     * Article click listener interface
     */
    public interface ArticleClickListener {
        void onArticleClick(final int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView thumbnailView;
        final TextView titleView;
        final TextView subtitleView;
        private int mPosition;

        ViewHolder(View view) {
            super(view);

            // Set the click listener on the view
            view.setOnClickListener(this);

            // Cache the views
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }

        void bind(final Cursor cursor, final int position) {
            mPosition = position;
            cursor.moveToPosition(position);

            titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));

            subtitleView.setText(Html.fromHtml(
                    ArticleDateUtils.outputDateString(mContext, cursor)
                    + "<br/>" + " by " + cursor.getString(ArticleLoader.Query.AUTHOR)));

            // Volley ImageRequest response listener
            Response.Listener<Bitmap> imageListener = new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    //This call back method is executed in the UI-Thread, when the loading is finished
                    thumbnailView.setImageBitmap(response); //example
                }
            };

            // Volley ImageRequest error listener
            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Timber.e("Error retrieving article thumbnail image");
                }
            };

            // Create the image request
            ImageRequest getImageRequest = new ImageRequest(
                    cursor.getString(ArticleLoader.Query.THUMB_URL),
                    imageListener, 0, 0, null, errorListener);

            // Add the image request to the queue
            ImageLoaderHelper.getInstance(
                    mContext.getApplicationContext()).getRequestQueue().add(getImageRequest);

            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            mArticleClickListener.onArticleClick(mPosition);
        }
    }
}

