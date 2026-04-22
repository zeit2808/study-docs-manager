package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.util.FileManagerNameUtils;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class FileManagerNamePolicy {

    public String requireFolderName(String name) {
        String trimmedName = FileManagerNameUtils.firstNonBlank(name);
        if (trimmedName == null) {
            throw new BadRequestException("Folder name is required", "FOLDER_NAME_REQUIRED", "name");
        }
        return trimmedName;
    }

    public String resolveDocumentName(String requestedDisplayName, String fileName, String title) {
        return FileManagerNameUtils.firstNonBlank(requestedDisplayName, fileName, title);
    }

    public String requireDocumentName(String requestedDisplayName, String fileName, String title) {
        String resolved = resolveDocumentName(requestedDisplayName, fileName, title);
        if (resolved == null || resolved.isBlank()) {
            throw new BadRequestException(
                    "Document display name is required",
                    "DOCUMENT_NAME_REQUIRED",
                    "displayName");
        }
        return resolved;
    }

    public String effectiveDocumentName(Document document) {
        DocumentAsset asset = document.getAsset();
        return resolveDocumentName(
                document.getDisplayName(),
                asset != null ? asset.getFileName() : null,
                document.getTitle());
    }

    public String normalize(String value) {
        return FileManagerNameUtils.normalize(value);
    }

    public String resolveCopyName(String preferredName, Set<String> occupiedNormalizedNames) {
        String trimmedPreferredName = FileManagerNameUtils.firstNonBlank(preferredName);
        if (trimmedPreferredName == null) {
            trimmedPreferredName = "Untitled";
        }

        String normalizedPreferredName = normalize(trimmedPreferredName);
        if (normalizedPreferredName != null && !occupiedNormalizedNames.contains(normalizedPreferredName)) {
            return trimmedPreferredName;
        }

        return FileManagerNameUtils.resolveCopyName(
                trimmedPreferredName,
                candidate -> occupiedNormalizedNames.contains(normalize(candidate)));
    }
}
