package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.config.SystemFFmpegLocator;
import com.sebastianhauss.videoplatform.dto.video.VideoMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoMetadataExtractor {

    private final SystemFFmpegLocator ffmpegLocator;

    public VideoMetadata extract(File videoFile) {
        try {
            MultimediaInfo info = new MultimediaObject(videoFile, ffmpegLocator).getInfo();

            long duration = info.getDuration();
            String contentType = mapFormatToContentType(info.getFormat());

            log.info("Extracted duration: {} ms ({} seconds)", duration, duration / 1000.0);

            return new VideoMetadata(contentType, duration);

        } catch (Exception e) {
            log.error("Failed to extract video metadata", e);
            return null;
        }
    }

    private String mapFormatToContentType(String format) {
        if (format == null) return null;

        if (format.contains("mp4") || format.contains("mov")) return "video/mp4";
        if (format.contains("matroska")) return "video/x-matroska";
        if (format.contains("webm")) return "video/webm";
        if (format.contains("avi")) return "video/x-msvideo";
        if (format.contains("flv")) return "video/x-flv";
        if (format.contains("mpegts")) return "video/mp2t";
        if (format.contains("3gp")) return "video/3gpp";
        if (format.contains("wmv") || format.contains("asf")) return "video/x-ms-wmv";
        if (format.contains("ogg")) return "video/ogg";
        if (format.contains("m4v")) return "video/x-m4v";

        return null;
    }
}