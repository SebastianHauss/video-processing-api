package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.config.SystemFFmpegLocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final SystemFFmpegLocator ffmpegLocator;

    /** Preferred seek position, in seconds, for videos long enough to allow it. */
    private static final float PREFERRED_OFFSET_SECONDS = 3f;

    /** Hard cap so a wedged ffmpeg process can't block the worker thread forever. */
    private static final long FFMPEG_TIMEOUT_SECONDS = 30;

    /**
     * Grabs a single frame as a PNG by invoking ffmpeg directly. We shell out
     * rather than use JAVE's encoder because JAVE can't express {@code -frames:v 1};
     * its duration/frame-rate approximation makes the image2 muxer complain about a
     * missing sequence pattern. The explicit command is simpler and unambiguous:
     * seek to the offset (input seeking, so it's fast) and write exactly one frame.
     */
    public File generateThumbnail(File videoFile, long durationMillis) throws IOException {
        File outputFile = Files.createTempFile("thumb-", ".png").toFile();

        String offset = String.format(Locale.ROOT, "%.3f", thumbnailOffsetSeconds(durationMillis));
        List<String> command = List.of(
                ffmpegLocator.getExecutablePath(),
                "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                "-ss", offset,
                "-i", videoFile.getAbsolutePath(),
                "-frames:v", "1",
                outputFile.getAbsolutePath()
        );

        runFfmpeg(command);

        // A seek past the last frame writes no frame, leaving a 0-byte file. Treat
        // that as a failure so the caller skips it instead of storing an empty
        // thumbnail that reads as success.
        if (outputFile.length() == 0) {
            throw new IOException("Thumbnail render produced an empty file");
        }
        return outputFile;
    }

    private void runFfmpeg(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("ffmpeg timed out after " + FFMPEG_TIMEOUT_SECONDS + "s generating thumbnail");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while generating thumbnail", e);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("ffmpeg exited with code " + exitCode + ": " + output.strip());
        }
        if (!output.isBlank()) {
            log.debug("ffmpeg thumbnail output: {}", output.strip());
        }
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
