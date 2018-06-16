package com.example.xyzreader.remote;

import android.content.Context;

import com.example.xyzreader.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class RemoteEndpointUtil {
    private RemoteEndpointUtil() {
    }

    public static JSONArray fetchJsonArray(final Context context) {
        String itemsJson;
        try {
            itemsJson = fetchPlainText(Config.BASE_URL);
        } catch (final IOException e) {
            Timber.e(e, context.getString(R.string.error_fetching_items_json));
            return null;
        }

        // Parse JSON
        try {
            final JSONTokener tokener = new JSONTokener(itemsJson);
            final Object val = tokener.nextValue();
            if (!(val instanceof JSONArray)) {
                throw new JSONException(context.getString(R.string.error_expected_json_array));
            }
            return (JSONArray) val;
        } catch (final JSONException e) {
            Timber.e(e, context.getString(R.string.error_parsing_items_json));
        }

        return null;
    }

    /*
     * Currently, the only URL parameter passed to this method is a single constant. The lint
     * warning regarding this is suppressed since moving the constant into this method seems
     * unnecessarily limiting.
     */
    private static String fetchPlainText(
            @SuppressWarnings("SameParameterValue") final URL url) throws IOException {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(url)
                .build();

        final Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
