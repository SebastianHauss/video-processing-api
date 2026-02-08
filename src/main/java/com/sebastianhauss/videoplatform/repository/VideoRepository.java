package com.sebastianhauss.videoplatform.repository;

import com.sebastianhauss.videoplatform.domain.video.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findAllByOrderByCreatedAtDesc();
    List<Video> findAllByOrderByUpdatedAtDesc();
    List<Video> findAllByOrderByCreatedAtAsc();
    List<Video> findAllByOrderByUpdatedAtAsc();
    List<Video> findVideosByOwner_Id(UUID userId);
}