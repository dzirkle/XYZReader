package com.example.xyzreader.date;

import android.database.Cursor;
import android.text.format.DateUtils;

import com.example.xyzreader.data.ArticleLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import timber.log.Timber;

public class ArticleDateUtils {
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());

    // Locale-specific date output format
    private static final DateFormat outputFormat = DateFormat.getDateInstance();

    // Most time functions can only handle 1902 - 2037
    private static final GregorianCalendar START_OF_EPOCH =
            new GregorianCalendar(2,1,1);

    // todo document
    public static String outputDateString(final Cursor cursor) {
        final Date publishedDate = ArticleDateUtils.parsePublishedDate(cursor);

        if (!publishedDate.before(ArticleDateUtils.START_OF_EPOCH.getTime())) {
            return DateUtils.getRelativeTimeSpanString(
                    publishedDate.getTime(), System.currentTimeMillis(),
                    DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();
        } else {
            return outputFormat.format(publishedDate);
        }
    }

    public static Date parsePublishedDate(final Cursor cursor) {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex.getMessage());
            Timber.i("passing today's date");
            return new Date();
        }
    }
}
