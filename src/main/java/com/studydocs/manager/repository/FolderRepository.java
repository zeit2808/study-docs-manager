package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder,Long> {
    List<Folder> findByUserIdAndParentIdIsNull(Long userId);
    List<Folder> findByUserIdAndParentId(Long userId, Long parentId);
}
