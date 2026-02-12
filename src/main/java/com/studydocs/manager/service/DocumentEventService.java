package com.studydocs.manager.service;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentEvent;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.DocumentEventRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import javax.print.Doc;

public class DocumentEventService {
    @Autowired
    private DocumentEventRepository documentEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;
    @Transactional
    public void logEvent(Long documentID, Long userId, DocumentEvent.DocumentEventType eventType,
                         String description, String oldValue, String newValue,
                         String ipAddress, String userAgent){
        Document document = documentRepository.findById(documentID).orElse(null);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        DocumentEvent event = new DocumentEvent();
        event.setDocument(document);
        event.setUser(user);
        event.setEventType(eventType);
        event.setEventDescription(description);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);

        documentEventRepository.save(event);
    }
}
