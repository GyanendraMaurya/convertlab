package com.convertlab.convertlab_backend.websocket;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Generic envelope for all WebSocket messages.
 *
 * Usage example:
 * <pre>
 * WebSocketEvent.of(WebSocketEventType.AI_INGEST_PROGRESS,
 *     IngestProgressPayload.builder()
 *         .stage(IngestStage.EMBEDDING)
 *         .percent(60)
 *         .message("Generating embeddings...")
 *         .build()
 * )
 * </pre>
 */
@Data
@Builder
public class WebSocketEvent<T> {

    /** Discriminator — tells the frontend which payload shape to expect. */
    private WebSocketEventType type;

    /** Strongly-typed payload; will be serialized to JSON. */
    private T payload;

    /** Server-side timestamp (epoch millis) for ordering/deduplication. */
    private long timestamp;

    public static <T> WebSocketEvent<T> of(WebSocketEventType type, T payload) {
        return WebSocketEvent.<T>builder()
                .type(type)
                .payload(payload)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}
