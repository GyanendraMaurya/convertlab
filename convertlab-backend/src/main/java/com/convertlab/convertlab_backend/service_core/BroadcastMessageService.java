package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.entity.BroadcastMessage;
import com.convertlab.convertlab_backend.exception.NotFoundException;
import com.convertlab.convertlab_backend.repository.BroadcastMessageRepository;
import com.convertlab.convertlab_backend.service_web.controllers.dto.BroadcastMessageResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.CreateBroadcastRequest;
import com.convertlab.convertlab_backend.websocket.WebSocketEvent;
import com.convertlab.convertlab_backend.websocket.WebSocketEventType;
import com.convertlab.convertlab_backend.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BroadcastMessageService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int ADMIN_HISTORY_LIMIT = 50;

    private final BroadcastMessageRepository broadcastMessageRepository;
    private final WebSocketService webSocketService;

    public List<BroadcastMessageResponse> getActiveBroadcasts() {
        Instant now = Instant.now();
        return broadcastMessageRepository.findAllByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(now)
                .stream()
                .map(message -> toResponse(message, now))
                .toList();
    }

    public List<BroadcastMessageResponse> getAdminBroadcasts() {
        Instant now = Instant.now();
        return broadcastMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, ADMIN_HISTORY_LIMIT))
                .stream()
                .map(message -> toResponse(message, now))
                .toList();
    }

    public BroadcastMessageResponse createBroadcast(CreateBroadcastRequest request) {
        String message = normalizeMessage(request.message());
        Instant expiresAt = request.expiresAt();
        Instant now = Instant.now();

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("Expiry must be in the future.");
        }

        BroadcastMessage saved = broadcastMessageRepository.save(new BroadcastMessage(message, expiresAt));
        BroadcastMessageResponse response = toResponse(saved, now);
        webSocketService.broadcast(WebSocketEvent.of(WebSocketEventType.BROADCAST_MESSAGE, response));
        return response;
    }

    public BroadcastMessageResponse deactivateBroadcast(UUID id) {
        BroadcastMessage broadcastMessage = broadcastMessageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Broadcast message not found."));

        broadcastMessage.deactivate();
        return toResponse(broadcastMessageRepository.save(broadcastMessage), Instant.now());
    }

    private String normalizeMessage(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required.");
        }

        String message = value.trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message must be 500 characters or fewer.");
        }

        return message;
    }

    private BroadcastMessageResponse toResponse(BroadcastMessage broadcastMessage, Instant now) {
        boolean active = broadcastMessage.isActive() && broadcastMessage.getExpiresAt().isAfter(now);
        return new BroadcastMessageResponse(
                broadcastMessage.getId(),
                broadcastMessage.getMessage(),
                broadcastMessage.getCreatedAt(),
                broadcastMessage.getExpiresAt(),
                active
        );
    }
}
