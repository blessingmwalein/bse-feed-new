package com.bse.feed.gateway.tcp;

import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.gateway.decoder.FastMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages TCP Snapshot channel connections to BSE.
 * Provides connect/disconnect/request capabilities exposed via REST API.
 */
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String applId;
    private final int heartbeatInterval;
    private final FastMessageDecoder decoder;
    private final MarketDataEventBus eventBus;

    private TcpFeedClient client;
    private Thread readerThread;
    private final List<String> connectionLog = new CopyOnWriteArrayList<>();
    private final List<MarketDataEvent> receivedEvents = new CopyOnWriteArrayList<>();

    public SnapshotService(String host, int port, String username, String password,
                           String applId, int heartbeatInterval,
                           FastMessageDecoder decoder, MarketDataEventBus eventBus) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.applId = applId;
        this.heartbeatInterval = heartbeatInterval;
        this.decoder = decoder;
        this.eventBus = eventBus;
    }

    /**
     * Connect to the BSE TCP Snapshot channel.
     * @return status map with connection result
     */
    public Map<String, Object> connect() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (client != null && client.isConnected()) {
            result.put("success", false);
            result.put("message", "Already connected");
            result.put("state", client.getFeedStatus().getState().name());
            return result;
        }

        addLog("Connecting to snapshot channel at " + host + ":" + port + "...");

        try {
            client = new TcpFeedClient(
                    TcpFeedClient.ChannelType.SNAPSHOT,
                    host, port, username, password, applId, heartbeatInterval,
                    decoder, eventBus);

            client.connect();

            addLog("Connected successfully! Logon accepted by BSE.");

            // Start background reader thread
            readerThread = new Thread(this::readLoop, "snapshot-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            result.put("success", true);
            result.put("message", "Connected to snapshot channel at " + host + ":" + port);
            result.put("state", client.getFeedStatus().getState().name());

        } catch (IOException e) {
            addLog("Connection FAILED: " + e.getMessage());
            log.error("Snapshot connection failed: {}", e.getMessage(), e);

            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
            result.put("state", "ERROR");
        }

        return result;
    }

    /**
     * Disconnect from the snapshot channel.
     */
    public Map<String, Object> disconnect() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (client == null || !client.isConnected()) {
            result.put("success", false);
            result.put("message", "Not connected");
            return result;
        }

        client.disconnect();
        addLog("Disconnected from snapshot channel.");

        result.put("success", true);
        result.put("message", "Disconnected");
        result.put("state", "DISCONNECTED");
        return result;
    }

    /**
     * Request a snapshot for specific symbols.
     * @param symbols array of symbols (e.g. "FNBB-EQO")
     */
    public Map<String, Object> requestSnapshot(String... symbols) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (client == null || !client.isConnected()) {
            result.put("success", false);
            result.put("message", "Not connected. Connect first.");
            return result;
        }

        String reqId = "SNAP-" + System.currentTimeMillis();
        addLog("Requesting snapshot for: " + String.join(", ", symbols) + " (reqId=" + reqId + ")");

        try {
            receivedEvents.clear();
            client.sendMarketDataRequest(reqId, symbols);

            // Wait briefly for response(s) to arrive in the reader thread
            Thread.sleep(3000);

            int eventCount = receivedEvents.size();
            addLog("Received " + eventCount + " events from snapshot request.");

            result.put("success", true);
            result.put("message", "Snapshot request sent for " + symbols.length + " symbol(s). Received " + eventCount + " events.");
            result.put("requestId", reqId);
            result.put("symbols", symbols);
            result.put("eventsReceived", eventCount);

        } catch (IOException e) {
            addLog("Snapshot request FAILED: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("success", false);
            result.put("message", "Interrupted while waiting for response");
        }

        return result;
    }

    /**
     * Background reader loop for incoming TCP messages.
     */
    private void readLoop() {
        log.info("Snapshot reader thread started");
        addLog("Reader thread started - listening for responses...");

        try {
            while (client != null && client.isConnected() && client.isRunning()) {
                MarketDataEvent event = client.readNextMessage();
                if (event != null) {
                    receivedEvents.add(event);
                    addLog("Received: " + event.getEventType() + " seq=" + event.getApplSeqNum()
                            + (event.getSymbol() != null ? " symbol=" + event.getSymbol() : ""));

                    // Publish to event bus so order book and instruments get updated
                    eventBus.publish(event);
                }
            }
        } catch (IOException e) {
            if (client != null && client.isConnected()) {
                addLog("Reader error: " + e.getMessage());
                log.error("Snapshot reader error: {}", e.getMessage());
            }
        }

        addLog("Reader thread stopped.");
        log.info("Snapshot reader thread stopped");
    }

    /**
     * Get current snapshot channel status.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        if (client != null) {
            FeedStatus fs = client.getFeedStatus();
            status.put("connected", client.isConnected());
            status.put("state", fs.getState().name());
            status.put("totalMessages", fs.getTotalMessagesReceived());
            status.put("lastMessageTime", fs.getLastMessageTime());
            status.put("connectionTime", fs.getConnectionTime());
            status.put("host", host);
            status.put("port", port);
        } else {
            status.put("connected", false);
            status.put("state", "NOT_INITIALIZED");
            status.put("totalMessages", 0);
            status.put("host", host);
            status.put("port", port);
        }

        status.put("eventsReceived", receivedEvents.size());
        status.put("log", getRecentLog(20));
        return status;
    }

    /**
     * Get the connection log (last N entries).
     */
    public List<String> getRecentLog(int maxEntries) {
        int size = connectionLog.size();
        if (size <= maxEntries) return new ArrayList<>(connectionLog);
        return new ArrayList<>(connectionLog.subList(size - maxEntries, size));
    }

    private void addLog(String message) {
        String entry = Instant.now().toString().substring(11, 19) + " " + message;
        connectionLog.add(entry);
        log.info("[Snapshot] {}", message);
        // Keep log bounded
        while (connectionLog.size() > 200) {
            connectionLog.remove(0);
        }
    }
}
