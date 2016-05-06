package com.kagisomedia.audiostreamlibrary.player.spoledge.aacdecoder.callback;

/**
 * Callback that will be invoked when new meta data is retrieved from the stream.
 *
 * Created by richard on 2016/04/06.
 */
public interface IcyStreamMetaDataCallback {

    /**
     * This method is called when the player receives a metadata information.
     * It can be either before starting the player (from HTTP header - all header pairs)
     * or during playback (metadata frame info).
     * <p/>
     * The list of available keys is not part of this project -
     * it is depending on the server implementation.
     *
     * @param key   the metadata key - e.g. from HTTP header: "icy-genre", "icy-url", "content-type",..
     *              or from the dynamic metadata frame: e.g. "StreamTitle" or "StreamUrl"
     * @param value the metadata value
     */
    void onPlayerMetadata(String key, String value);
}
