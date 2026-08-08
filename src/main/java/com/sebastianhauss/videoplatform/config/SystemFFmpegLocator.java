package com.sebastianhauss.videoplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;

/**
 * Points JAVE at an ffmpeg binary present on the host instead of the one JAVE
 * bundles in its jars. Defaults to "ffmpeg" (resolved via PATH) and can be
 * overridden with the {@code ffmpeg.path} property / {@code FFMPEG_PATH} env var.
 * <p>
 * Implements {@link ProcessLocator} directly (rather than extending
 * {@code DefaultFFMPEGLocator}) so no bundled binary is ever extracted.
 */
@Component
public class SystemFFmpegLocator implements ProcessLocator {

    private final String ffmpegPath;

    public SystemFFmpegLocator(@Value("${ffmpeg.path:ffmpeg}") String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public String getExecutablePath() {
        return ffmpegPath;
    }

    @Override
    public ProcessWrapper createExecutor() {
        return new ProcessWrapper(ffmpegPath);
    }
}
