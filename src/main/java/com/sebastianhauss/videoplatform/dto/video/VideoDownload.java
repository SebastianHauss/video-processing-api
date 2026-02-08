package com.sebastianhauss.videoplatform.dto.video;

import org.springframework.core.io.Resource;

public record VideoDownload(
        Resource resource,
        String filename,
        String contentType
) {}
