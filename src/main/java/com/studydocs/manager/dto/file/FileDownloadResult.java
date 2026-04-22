package com.studydocs.manager.dto.file;

import org.springframework.core.io.Resource;

public record FileDownloadResult(
        Resource resource,
        String filename) {
}
