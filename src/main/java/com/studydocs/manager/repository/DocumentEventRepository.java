package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentEventRepository extends JpaRepository<DocumentEvent,Long> {

}
