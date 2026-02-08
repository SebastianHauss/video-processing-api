package com.sebastianhauss.videoplatform.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;

@Component
@Slf4j
public class VideoMetadataExtractor {

    public Long extractDurationInMillis(File videoFile) {
        try {
            MultimediaObject multimediaObject = new MultimediaObject(videoFile);
            MultimediaInfo info = multimediaObject.getInfo();

            long durationMillis = info.getDuration();
            log.info("Extracted duration: {} ms ({} seconds)",
                    durationMillis, durationMillis / 1000.0);
            return durationMillis;

        } catch (Exception e) {
            log.error("Failed to extract video duration", e);
            return null;
        }
    }
}