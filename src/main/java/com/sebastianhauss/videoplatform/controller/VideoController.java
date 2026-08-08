package com.sebastianhauss.videoplatform.controller;

import com.sebastianhauss.videoplatform.auth.AppUserPrincipal;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStatusResponse;
import com.sebastianhauss.videoplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VideoResponse>> getVideosOfUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(videoService.getVideosOfUser(userId));
    }

    /** The authenticated caller's own videos — the non-admin way to list. */
    @GetMapping("/mine")
    public ResponseEntity<List<VideoResponse>> getMyVideos(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(videoService.getVideosOfUser(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<VideoResponse> uploadVideo(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam MultipartFile file
    ) {
        // 202: accepted for asynchronous processing; poll the video's status for readiness
        return ResponseEntity.accepted().body(videoService.uploadVideo(principal.getId(), file));
    }

    @GetMapping("/{videoId}/metadata")
    public ResponseEntity<VideoResponse> getVideoMetadata(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(videoService.getVideoMetadata(videoId, principal.getId()));
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(videoService.getVideoStatus(videoId, principal.getId()));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<Resource> downloadVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        VideoDownload download = videoService.downloadVideo(videoId, principal.getId());

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + download.filename() + "\"")
                .header("Content-Type", download.contentType())
                .body(download.resource());
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        videoService.deleteVideo(videoId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
