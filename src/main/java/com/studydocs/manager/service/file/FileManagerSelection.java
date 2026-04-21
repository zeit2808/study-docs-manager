package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.enums.FileManagerItemType;

public record FileManagerSelection(FileManagerItemType type, Folder folder, Document document) {

    public static FileManagerSelection forFolder(Folder folder) {
        return new FileManagerSelection(FileManagerItemType.FOLDER, folder, null);
    }

    public static FileManagerSelection forDocument(Document document) {
        return new FileManagerSelection(FileManagerItemType.DOCUMENT, null, document);
    }
}
