package com.kagisomedia.audiostreamlibrary.player.spoledge.aacdecoder;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class IcyURLStreamHandler extends URLStreamHandler {
    public IcyURLStreamHandler() {
    }

    protected int getDefaultPort() {
        return 80;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return new IcyURLConnection(url);
    }
}