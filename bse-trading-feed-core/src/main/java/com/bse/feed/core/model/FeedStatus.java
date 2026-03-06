package com.bse.feed.core.model;

import java.time.Instant;

/**
 * Represents a feed status event used for monitoring.
 * Tracks connection state, message counts, gap events, etc.
 */
public class FeedStatus {

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECEIVING,
        GAP_DETECTED,
        RECOVERING,
        ERROR
    }

    private String channelName;            // "UDP-FeedA", "UDP-FeedB", "TCP-Snapshot", etc.
    private ConnectionState state;
    private Instant lastMessageTime;
    private Instant connectionTime;

    // Sequence tracking
    private long lastSequenceNumber;
    private long expectedSequenceNumber;
    private long totalMessagesReceived;
    private long totalGapsDetected;
    private long totalDuplicatesSkipped;

    // Message type counters
    private long heartbeatCount;
    private long snapshotCount;
    private long incrementalCount;
    private long securityDefinitionCount;
    private long tradingStatusCount;

    // Error tracking
    private long decodeErrorCount;
    private String lastError;
    private Instant lastErrorTime;

    // Performance
    private double avgDecodeTimeMs;
    private long maxDecodeTimeMs;

    public FeedStatus() {
        this.state = ConnectionState.DISCONNECTED;
    }

    public FeedStatus(String channelName) {
        this.channelName = channelName;
        this.state = ConnectionState.DISCONNECTED;
    }

    // --- Getters and Setters ---

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public ConnectionState getState() { return state; }
    public void setState(ConnectionState state) { this.state = state; }

    public Instant getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Instant lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Instant getConnectionTime() { return connectionTime; }
    public void setConnectionTime(Instant connectionTime) { this.connectionTime = connectionTime; }

    public long getLastSequenceNumber() { return lastSequenceNumber; }
    public void setLastSequenceNumber(long lastSequenceNumber) { this.lastSequenceNumber = lastSequenceNumber; }

    public long getExpectedSequenceNumber() { return expectedSequenceNumber; }
    public void setExpectedSequenceNumber(long expectedSequenceNumber) { this.expectedSequenceNumber = expectedSequenceNumber; }

    public long getTotalMessagesReceived() { return totalMessagesReceived; }
    public void setTotalMessagesReceived(long totalMessagesReceived) { this.totalMessagesReceived = totalMessagesReceived; }

    public long getTotalGapsDetected() { return totalGapsDetected; }
    public void setTotalGapsDetected(long totalGapsDetected) { this.totalGapsDetected = totalGapsDetected; }

    public long getTotalDuplicatesSkipped() { return totalDuplicatesSkipped; }
    public void setTotalDuplicatesSkipped(long totalDuplicatesSkipped) { this.totalDuplicatesSkipped = totalDuplicatesSkipped; }

    public long getHeartbeatCount() { return heartbeatCount; }
    public void setHeartbeatCount(long heartbeatCount) { this.heartbeatCount = heartbeatCount; }

    public long getSnapshotCount() { return snapshotCount; }
    public void setSnapshotCount(long snapshotCount) { this.snapshotCount = snapshotCount; }

    public long getIncrementalCount() { return incrementalCount; }
    public void setIncrementalCount(long incrementalCount) { this.incrementalCount = incrementalCount; }

    public long getSecurityDefinitionCount() { return securityDefinitionCount; }
    public void setSecurityDefinitionCount(long securityDefinitionCount) { this.securityDefinitionCount = securityDefinitionCount; }

    public long getTradingStatusCount() { return tradingStatusCount; }
    public void setTradingStatusCount(long tradingStatusCount) { this.tradingStatusCount = tradingStatusCount; }

    public long getDecodeErrorCount() { return decodeErrorCount; }
    public void setDecodeErrorCount(long decodeErrorCount) { this.decodeErrorCount = decodeErrorCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getLastErrorTime() { return lastErrorTime; }
    public void setLastErrorTime(Instant lastErrorTime) { this.lastErrorTime = lastErrorTime; }

    public double getAvgDecodeTimeMs() { return avgDecodeTimeMs; }
    public void setAvgDecodeTimeMs(double avgDecodeTimeMs) { this.avgDecodeTimeMs = avgDecodeTimeMs; }

    public long getMaxDecodeTimeMs() { return maxDecodeTimeMs; }
    public void setMaxDecodeTimeMs(long maxDecodeTimeMs) { this.maxDecodeTimeMs = maxDecodeTimeMs; }

    // --- Convenience methods ---

    public void incrementMessagesReceived() { totalMessagesReceived++; }
    public void incrementGaps() { totalGapsDetected++; }
    public void incrementDuplicates() { totalDuplicatesSkipped++; }
    public void incrementDecodeErrors() { decodeErrorCount++; }
    public void incrementHeartbeats() { heartbeatCount++; }
    public void incrementSnapshots() { snapshotCount++; }
    public void incrementIncrementals() { incrementalCount++; }
    public void incrementSecurityDefinitions() { securityDefinitionCount++; }
    public void incrementTradingStatuses() { tradingStatusCount++; }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED || state == ConnectionState.RECEIVING;
    }

    public boolean isReceiving() {
        return state == ConnectionState.RECEIVING;
    }

    @Override
    public String toString() {
        return String.format("FeedStatus{%s state=%s msgs=%d seq=%d gaps=%d errors=%d}",
                channelName, state, totalMessagesReceived, lastSequenceNumber,
                totalGapsDetected, decodeErrorCount);
    }
}
