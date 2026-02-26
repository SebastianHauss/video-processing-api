package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoMetadata;
import com.sebastianhauss.videoplatform.exception.ErrorCode;
import com.sebastianhauss.videoplatform.exception.StorageException;
import com.sebastianhauss.videoplatform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoUploadProcessor {

    private final StorageService storageService;
    private final VideoMetadataExtractor metadataExtractor;
    private final ThumbnailService thumbnailService;

    public ProcessedVideo process(User user, MultipartFile multipartFile) {
        File tempFile = null;
        File tempThumbnail = null;

        try {
            tempFile = Files.createTempFile("video-upload-", ".tmp").toFile();
            multipartFile.transferTo(tempFile);

            VideoMetadata metadata = metadataExtractor.extract(tempFile);

            String contentType = (metadata != null && metadata.contentType() != null)
                    ? metadata.contentType()
                    : resolveContentType(multipartFile);

            long duration = (metadata != null && metadata.durationMillis() != null)
                    ? metadata.durationMillis()
                    : 0L;

            String objectKey = generateObjectKey(user, multipartFile);

            StoredObject storedObject = storageService.uploadFromFile(
                    tempFile,
                    objectKey,
                    contentType
            );

            String thumbnailKey = null;
            try {
                tempThumbnail = thumbnailService.generateThumbnail(tempFile);
                String thumbObjectKey = "thumbnails/" + objectKey + ".png";
                storageService.uploadFromFile(tempThumbnail, thumbObjectKey, "image/png");
                thumbnailKey = thumbObjectKey;
            } catch (Exception e) {
                log.warn("Thumbnail generation failed for [{}], continuing without it", objectKey, e);
            }

            return new ProcessedVideo(storedObject, duration, contentType, thumbnailKey);

        } catch (IOException e) {
            throw new StorageException(
                    ErrorCode.STORAGE_UPLOAD_FAILED,
                    "Could not process file",
                    e
            );
        } finally {
            deleteTempFile(tempFile);
            deleteTempFile(tempThumbnail);
        }
    }

    // ============================
    // helpers
    // ============================

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                log.warn("Could not delete temp file: {}", file.getAbsolutePath());
            }
        }
    }

    private String generateObjectKey(User user, MultipartFile file) {
        return user.getId() + "/" +
                UUID.randomUUID() + "-" +
                file.getOriginalFilename();
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank() || !contentType.contains("/")) {
            contentType = detectFromFilename(file.getOriginalFilename());
        }

        return contentType != null ? contentType : "application/octet-stream";
    }

    private String detectFromFilename(String filename) {
        if (filename == null) return null;

        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".flv")) return "video/x-flv";
        if (lower.endsWith(".wmv")) return "video/x-ms-wmv";
        if (lower.endsWith(".ts")) return "video/mp2t";
        if (lower.endsWith(".m4v")) return "video/x-m4v";
        if (lower.endsWith(".3gp")) return "video/3gpp";
        if (lower.endsWith(".ogv")) return "video/ogg";

        return null;
    }
}
