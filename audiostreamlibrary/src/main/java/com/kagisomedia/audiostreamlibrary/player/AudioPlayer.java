package com.kagisomedia.audiostreamlibrary.player;

import android.media.MediaCodec;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper around {@link ExoPlayer}.
 *
 * Created by richard on 2016/04/05.
 */
public class AudioPlayer implements ExoPlayer.Listener, MediaCodecAudioTrackRenderer.EventListener {

    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Constructs the necessary components for playback.
         *
         * @param player The parent player.
         * @param callback The callback that will be invoked when the renderer has been built.
         */
        void buildRenderer(AudioPlayer player, RendererBuilderCallback callback);
    }

    /**
     * A callback invoked by a {@link RendererBuilder}.
     */
    public interface RendererBuilderCallback {

        /**
         * Invoked with the results from a {@link RendererBuilder}.
         *
         * @param renderer The audio track renderer that was built.
         */
        void onRenderer(TrackRenderer renderer);

        /**
         * Invoked if a {@link RendererBuilder} encounters an error.
         *
         * @param e Describes the error.
         */
        void onRendererError(Exception e);
    }

    /**
     * Invoked when an {@link AudioTrack} fails to initialize.
     *
     * @param e The corresponding exception.
     */
    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        Log.e(TAG, "audioTrackInitializationError", e);
    }

    /**
     * Invoked when an {@link AudioTrack} write fails.
     *
     * @param e The corresponding exception.
     */
    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        Log.e(TAG, "audioTrackWriteError", e);
    }

    /**
     * Invoked when an {@link AudioTrack} underrun occurs.
     *
     * @param bufferSize             The size of the {@link AudioTrack}'s buffer, in bytes.
     * @param bufferSizeMs           The size of the {@link AudioTrack}'s buffer, in milliseconds, if it is
     *                               configured for PCM output. -1 if it is configured for passthrough output, as the buffered
     *                               media can have a variable bitrate so the duration may be unknown.
     * @param elapsedSinceLastFeedMs The time since the {@link AudioTrack} was last fed data.
     */
    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        Log.d(TAG, String.format(Locale.ENGLISH, "audioTrackUnderrun bufferSize=[%d] bufferSizeMs=[%d] elapsedSinceLastFeedMs=[%d]", bufferSize, bufferSizeMs, elapsedSinceLastFeedMs));
    }

    /**
     * Invoked when a decoder fails to initialize.
     *
     * @param e The corresponding exception.
     */
    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        Log.e(TAG, "decoderInitializationError", e);
    }

    /**
     * Invoked when a decoder operation raises a CryptoException.
     *
     * @param e The corresponding exception.
     */
    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        Log.e(TAG, "cryptoError", e);
    }

    /**
     * Invoked when a decoder is successfully created.
     *
     * @param decoderName              The decoder that was configured and created.
     * @param elapsedRealtimeMs        {@code elapsedRealtime} timestamp of when the initialization
     *                                 finished.
     * @param initializationDurationMs Amount of time taken to initialize the decoder.
     */
    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
        Log.d(TAG, String.format(Locale.ENGLISH, "decoderInitialized decoderName=[%s] elapsedRealtimeMs=[%d] initializationDurationMs=[%d]", decoderName, elapsedRealtimeMs, initializationDurationMs));
    }

    /**
     * Invoked when the value returned from either {@link ExoPlayer#getPlayWhenReady()} or
     * {@link ExoPlayer#getPlaybackState()} changes.
     *
     * @param playWhenReady Whether playback will proceed when ready.
     * @param playbackState One of the {@code STATE} constants defined in the {@link ExoPlayer}
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, String.format(Locale.ENGLISH, "onPlayerStateChanged() playWhenReady=[%s] playbackState=[%d]", playWhenReady, playbackState));
        notifyPlayerStateIfChanged();
    }

    /**
     * Invoked when the current value of {@link ExoPlayer#getPlayWhenReady()} has been reflected
     * by the internal playback thread.
     * <p>
     * An invocation of this method will shortly follow any call to
     * {@link ExoPlayer#setPlayWhenReady(boolean)} that changes the state. If multiple calls are
     * made in rapid succession, then this method will be invoked only once, after the final state
     * has been reflected.
     */
    @Override
    public void onPlayWhenReadyCommitted() {
        // Do nothing.
    }

    /**
     * Invoked when an error occurs. The playback state will transition to
     * {@link ExoPlayer#STATE_IDLE} immediately after this method is invoked. The player instance
     * can still be used, and {@link ExoPlayer#release()} must still be called on the player should
     * it no longer be required.
     *
     * @param exception The error.
     */
    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        Log.e(TAG, "PlayerError", exception);
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(exception);
        }
    }

    /**
     * A listener for core events.
     */
    public interface Listener {

        /**
         * Invoked when the state of the player has changed.
         * @param playWhenReady The current playWhenReady state.
         * @param playbackState The new state.
         */
        void onStateChanged(boolean playWhenReady, int playbackState);

        /**
         * Invoked when an error occurs.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    private static final String TAG = "AudioPlayer";

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    private final ExoPlayer player;
    private final RendererBuilder rendererBuilder;
    private final CopyOnWriteArrayList<Listener> listeners;

    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private InternalRendererBuilderCallback builderCallback;

    public AudioPlayer(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(1, 1000, 5000);
        player.addListener(this);
        listeners = new CopyOnWriteArrayList<>();
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
    }

    /**
     * Adds a listener that will be notified of state changes and errors.
     * @param listener The listener that will be added.
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the listener.
     * @param listener The listener that will be removed.
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Prepares the AudioPlayer
     */
    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        if (builderCallback != null) {
            builderCallback.cancel();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        builderCallback = new InternalRendererBuilderCallback();
        rendererBuilder.buildRenderer(this, builderCallback);
    }

    public void release() {
        if (builderCallback != null) {
            builderCallback.cancel();
            builderCallback = null;
        }
        player.release();
    }

    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param renderer The audio track renderer that was built.
     */
    private void onRenderer(TrackRenderer renderer) {
        builderCallback = null;
        player.prepare(renderer);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    private void onRendererError(Exception e) {
        for (Listener listener : listeners) {
            listener.onError(e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        notifyPlayerStateIfChanged();
    }

    /**
     * Sets whether playback should proceed when {@link #getPlaybackState()} == {@link ExoPlayer#STATE_READY}.
     * If the player is already in this state, then this method can be used to pause and resume
     * playback.
     *
     * @param playWhenReady Whether playback should proceed when ready.
     */
    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    /**
     * Whether playback will proceed when {@link #getPlaybackState()} == {@link ExoPlayer#STATE_READY}.
     *
     * @return Whether playback will proceed when ready.
     */
    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    /**
     * Gets the current state of the playback.
     *
     * @return The current state of the player.
     */
    public int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return ExoPlayer.STATE_PREPARING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT && rendererBuildingState == RENDERER_BUILDING_STATE_IDLE) {
            // This is an edge case where the renderer is built, but is still being passed to the
            // player's playback thread.
            return ExoPlayer.STATE_PREPARING;
        }
        return playerState;
    }

    /**
     * Stops the player.
     */
    public void stop() {
        player.setPlayWhenReady(false);
        player.stop();
    }

    /**
     * If the state of the player has changed then the listeners should be notified.
     */
    private void notifyPlayerStateIfChanged() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
            for (Listener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);
            }
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }

    /**
     * Internal implementation of the RendererBuilderCallback.
     */
    private class InternalRendererBuilderCallback implements RendererBuilderCallback {

        private boolean canceled;

        /**
         * Cancels the callback if it has not yet complete.
         */
        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRenderer(TrackRenderer renderer) {
            if (!canceled) {
                AudioPlayer.this.onRenderer(renderer);
            }
        }

        @Override
        public void onRendererError(Exception e) {
            if (!canceled) {
                AudioPlayer.this.onRendererError(e);
            }
        }

    }
}