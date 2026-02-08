package com.sebastianhauss.videoplatform.domain.video;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VideoFactory {

    public Video createUploadedVideo(
            User owner,
            StoredObject stored,
            MultipartFile file,
            String contentType,
            Long durationMillis
    ) {
        return Video.builder()
                .owner(owner)
                .bucket(stored.bucket())
                .objectKey(stored.objectKey())
                .originalFilename(file.getOriginalFilename())
                .sizeBytes(file.getSize())
                .contentType(contentType)
                .durationMillis(durationMillis)
                .status(VideoStatus.UPLOADED)
                .build();
    }
}