package com.studydocs.manager.repository;

import com.studydocs.manager.entity.FolderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FolderEventRepository extends JpaRepository<FolderEvent, Long> {

    List<FolderEvent> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    @Modifying
    @Query("DELETE FROM FolderEvent e WHERE e.folder.id IN :folderIds")
    void deleteByFolderIdIn(@Param("folderIds") List<Long> folderIds);
}
