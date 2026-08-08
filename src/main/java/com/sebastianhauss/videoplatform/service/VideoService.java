package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJobType;
import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.domain.video.VideoVisibility;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStatusResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStreamResource;
import com.sebastianhauss.videoplatform.exception.*;
import com.sebastianhauss.videoplatform.mapper.VideoMapper;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import com.sebastianhauss.videoplatform.repository.VideoRepository;
import com.sebastianhauss.videoplatform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private static final int MAX_FEED_PAGE_SIZE = 100;

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
     * The public frontpage: newest PUBLIC + READY videos. No authentication
     * required — this is what "everyone" sees.
     */
    public List<VideoResponse> getFeed(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_FEED_PAGE_SIZE));
        return videoMapper.toResponseList(
                videoRepository.findByVisibilityAndStatusOrderByCreatedAtDesc(
                        VideoVisibility.PUBLIC, VideoStatus.READY, pageable).getContent());
    }

    /**
     * Full metadata for a single video. A read: allowed for anyone on
     * PUBLIC/UNLISTED videos, owner-only on PRIVATE. {@code userId} may be
     * {@code null} for anonymous callers.
     */
    public VideoResponse getVideoMetadata(UUID videoId, UUID userId) {
        return videoMapper.toResponse(requireViewableVideo(videoId, userId));
    }

    /** Just the processing state, for cheap polling. */
    public VideoStatusResponse getVideoStatus(UUID videoId, UUID userId) {
        Video video = requireOwnedVideo(videoId, userId);
        return new VideoStatusResponse(video.getId(), video.getStatus());
    }

    /**
     * Stores the raw upload and enqueues a processing job, then returns
     * immediately with the video in {@link VideoStatus#UPLOADED}. Metadata
     * extraction and thumbnail generation happen asynchronously in
     * {@code VideoProcessingWorker}.
     */
    @Transactional
    public VideoResponse uploadVideo(UUID userId, MultipartFile file, VideoVisibility visibility) {
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
                .visibility(visibility != null ? visibility : VideoVisibility.PUBLIC)
                .build());

        processingJobService.createJob(video, ProcessingJobType.GENERATE_THUMBNAIL);
        log.info("Accepted video {} for processing", video.getId());

        return videoMapper.toResponse(video);
    }

    public VideoDownload downloadVideo(UUID videoId, UUID userId) {
        Video video = requireViewableVideo(videoId, userId);
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

    /**
     * Resolve one HTTP range for progressive playback. Honors the (optional)
     * {@code Range} header, clamps it to the object size, and fetches only that
     * slice from storage. A {@code null}/blank range yields the whole object.
     */
    public VideoStreamResource streamVideo(UUID videoId, UUID userId, String rangeHeader) {
        Video video = requireViewableVideo(videoId, userId);

        Long size = video.getSizeBytes();
        if (size == null || size <= 0) {
            throw new BadRequestException("Video has no streamable content yet");
        }

        long[] range = parseRange(rangeHeader, size);
        long start = range[0];
        long end = range[1];
        long length = end - start + 1;

        InputStream stream = storageService.downloadRange(
                new StoredObject(video.getBucket(), video.getObjectKey()), start, length);

        return new VideoStreamResource(
                new InputStreamResource(stream),
                video.getContentType(),
                start, end, size);
    }

    /** Owner-only mutation: change who can watch this video. */
    @Transactional
    public VideoResponse updateVisibility(UUID videoId, UUID userId, VideoVisibility visibility) {
        if (visibility == null) {
            throw new BadRequestException("visibility is required");
        }
        Video video = requireOwnedVideo(videoId, userId);
        video.setVisibility(visibility);
        return videoMapper.toResponse(videoRepository.save(video));
    }

    public void deleteVideo(UUID videoId, UUID userId) {
        Video video = requireOwnedVideo(videoId, userId);
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

    /** Loads the video and asserts the given user owns it, else 403. Used to guard mutations. */
    private Video requireOwnedVideo(UUID videoId, UUID userId) {
        Video video = requireVideo(videoId);
        if (!video.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Not your video!");
        }
        return video;
    }

    /**
     * Loads the video and asserts the caller may <em>watch</em> it: PUBLIC and
     * UNLISTED are viewable by anyone (including anonymous, {@code userId == null});
     * PRIVATE is owner-only. Used to guard reads. Note {@code owner.getId()} reads
     * the FK off the lazy proxy without initializing it, so no transaction needed.
     */
    private Video requireViewableVideo(UUID videoId, UUID userId) {
        Video video = requireVideo(videoId);
        if (video.getVisibility() == VideoVisibility.PRIVATE
                && (userId == null || !video.getOwner().getId().equals(userId))) {
            throw new ForbiddenException("This video is private");
        }
        return video;
    }

    /**
     * Parse a single-range {@code Range} header against {@code totalSize} and
     * return {@code [start, end]} inclusive, both clamped into {@code [0, totalSize)}.
     * Supports {@code bytes=start-end}, {@code bytes=start-} and suffix
     * {@code bytes=-N}. A missing/blank/multi-range/unit-mismatched header means
     * "whole object". A syntactically valid but unsatisfiable range yields 416.
     */
    private long[] parseRange(String rangeHeader, long totalSize) {
        long fullEnd = totalSize - 1;
        if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
            return new long[]{0, fullEnd};
        }

        String spec = rangeHeader.substring("bytes=".length()).trim();
        // Only a single range is supported; ignore anything with multiple ranges.
        if (spec.contains(",") || !spec.contains("-")) {
            return new long[]{0, fullEnd};
        }

        int dash = spec.indexOf('-');
        String startPart = spec.substring(0, dash).trim();
        String endPart = spec.substring(dash + 1).trim();

        try {
            long start;
            long end;
            if (startPart.isEmpty()) {
                // Suffix range: last N bytes.
                long suffix = Long.parseLong(endPart);
                if (suffix <= 0) {
                    throw rangeNotSatisfiable(rangeHeader);
                }
                start = Math.max(0, totalSize - suffix);
                end = fullEnd;
            } else {
                start = Long.parseLong(startPart);
                end = endPart.isEmpty() ? fullEnd : Long.parseLong(endPart);
                end = Math.min(end, fullEnd);
            }

            if (start < 0 || start > fullEnd || end < start) {
                throw rangeNotSatisfiable(rangeHeader);
            }
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            throw rangeNotSatisfiable(rangeHeader);
        }
    }

    private ApiException rangeNotSatisfiable(String rangeHeader) {
        return new ApiException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                "Cannot satisfy range: " + rangeHeader);
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
