package com.studydocs.manager.service.document;
import com.studydocs.manager.enums.*;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentEvent;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.DocumentEventRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventService {
    private final DocumentEventRepository documentEventRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public DocumentEventService(
            DocumentEventRepository documentEventRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository) {
        this.documentEventRepository = documentEventRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void logEvent(Long documentID, Long userId, DocumentEventType eventType,
            String description, String oldValue, String newValue,
            String ipAddress, String userAgent) {
        Document document = documentRepository.findById(documentID).orElse(null);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        DocumentEvent event = new DocumentEvent();
        event.setDocument(document);
        event.setUser(user);
        event.setEventType(eventType);
        event.setEventDescription(description);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        // ipAddress & userAgent removed from DocumentEvent (belongs to AuditLog only)

        documentEventRepository.save(event);
    }
}
