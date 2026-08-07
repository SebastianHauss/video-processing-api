package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.exception.*;
import com.sebastianhauss.videoplatform.mapper.VideoMapper;
import com.sebastianhauss.videoplatform.processing.VideoUploadProcessor;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import com.sebastianhauss.videoplatform.repository.VideoRepository;
import com.sebastianhauss.videoplatform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final long MAX_SIZE_BYTES = 500L * 1024 * 1024;

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final VideoMapper videoMapper;
    private final VideoUploadProcessor videoUploadProcessor;

    public List<VideoResponse> getAllVideos() {
        return videoMapper.toResponseList(videoRepository.findAll());
    }

    public List<VideoResponse> getVideosOfUser(UUID userId) {
        return videoMapper.toResponseList(videoRepository.findVideosByOwner_Id(userId));
    }

    @Transactional
    public VideoResponse uploadVideo(UUID userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id '" + userId + "' not found"));

        ProcessedVideo processed = videoUploadProcessor.process(user, file);

        if (!processed.contentType().startsWith("video/")) {
            throw new BadRequestException("Upload file is not a valid video");
        }

        Video video = Video.builder()
                .owner(user)
                .bucket(processed.storedObject().bucket())
                .objectKey(processed.storedObject().objectKey())
                .originalFilename(file.getOriginalFilename())
                .sizeBytes(file.getSize())
                .contentType(processed.contentType())
                .durationMillis(processed.durationMillis())
                .thumbnailKey(processed.thumbnailKey())
                .status(VideoStatus.UPLOADED)
                .build();

        return videoMapper.toResponse(videoRepository.save(video));
    }

    public VideoDownload downloadVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("Video with id '" + videoId + "' not found"));

        try {
            InputStream stream = storageService.download(
                    new StoredObject(video.getBucket(), video.getObjectKey()));

            return new VideoDownload(
                    new InputStreamResource(stream),
                    video.getOriginalFilename(),
                    video.getContentType()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to download video", e);
        }
    }

    public void deleteVideo(UUID videoId, UUID userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("Video with id '" + videoId + "' not found"));

        if (!video.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Not your video!");
        }

        storageService.delete(video.getObjectKey());
        videoRepository.delete(video);
    }

    // *************************************
    // HELPER
    // *************************************

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 500MB");
        }
        if (!isValidVideoType(file.getContentType())) {
            throw new BadRequestException("Only video files allowed");
        }
    }

    private boolean isValidVideoType(String contentType) {
        if (contentType == null) return false;
        if (contentType.startsWith("video/")) return true;
        // octet-stream only tentatively allowed — real check happens via FFmpeg in the processor
        return contentType.equals("application/octet-stream");
    }
}
