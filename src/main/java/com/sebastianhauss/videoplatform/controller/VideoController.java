package com.sebastianhauss.videoplatform.controller;

import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStatusResponse;
import com.sebastianhauss.videoplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping()
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<VideoResponse>> getVideosOfUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(videoService.getVideosOfUser(userId));
    }

    @PostMapping
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam UUID userId,
            @RequestParam MultipartFile file
    ) {
        // 202: accepted for asynchronous processing; poll the video's status for readiness
        return ResponseEntity.accepted().body(videoService.uploadVideo(userId, file));
    }

    @GetMapping("/{videoId}/metadata")
    public ResponseEntity<VideoResponse> getVideoMetadata(@PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getVideoMetadata(videoId));
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(@PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getVideoStatus(videoId));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable UUID videoId) {

        VideoDownload download = videoService.downloadVideo(videoId);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + download.filename() + "\"")
                .header("Content-Type", download.contentType())
                .body(download.resource());
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable UUID videoId,
            @RequestParam UUID userId) {

        videoService.deleteVideo(videoId, userId);
        return ResponseEntity.noContent().build();
    }
}
