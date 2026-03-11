import { inject, Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject, filter } from 'rxjs';
import { environment } from '../../environments/environment';
import { SessionService } from './session.service';


export type WebSocketEventType =
    | 'DOCUMENT_EXTRACTED'
    | 'DOCUMENT_CLEANED'
    | 'DOCUMENT_CHUNKED'
    | 'DOCUMENT_EMBEDDED'
    | 'NOTIFICATION'
    | 'PING';


export interface NotificationPayload {
    title: string;
    message: string;
}

export interface WebSocketEvent<T = unknown> {
    type: WebSocketEventType;
    payload: T;
    timestamp: number;
}

export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

    private readonly sessionService = inject(SessionService);

    // Observable stream of ALL incoming events — components filter by type
    private readonly eventSubject = new Subject<WebSocketEvent>();
    readonly events$ = this.eventSubject.asObservable();

    // Connection state — useful for showing a "Live" indicator in the UI
    private readonly connectionState = new BehaviorSubject<ConnectionState>('DISCONNECTED');
    readonly connectionState$ = this.connectionState.asObservable();

    private client: Client | null = null;
    private subscriptions: StompSubscription[] = [];

    // ── Connect / Disconnect ───────────────────────────────────────────────────

    /**
     * Connect to the WebSocket server.
     *
     * @param accessToken  Optional JWT. When provided:
     *   - Sent in the STOMP CONNECT headers so the server sets the user principal
     *   - Enables subscription to /user/queue/events (user-specific events)
     *
     * Without a token the service still connects and subscribes to
     * /topic/session/{sessionId} for anonymous/background updates.
     */
    connect(accessToken?: string): void {
        if (this.client?.active) {
            return; // already connected
        }

        this.connectionState.next('CONNECTING');
        const sessionId = this.sessionService.sessionId();
        if (!sessionId) {
            return;
        }

        this.client = new Client({
            webSocketFactory: () => new SockJS(`${environment.apiUrl}/ws`) as WebSocket,

            connectHeaders: accessToken
                ? { Authorization: `Bearer ${accessToken}` }
                : {},

            reconnectDelay: 5000,

            onConnect: () => {
                this.connectionState.next('CONNECTED');
                this.setupSubscriptions(!!accessToken, sessionId);
            },

            onDisconnect: () => {
                this.connectionState.next('DISCONNECTED');
                this.subscriptions = [];
            },

            onStompError: (frame) => {
                console.error('STOMP error', frame);
                this.connectionState.next('ERROR');
            },

            onWebSocketError: (event) => {
                console.error('WebSocket error', event);
                this.connectionState.next('ERROR');
            },
        });

        this.client.activate();
    }

    disconnect(): void {
        this.subscriptions.forEach(s => {
            try { s.unsubscribe(); } catch { /* ignore */ }
        });
        this.subscriptions = [];
        this.client?.deactivate();
        this.client = null;
        this.connectionState.next('DISCONNECTED');
    }

    // ── Filtered event helpers ─────────────────────────────────────────────────

    /**
     * Returns an Observable that emits only events of the given type(s).
     *
     * Example:
     * ```ts
     * this.ws.on('AI_INGEST_PROGRESS', 'AI_INGEST_COMPLETE', 'AI_INGEST_FAILED')
     *   .subscribe(event => { ... });
     * ```
     */
    on<T = unknown>(...types: WebSocketEventType[]): Observable<WebSocketEvent<T>> {
        return this.events$.pipe(
            filter(e => types.includes(e.type))
        ) as Observable<WebSocketEvent<T>>;
    }

    /**
     * Typed shortcut for ingest progress events for a specific fileId.
     *
     * Example:
     * ```ts
     * this.ws.ingestEvents(fileId).subscribe(e => this.progress = e.payload.percent);
     * ```
     */
    // ingestEvents(fileId: string): Observable<WebSocketEvent<IngestProgressPayload>> {
    //     return this.on<IngestProgressPayload>(
    //         'AI_INGEST_STARTED',
    //         'AI_INGEST_PROGRESS',
    //         'AI_INGEST_COMPLETE',
    //         'AI_INGEST_FAILED'
    //     ).pipe(
    //         filter(e => e.payload.fileId === fileId)
    //     );
    // }

    // ── Private ────────────────────────────────────────────────────────────────

    private setupSubscriptions(isAuthenticated: boolean, sessionId: string): void {
        if (!this.client) return;

        // Always subscribe by sessionId — works for anonymous and authenticated users,
        // and correctly isolates multiple tabs of the same user
        const sessionSub = this.client.subscribe(
            `/topic/session/${sessionId}`,
            (msg: IMessage) => this.dispatch(msg)
        );
        this.subscriptions.push(sessionSub);

        // Also subscribe by user principal if authenticated
        if (isAuthenticated) {
            const userSub = this.client.subscribe(
                '/user/queue/events',
                (msg: IMessage) => this.dispatch(msg)
            );
            this.subscriptions.push(userSub);
        }
    }

    private dispatch(msg: IMessage): void {
        try {
            const event: WebSocketEvent = JSON.parse(msg.body);
            this.eventSubject.next(event);
        } catch (e) {
            console.error('Failed to parse WebSocket message', msg.body, e);
        }
    }

    ngOnDestroy(): void {
        this.disconnect();
    }
}