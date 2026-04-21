package com.studydocs.manager.dto.filemanager;

import com.studydocs.manager.enums.FileManagerItemType;

public class FileManagerPasteResult {

    private FileManagerItemType sourceType;
    private Long sourceId;
    private Long resultId;
    private String finalName;
    private Long targetFolderId;

    public FileManagerItemType getSourceType() {
        return sourceType;
    }

    public void setSourceType(FileManagerItemType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public String getFinalName() {
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public Long getTargetFolderId() {
        return targetFolderId;
    }

    public void setTargetFolderId(Long targetFolderId) {
        this.targetFolderId = targetFolderId;
    }
}
