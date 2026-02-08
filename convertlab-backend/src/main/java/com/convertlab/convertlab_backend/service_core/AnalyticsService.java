package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.entity.PageVisit;
import com.convertlab.convertlab_backend.repository.PageVisitRepository;
import com.convertlab.convertlab_backend.service_core.pojos.GeoLocation;
import com.convertlab.convertlab_backend.service_util.IpUtil;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PageVisitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PageVisitRepository pageVisitRepository;

    /**
     * Record a page visit - creates new record or increments existing
     */
    private final GeoLocationService geoLocationService;

    /**
     * Record a page visit with IP-based geolocation
     * Geolocation lookup is performed asynchronously to not block the response
     *
     * @param request Page visit request data
     * @param ipAddress Client IP address from request
     */
    @Transactional
    public void recordPageVisit(PageVisitRequest request, String ipAddress) {
        log.debug("Recording page visit for: {} from IP: {}", request, IpUtil.maskIp(ipAddress));

        UUID id = UUID.randomUUID();
        String ipHash = IpUtil.hashIp(ipAddress);

        // Start async geolocation lookup
        CompletableFuture<GeoLocation> locationFuture =
                geoLocationService.resolveLocationAsync(ipAddress);

        // Wait for geolocation result (with fallback)
        locationFuture.thenAccept(location -> {
            try {
                savePageVisit(id, request, ipHash, location);
            } catch (Exception e) {
                log.error("Failed to save page visit with geolocation", e);
                // Fallback: save without location
                savePageVisit(id, request, ipHash, GeoLocation.unknown());
            }
        }).exceptionally(ex -> {
            log.error("Geolocation lookup failed for IP: {}", IpUtil.maskIp(ipAddress), ex);
            // Fallback: save without location
            savePageVisit(id, request, ipHash, GeoLocation.unknown());
            return null;
        });
    }

    /**
     * Save page visit to database
     */
    private void savePageVisit(UUID id, PageVisitRequest request, String ipHash, GeoLocation location) {
        PageVisit pageVisit = new PageVisit(
                id,
                request.getSessionId(),
                request.getPath(),
                request.getEntry(),
                Instant.now(),
                ipHash,
                location.getCity(),
                location.getCountry(),
                location.getCountryCode()
        );

        pageVisitRepository.save(pageVisit);

        log.info("Page visit saved - Path: {}, Location: {}, {}, {}",
                request.getPath(), location.getCity(), location.getCountry(), location.getCountryCode());
    }


}