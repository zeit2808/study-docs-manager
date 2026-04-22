package com.studydocs.manager.repository;

import com.studydocs.manager.entity.FolderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderEventRepository extends JpaRepository<FolderEvent, Long> {

    List<FolderEvent> findByFolderIdOrderByCreatedAtDesc(Long folderId);
}
