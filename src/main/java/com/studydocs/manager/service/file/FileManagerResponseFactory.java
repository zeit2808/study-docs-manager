package com.studydocs.manager.service.file;

import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResult;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.enums.ClipboardOperation;
import com.studydocs.manager.enums.FileManagerItemType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileManagerResponseFactory {

    public FileManagerPasteResult buildResult(
            FileManagerItemType type,
            Long sourceId,
            Long resultId,
            String finalName,
            Folder targetFolder) {
        FileManagerPasteResult result = new FileManagerPasteResult();
        result.setSourceType(type);
        result.setSourceId(sourceId);
        result.setResultId(resultId);
        result.setFinalName(finalName);
        result.setTargetFolderId(targetFolder != null ? targetFolder.getId() : null);
        return result;
    }

    public FileManagerPasteResponse buildResponse(
            ClipboardOperation operation,
            Folder targetFolder,
            int folderCount,
            int documentCount,
            List<FileManagerPasteResult> results) {
        FileManagerPasteResponse response = new FileManagerPasteResponse();
        response.setOperation(operation);
        response.setTargetFolderId(targetFolder != null ? targetFolder.getId() : null);
        response.setFolderCount(folderCount);
        response.setDocumentCount(documentCount);
        response.setResults(results);
        return response;
    }
}
