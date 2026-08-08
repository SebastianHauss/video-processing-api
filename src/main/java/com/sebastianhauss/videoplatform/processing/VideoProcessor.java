package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoMetadata;
import com.sebastianhauss.videoplatform.exception.StorageException;
import com.sebastianhauss.videoplatform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Does the heavy lifting for a single video, off the request thread: pulls the
 * raw upload back from storage, extracts metadata and generates a thumbnail.
 * Runs from {@link VideoProcessingWorker}; performs no database work itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessor {

    private final StorageService storageService;
    private final VideoMetadataExtractor metadataExtractor;
    private final ThumbnailService thumbnailService;

    public ProcessedVideo process(Video video) {
        File tempFile = null;
        File tempThumbnail = null;

        try {
            tempFile = downloadToTemp(video);

            VideoMetadata metadata = metadataExtractor.extract(tempFile);
            if (metadata == null || metadata.contentType() == null
                    || !metadata.contentType().startsWith("video/")) {
                throw new IllegalStateException("Uploaded file is not a valid video");
            }

            long duration = metadata.durationMillis() != null ? metadata.durationMillis() : 0L;

            String thumbnailKey = null;
            try {
                tempThumbnail = thumbnailService.generateThumbnail(tempFile, duration);
                String thumbObjectKey = "thumbnails/" + video.getObjectKey() + ".png";
                storageService.uploadFromFile(tempThumbnail, thumbObjectKey, "image/png");
                thumbnailKey = thumbObjectKey;
            } catch (Exception e) {
                log.warn("Thumbnail generation failed for [{}], continuing without it",
                        video.getObjectKey(), e);
            }

            return new ProcessedVideo(duration, metadata.contentType(), thumbnailKey);

        } finally {
            deleteTempFile(tempFile);
            deleteTempFile(tempThumbnail);
        }
    }

    // ============================
    // helpers
    // ============================

    private File downloadToTemp(Video video) {
        try (InputStream in = storageService.download(
                new StoredObject(video.getBucket(), video.getObjectKey()))) {

            File temp = Files.createTempFile("video-process-", ".tmp").toFile();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            throw new StorageException("Could not read stored video for processing", e);
        }
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.warn("Could not delete temp file: {}", file.getAbsolutePath());
        }
    }
}
