package com.convertlab.convertlab_backend.websocket;

/**
 * All possible WebSocket event types across the application.
 *
 * Naming convention: <FEATURE>_<WHAT_HAPPENED>
 * Add new entries here as new features need real-time updates.
 */
public enum WebSocketEventType {

    DOCUMENT_EXTRACTED,
    DOCUMENT_CLEANED,
    DOCUMENT_CHUNKED,
    DOCUMENT_EMBEDDED,

    // ── Generic / System ────────────────────────────────────────────────────
    /** Heartbeat / keep-alive acknowledgment (optional). */
    PING,

    /** Server-side notification to the user (e.g. "Your plan renews tomorrow"). */
    NOTIFICATION,

    /** Site-wide admin broadcast with an expiry window. */
    BROADCAST_MESSAGE
}
