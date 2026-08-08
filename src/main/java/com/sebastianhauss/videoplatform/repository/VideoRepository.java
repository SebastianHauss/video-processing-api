package com.sebastianhauss.videoplatform.repository;

import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.domain.video.VideoVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findAllByOrderByCreatedAtDesc();
    List<Video> findAllByOrderByUpdatedAtDesc();
    List<Video> findAllByOrderByCreatedAtAsc();
    List<Video> findAllByOrderByUpdatedAtAsc();
    List<Video> findVideosByOwner_Id(UUID userId);

    /** The public frontpage: newest ready-and-public videos first. */
    Page<Video> findByVisibilityAndStatusOrderByCreatedAtDesc(
            VideoVisibility visibility, VideoStatus status, Pageable pageable);
}
