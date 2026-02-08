package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoFactory;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.exception.*;
import com.sebastianhauss.videoplatform.mapper.VideoMapper;
import com.sebastianhauss.videoplatform.processing.VideoMetadataExtractor;
import com.sebastianhauss.videoplatform.processing.VideoUploadProcessor;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import com.sebastianhauss.videoplatform.repository.VideoRepository;
import com.sebastianhauss.videoplatform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
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

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final VideoMapper videoMapper;
    private final VideoFactory videoFactory;
    private final VideoUploadProcessor videoUploadProcessor;

    @Transactional
    public VideoResponse uploadVideo(UUID userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ProcessedVideo processed = videoUploadProcessor.process(user, file);

        Video video = videoFactory.createUploadedVideo(
                user,
                processed.storedObject(),
                file,
                processed.contentType(),
                processed.durationMillis()
        );

        video.setStatus(VideoStatus.PROCESSING);

        return videoMapper.toResponse(videoRepository.save(video));
    }

    public List<VideoResponse> getVideosOfUser(UUID userId) {
        List<Video> videos = videoRepository.findVideosByOwner_Id(userId);
        return videoMapper.toResponseList(videos);
    }

    public VideoDownload downloadVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));

        try {
            StoredObject stored = new StoredObject(
                    video.getBucket(),
                    video.getObjectKey()
            );

            InputStream stream = storageService.download(stored);

            return new VideoDownload(
                    new InputStreamResource(stream),
                    video.getOriginalFilename(),
                    video.getContentType()
            );
        } catch (Exception e) {
            throw new VideoDownloadException("Failed to download video", e);
        }
    }

    public void deleteVideo(UUID videoId, UUID userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));

        if (!video.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("Not your video!");
        }

        storageService.delete(video.getObjectKey());
        videoRepository.delete(video);
    }

    // *************************************
    // HELPER
    // *************************************

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        // Max size (e.g., 500MB)
        long maxSize = 500 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new FileTooLargeException("File exceeds 500MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (!isValidVideoType(contentType)) {
            throw new InvalidFileTypeException("Only video files allowed");
        }
    }

    private boolean isValidVideoType(String contentType) {
        return contentType != null && (
                contentType.startsWith("video/") ||
                        contentType.equals("application/octet-stream")
        );
    }
}
