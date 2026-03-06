package com.bse.feed.core.model;

import java.time.Instant;

/**
 * Represents a heartbeat message from the BSE FAST-UDP gateway.
 * Heartbeats are sent every 2 seconds per channel to indicate liveness.
 * Template ID: 0 (Heartbeat)
 */
public class HeartbeatMessage {

    private String applId;
    private long applSeqNum;  // Last ApplSeqNum processed
    private Instant receivedAt;

    public HeartbeatMessage() {
        this.receivedAt = Instant.now();
    }

    public HeartbeatMessage(String applId, long applSeqNum) {
        this.applId = applId;
        this.applSeqNum = applSeqNum;
        this.receivedAt = Instant.now();
    }

    public String getApplId() { return applId; }
    public void setApplId(String applId) { this.applId = applId; }

    public long getApplSeqNum() { return applSeqNum; }
    public void setApplSeqNum(long applSeqNum) { this.applSeqNum = applSeqNum; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    @Override
    public String toString() {
        return String.format("Heartbeat{applId=%s, seq=%d, at=%s}", applId, applSeqNum, receivedAt);
    }
}
