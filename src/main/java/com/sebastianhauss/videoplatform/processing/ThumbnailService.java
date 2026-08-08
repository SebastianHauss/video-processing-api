package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.config.SystemFFmpegLocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final SystemFFmpegLocator ffmpegLocator;

    /** Preferred seek position, in seconds, for videos long enough to allow it. */
    private static final float PREFERRED_OFFSET_SECONDS = 3f;

    public File generateThumbnail(File videoFile, long durationMillis) throws EncoderException, IOException {
        File outputFile = Files.createTempFile("thumb-", ".png").toFile();

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("image2");
        attrs.setOffset(thumbnailOffsetSeconds(durationMillis));
        attrs.setDuration(0.1f);

        VideoAttributes video = new VideoAttributes();
        video.setCodec("png");
        video.setFrameRate(1);

        attrs.setVideoAttributes(video);
        attrs.setAudioAttributes(null);

        new Encoder(ffmpegLocator).encode(new MultimediaObject(videoFile, ffmpegLocator), outputFile, attrs);

        // A seek past the last frame yields a valid-looking but empty PNG. Treat
        // that as a failure so the caller skips it instead of storing a 0-byte
        // thumbnail that reads as success.
        if (outputFile.length() == 0) {
            throw new IOException("Thumbnail render produced an empty file");
        }
        return outputFile;
    }

    /**
     * Seeks to 3s in when the clip is long enough, otherwise to its midpoint, so
     * that clips of 3 seconds or less still yield a real frame. Unknown/zero
     * duration falls back to the first frame.
     */
    private float thumbnailOffsetSeconds(long durationMillis) {
        if (durationMillis <= 0) {
            return 0f;
        }
        float halfSeconds = durationMillis / 2000f;
        return Math.min(PREFERRED_OFFSET_SECONDS, halfSeconds);
    }
}
