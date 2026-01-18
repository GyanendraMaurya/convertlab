package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.entity.PageVisit;
import com.convertlab.convertlab_backend.repository.PageVisitRepository;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PageVisitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PageVisitRepository pageVisitRepository;

    /**
     * Record a page visit - creates new record or increments existing
     */
    @Transactional
    public void recordPageVisit(PageVisitRequest request) {
        UUID id = UUID.randomUUID();
        PageVisit pageVisit = new PageVisit(id, request.getSessionId(), request.getPath(), request.getEntry(), Instant.now());
        log.debug("Recording page visit for: {}", request);
        pageVisitRepository.save(pageVisit);
    }


}