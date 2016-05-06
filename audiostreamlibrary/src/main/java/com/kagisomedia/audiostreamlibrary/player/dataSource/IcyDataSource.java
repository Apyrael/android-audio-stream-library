package com.kagisomedia.audiostreamlibrary.player.dataSource;


import android.util.Log;

import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.util.Predicate;
import com.kagisomedia.audiostreamlibrary.player.spoledge.aacdecoder.callback.IcyPlayerCallback;
import com.kagisomedia.audiostreamlibrary.player.spoledge.aacdecoder.callback.IcyStreamMetaDataCallback;
import com.kagisomedia.audiostreamlibrary.player.spoledge.aacdecoder.IcyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * A {@link DefaultHttpDataSource} that uses Android's {@link DefaultHttpDataSource}.
 */
public class IcyDataSource extends DefaultHttpDataSource {

    private static String TAG = "IcyDataSource";
    boolean metadataEnabled = true;
    private IcyStreamMetaDataCallback streamMetaDataCallback;

    IcyPlayerCallback playerCallback = new IcyPlayerCallback() {

        @Override
        public void playerMetadata(String key, String value) {
            Log.i(TAG, String.format(Locale.ENGLISH,"playerMetadata %s : %s", key, value));
            if (streamMetaDataCallback != null) {
                streamMetaDataCallback.onPlayerMetadata(key, value);
            }
        }
    };

    public IcyDataSource(String userAgent, Predicate<String> contentTypePredicate, IcyStreamMetaDataCallback streamMetaDataCallback) {
        super(userAgent, contentTypePredicate);
        this.streamMetaDataCallback = streamMetaDataCallback;
    }

    @Override
    protected HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        Log.i(TAG, String.format(Locale.ENGLISH,"makeConnection [%s-%d]", dataSpec.position, dataSpec.length));

        URL url = new URL(dataSpec.uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Icy-Metadata", "1");
        return connection;
    }

    /**
     * Gets the input stream from the connection.
     * Actually returns the underlying stream or IcyInputStream.
     */
    @Override
    protected InputStream getInputStream(HttpURLConnection conn) throws IOException {
        String smetaint = conn.getHeaderField("icy-metaint");
        InputStream ret = conn.getInputStream();

        if (!metadataEnabled) {
            Log.i(TAG, "Metadata not enabled");
        } else if (smetaint != null) {
            int period = -1;
            try {
                period = Integer.parseInt(smetaint);
            } catch (Exception e) {
                Log.e(TAG, String.format(Locale.ENGLISH,"The icy-metaint '[%s]' cannot be parsed", e, smetaint));
            }

            if (period > 0) {
                Log.i(TAG, String.format(Locale.ENGLISH,"The dynamic metainfo is sent every [%d] bytes", period));
                ret = new IcyInputStream(ret, period, playerCallback, null);
            }
        } else {
            Log.i(TAG, "This stream does not provide dynamic metainfo");
        }

        return ret;
    }
}
