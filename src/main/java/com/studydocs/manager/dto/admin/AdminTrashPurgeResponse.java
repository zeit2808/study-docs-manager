package com.studydocs.manager.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class AdminTrashPurgeResponse {

    private int purgedDocuments;
    private int purgedFolderRoots;
    private List<Long> failedDocumentIds = new ArrayList<>();
    private List<Long> failedFolderRootIds = new ArrayList<>();

    public int getPurgedDocuments() {
        return purgedDocuments;
    }

    public void setPurgedDocuments(int purgedDocuments) {
        this.purgedDocuments = purgedDocuments;
    }

    public int getPurgedFolderRoots() {
        return purgedFolderRoots;
    }

    public void setPurgedFolderRoots(int purgedFolderRoots) {
        this.purgedFolderRoots = purgedFolderRoots;
    }

    public List<Long> getFailedDocumentIds() {
        return failedDocumentIds;
    }

    public void setFailedDocumentIds(List<Long> failedDocumentIds) {
        this.failedDocumentIds = failedDocumentIds;
    }

    public List<Long> getFailedFolderRootIds() {
        return failedFolderRootIds;
    }

    public void setFailedFolderRootIds(List<Long> failedFolderRootIds) {
        this.failedFolderRootIds = failedFolderRootIds;
    }
}
