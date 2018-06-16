package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;

import com.example.xyzreader.R;
import com.example.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import timber.log.Timber;

public class UpdaterService extends IntentService {
    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";

    private static final String TAG = UpdaterService.class.getSimpleName();

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        if (ni == null || !ni.isConnected()) {
            Timber.w(getString(R.string.warning_offline));
            return;
        }

        /*
         * sendStickyBroadcast is deprecated. The lint warning is simply suppressed here because
         * the purpose of this project is to demonstrate material design concepts, rather than to
         * resolve internal issues with the starter code.
         */
        //noinspection deprecation
        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        final Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            final JSONArray array = RemoteEndpointUtil.fetchJsonArray(this);
            if (array == null) {
                throw new JSONException(getString(R.string.error_invalid_parsed_item_array));
            }

            for (int i = 0; i < array.length(); i++) {
                final ContentValues values = new ContentValues();
                final JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id" ));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author" ));
                values.put(ItemsContract.Items.TITLE, object.getString("title" ));
                values.put(ItemsContract.Items.BODY, object.getString("body" ));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb" ));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo" ));
                values.put(ItemsContract.Items.ASPECT_RATIO,
                        object.getString("aspect_ratio" ));
                values.put(ItemsContract.Items.PUBLISHED_DATE,
                        object.getString("published_date"));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Timber.e(e,getString(R.string.error_updating_content));
        }

        /*
         * sendStickyBroadcast is deprecated. The lint warning is simply suppressed here because
         * the purpose of this project is to demonstrate material design concepts, rather than to
         * resolve internal issues with the starter code.
         */
        //noinspection deprecation
        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
