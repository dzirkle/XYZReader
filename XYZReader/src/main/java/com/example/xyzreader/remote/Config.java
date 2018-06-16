package com.example.xyzreader.remote;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

class Config {
    public static final URL BASE_URL;

    static {
        final String BASE_URL_STRING = "https://go.udacity.com/xyz-reader-json";
        URL url;
        try {
            url = new URL(BASE_URL_STRING);
        } catch (final MalformedURLException mue) {
            Timber.e(mue, BASE_URL_STRING);
            throw new ExceptionInInitializerError(mue);
        }

        BASE_URL = url;
    }
}
