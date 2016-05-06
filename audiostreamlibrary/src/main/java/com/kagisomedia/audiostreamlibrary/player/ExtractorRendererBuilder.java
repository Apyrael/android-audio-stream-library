package com.kagisomedia.audiostreamlibrary.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

/**
 * A {@link AudioPlayer.RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements AudioPlayer.RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final String userAgent;
    private final Uri uri;
    private final Extractor extractor;
    private final DataSource dataSource;

    public ExtractorRendererBuilder(@NonNull Context context, String userAgent, Uri uri, Extractor extractor) {
        this.userAgent = userAgent;
        this.uri = uri;
        this.extractor = extractor;
        this.dataSource = new DefaultUriDataSource(context, userAgent);
    }
    public ExtractorRendererBuilder(String userAgent, Uri uri, Extractor extractor, DataSource dataSource) {
        this.userAgent = userAgent;
        this.uri = uri;
        this.extractor = extractor;
        this.dataSource = dataSource;
    }

    @Override
    public void buildRenderer(AudioPlayer player, AudioPlayer.RendererBuilderCallback callback) {

        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT, extractor);
        TrackRenderer renderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);
        callback.onRenderer(renderer);
    }

}
