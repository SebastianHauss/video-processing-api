package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.user.UserRole;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.dto.video.VideoStatusResponse;
import com.sebastianhauss.videoplatform.exception.ForbiddenException;
import com.sebastianhauss.videoplatform.exception.NotFoundException;
import com.sebastianhauss.videoplatform.mapper.VideoMapper;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import com.sebastianhauss.videoplatform.repository.VideoRepository;
import com.sebastianhauss.videoplatform.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the ownership authorization on owner-scoped operations: the caller must
 * own the video, otherwise the operation is rejected before any side effect
 * (mapping, storage read/delete) happens. This is the security-critical logic
 * behind {@code getVideoMetadata}, {@code getVideoStatus}, {@code downloadVideo}
 * and {@code deleteVideo}.
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceAuthorizationTest {

    @Mock
    private VideoRepository videoRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoMapper videoMapper;
    @Mock
    private ProcessingJobService processingJobService;

    @InjectMocks
    private VideoService videoService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID videoId = UUID.randomUUID();

    private Video video;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(ownerId)
                .username("owner")
                .passwordHash("hash")
                .email("owner@example.com")
                .role(UserRole.USER)
                .build();

        video = Video.builder()
                .id(videoId)
                .owner(owner)
                .bucket("bucket")
                .objectKey("owner/key.mp4")
                .originalFilename("clip.mp4")
                .contentType("video/mp4")
                .status(VideoStatus.READY)
                .build();
    }

    @Test
    void getVideoStatus_ownerIsAllowed() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        VideoStatusResponse response = videoService.getVideoStatus(videoId, ownerId);

        assertThat(response.id()).isEqualTo(videoId);
        assertThat(response.status()).isEqualTo(VideoStatus.READY);
    }

    @Test
    void getVideoStatus_nonOwnerIsForbidden() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> videoService.getVideoStatus(videoId, otherUserId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getVideoMetadata_nonOwnerIsForbidden_andNeverMapsResponse() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> videoService.getVideoMetadata(videoId, otherUserId))
                .isInstanceOf(ForbiddenException.class);

        verify(videoMapper, never()).toResponse(any());
    }

    @Test
    void downloadVideo_nonOwnerIsForbidden_andNeverReadsStorage() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> videoService.downloadVideo(videoId, otherUserId))
                .isInstanceOf(ForbiddenException.class);

        verify(storageService, never()).download(any());
    }

    @Test
    void deleteVideo_nonOwnerIsForbidden_andNeverDeletes() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> videoService.deleteVideo(videoId, otherUserId))
                .isInstanceOf(ForbiddenException.class);

        verify(storageService, never()).delete(anyString());
        verify(videoRepository, never()).delete(any());
    }

    @Test
    void deleteVideo_ownerDeletesObjectAndRow() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        videoService.deleteVideo(videoId, ownerId);

        verify(storageService).delete("owner/key.mp4");
        verify(videoRepository).delete(video);
    }

    @Test
    void unknownVideoIsNotFound() {
        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.getVideoStatus(videoId, ownerId))
                .isInstanceOf(NotFoundException.class);
    }
}
