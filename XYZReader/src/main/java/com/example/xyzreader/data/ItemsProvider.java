
package com.example.xyzreader.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.xyzreader.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemsProvider extends ContentProvider {
	private SQLiteOpenHelper mOpenHelper;

	interface Tables {
		String ITEMS = "items";
	}

	private static final int ITEMS = 0;
	private static final int ITEMS__ID = 1;

	private static final UriMatcher sUriMatcher = buildUriMatcher();

	private Context mContext;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = ItemsContract.CONTENT_AUTHORITY;
		matcher.addURI(authority, ItemsContract.ITEMS_SEGMENT, ITEMS);
		matcher.addURI(authority, ItemsContract.ITEMS_SEGMENT + "/#", ITEMS__ID);
		return matcher;
	}

	@Override
	public boolean onCreate() {
	    mContext = Objects.requireNonNull(getContext());
        mOpenHelper = new ItemsDatabase(mContext);
		return true;
	}

	@Override
	public String getType(@NonNull final Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case ITEMS:
				return ItemsContract.Items.CONTENT_TYPE;
			case ITEMS__ID:
				return ItemsContract.Items.CONTENT_ITEM_TYPE;
			default:
				throw new UnsupportedOperationException(
				        mContext.getString(R.string.error_unknown_uri) + uri);
		}
	}

	@Override
	public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		final SelectionBuilder builder = buildSelection(uri);
		Cursor cursor = builder.where(selection, selectionArgs).query(db, projection, sortOrder);
        if (cursor != null) {
            cursor.setNotificationUri(
            		Objects.requireNonNull(getContext()).getContentResolver(), uri);
        }
        return cursor;
	}

	@Override
	public Uri insert(@NonNull final Uri uri, final ContentValues values) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case ITEMS: {
				final long _id = db.insertOrThrow(Tables.ITEMS, null, values);
                Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
				return ItemsContract.Items.buildItemUri(_id);
			}
			default: {
				throw new UnsupportedOperationException(
				        mContext.getString(R.string.error_unknown_uri) + uri);
			}
		}
	}

	@Override
	public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
                      final String[] selectionArgs) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSelection(uri);
        mContext.getContentResolver().notifyChange(uri, null);
		return builder.where(selection, selectionArgs).update(db, values);
	}

	@Override
	public int delete(@NonNull final Uri uri, final String selection,
                      final String[] selectionArgs) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSelection(uri);
        mContext.getContentResolver().notifyChange(uri, null);
		return builder.where(selection, selectionArgs).delete(db);
	}

	private SelectionBuilder buildSelection(final Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder(mContext);
		final int match = sUriMatcher.match(uri);
		return buildSelection(uri, match, builder);
	}

	private SelectionBuilder buildSelection(final Uri uri, final int match,
                                            final SelectionBuilder builder) {
		final List<String> paths = uri.getPathSegments();
		switch (match) {
			case ITEMS: {
				return builder.table(Tables.ITEMS);
			}
			case ITEMS__ID: {
				final String _id = paths.get(1);
				return builder.table(Tables.ITEMS).where(ItemsContract.Items._ID + "=?", _id);
			}
			default: {
				throw new UnsupportedOperationException(
				        mContext.getString(R.string.error_unknown_uri) + uri);
			}
		}
	}

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @NonNull
	public ContentProviderResult[] applyBatch(
	        @NonNull final ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }
}
