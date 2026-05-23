package com.smartdispatch.tracking.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks WebSocket session lifecycle.
 * Useful for monitoring active connections and debugging.
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final AtomicInteger activeConnections = new AtomicInteger(0);

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        int count = activeConnections.incrementAndGet();
        log.info("WebSocket connected. Active sessions: {}", count);
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        int count = activeConnections.decrementAndGet();
        log.info("WebSocket disconnected. Active sessions: {}", count);
    }

    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
}
