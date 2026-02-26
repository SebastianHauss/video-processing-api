package com.sebastianhauss.videoplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

@Component
public class SystemFFmpegLocator extends DefaultFFMPEGLocator {

    private final String ffmpegPath;

    public SystemFFmpegLocator(@Value("${ffmpeg.path:/opt/homebrew/bin/ffmpeg}") String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public ProcessWrapper createExecutor() {
        return new ProcessWrapper(ffmpegPath);
    }
}