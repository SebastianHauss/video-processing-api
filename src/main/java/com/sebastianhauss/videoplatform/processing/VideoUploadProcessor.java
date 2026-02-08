package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
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

    public ProcessedVideo process(User user, MultipartFile multipartFile) {
        File tempFile = null;

        try {
            // 1 convert to temp file
            tempFile = Files.createTempFile("video-upload-", ".tmp").toFile();
            multipartFile.transferTo(tempFile);

            // 2 metadata
            long duration = metadataExtractor.extractDurationInMillis(tempFile);

            // 3 content type
            String contentType = resolveContentType(multipartFile);

            // 4 object key
            String objectKey = generateObjectKey(user, multipartFile);

            // 5 upload
            StoredObject storedObject = storageService.uploadFromFile(
                    tempFile,
                    objectKey,
                    contentType
            );

            return new ProcessedVideo(storedObject, duration, contentType);

        } catch (IOException e) {
            throw new StorageException(
                    ErrorCode.STORAGE_UPLOAD_FAILED,
                    "Could not process file",
                    e
            );
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // ============================
    // helpers
    // ============================

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

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return contentType;
    }

    private String detectFromFilename(String filename) {
        if (filename == null) return null;

        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".flv")) return "video/x-flv";

        return null;
    }
}
