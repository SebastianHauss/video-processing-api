package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJobType;
import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.exception.*;
import com.sebastianhauss.videoplatform.mapper.VideoMapper;
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
    private final ProcessingJobService processingJobService;

    public List<VideoResponse> getAllVideos() {
        return videoMapper.toResponseList(videoRepository.findAll());
    }

    public List<VideoResponse> getVideosOfUser(UUID userId) {
        return videoMapper.toResponseList(videoRepository.findVideosByOwner_Id(userId));
    }

    /**
     * Stores the raw upload and enqueues a processing job, then returns
     * immediately with the video in {@link VideoStatus#UPLOADED}. Metadata
     * extraction and thumbnail generation happen asynchronously in
     * {@code VideoProcessingWorker}.
     */
    @Transactional
    public VideoResponse uploadVideo(UUID userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id '" + userId + "' not found"));

        String objectKey = generateObjectKey(user, file);
        String contentType = resolveContentType(file);

        StoredObject stored = storageService.upload(file, objectKey, contentType);

        Video video = videoRepository.save(Video.builder()
                .owner(user)
                .bucket(stored.bucket())
                .objectKey(stored.objectKey())
                .originalFilename(file.getOriginalFilename())
                .sizeBytes(file.getSize())
                .contentType(contentType)
                .status(VideoStatus.UPLOADED)
                .build());

        processingJobService.createJob(video, ProcessingJobType.GENERATE_THUMBNAIL);
        log.info("Accepted video {} for processing", video.getId());

        return videoMapper.toResponse(video);
    }

    public VideoDownload downloadVideo(UUID videoId) {
        Video video = requireVideo(videoId);
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
        Video video = requireVideo(videoId);

        if (!video.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Not your video!");
        }

        storageService.delete(video.getObjectKey());
        videoRepository.delete(video);
    }

    // *************************************
    // PROCESSING LIFECYCLE (called by the worker)
    // *************************************

    /** Marks the video PROCESSING and returns the loaded entity for the worker. */
    @Transactional
    public Video markProcessing(UUID videoId) {
        Video video = requireVideo(videoId);
        video.setStatus(VideoStatus.PROCESSING);
        return videoRepository.save(video);
    }

    @Transactional
    public void markReady(UUID videoId, ProcessedVideo result) {
        Video video = requireVideo(videoId);
        video.setDurationMillis(result.durationMillis());
        video.setContentType(result.contentType());
        video.setThumbnailKey(result.thumbnailKey());
        video.setStatus(VideoStatus.READY);
        videoRepository.save(video);
    }

    @Transactional
    public void markFailed(UUID videoId) {
        Video video = requireVideo(videoId);
        video.setStatus(VideoStatus.FAILED);
        videoRepository.save(video);
    }

    // *************************************
    // HELPERS
    // *************************************

    private Video requireVideo(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("Video with id '" + videoId + "' not found"));
    }

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
        // octet-stream only tentatively allowed — real check happens via FFmpeg during processing
        return contentType.equals("application/octet-stream");
    }

    private String generateObjectKey(User user, MultipartFile file) {
        return user.getId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
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
