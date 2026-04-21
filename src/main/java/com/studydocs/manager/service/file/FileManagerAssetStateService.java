package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import org.springframework.stereotype.Service;

@Service
public class FileManagerAssetStateService {

    private final FileManagerResponseMapper fileManagerResponseMapper;

    public FileManagerAssetStateService(FileManagerResponseMapper fileManagerResponseMapper) {
        this.fileManagerResponseMapper = fileManagerResponseMapper;
    }

    public DocumentAsset resolveAsset(Document document) {
        return fileManagerResponseMapper.resolveAsset(document);
    }

    public boolean wasFileCleaned(DocumentAsset asset) {
        return fileManagerResponseMapper.wasFileCleaned(asset);
    }
}
