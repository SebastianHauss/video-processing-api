package com.sebastianhauss.videoplatform.dto.video;

import org.springframework.core.io.Resource;

/**
 * A byte slice of a video for HTTP range streaming. {@code rangeStart} and
 * {@code rangeEnd} are inclusive; {@code totalSize} is the full object size, so
 * the controller can build the {@code Content-Range} header.
 */
public record VideoStreamResource(
        Resource resource,
        String contentType,
        long rangeStart,
        long rangeEnd,
        long totalSize
) {
    /** Number of bytes carried by this slice (the {@code Content-Length}). */
    public long contentLength() {
        return rangeEnd - rangeStart + 1;
    }

    /** True when this represents a partial range rather than the whole object. */
    public boolean isPartial() {
        return rangeStart > 0 || rangeEnd < totalSize - 1;
    }
}
