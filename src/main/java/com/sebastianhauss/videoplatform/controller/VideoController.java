package com.sebastianhauss.videoplatform.controller;

import com.sebastianhauss.videoplatform.auth.AppUserPrincipal;
import com.sebastianhauss.videoplatform.domain.video.VideoVisibility;
import com.sebastianhauss.videoplatform.dto.video.VideoDownload;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStatusResponse;
import com.sebastianhauss.videoplatform.dto.video.VideoStreamResource;
import com.sebastianhauss.videoplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    /** The public frontpage — no authentication required. */
    @GetMapping("/feed")
    public ResponseEntity<List<VideoResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(videoService.getFeed(page, size));
    }

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
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "PUBLIC") VideoVisibility visibility
    ) {
        // 202: accepted for asynchronous processing; poll the video's status for readiness
        return ResponseEntity.accepted()
                .body(videoService.uploadVideo(principal.getId(), file, visibility));
    }

    @GetMapping("/{videoId}/metadata")
    public ResponseEntity<VideoResponse> getVideoMetadata(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(videoService.getVideoMetadata(videoId, callerId(principal)));
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(videoService.getVideoStatus(videoId, principal.getId()));
    }

    /**
     * Progressive streaming for {@code <video>} players. Honors the HTTP
     * {@code Range} header and replies {@code 206 Partial Content} with
     * {@code Accept-Ranges}/{@code Content-Range} so the browser can seek.
     * Public/unlisted videos stream without authentication.
     */
    @GetMapping("/{videoId}/stream")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {

        VideoStreamResource stream =
                videoService.streamVideo(videoId, callerId(principal), range);

        HttpStatus status = stream.isPartial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

        return ResponseEntity.status(status)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_TYPE, stream.contentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(stream.contentLength()))
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + stream.rangeStart() + "-" + stream.rangeEnd()
                                + "/" + stream.totalSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(stream.resource());
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<Resource> downloadVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        VideoDownload download = videoService.downloadVideo(videoId, callerId(principal));

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + download.filename() + "\"")
                .header("Content-Type", download.contentType())
                .body(download.resource());
    }

    /** Owner-only: change who can watch this video. */
    @PatchMapping("/{videoId}/visibility")
    public ResponseEntity<VideoResponse> updateVisibility(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam VideoVisibility visibility) {
        return ResponseEntity.ok(
                videoService.updateVisibility(videoId, principal.getId(), visibility));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        videoService.deleteVideo(videoId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** Caller id from the JWT, or {@code null} for anonymous access to public content. */
    private UUID callerId(AppUserPrincipal principal) {
        return principal != null ? principal.getId() : null;
    }
}
