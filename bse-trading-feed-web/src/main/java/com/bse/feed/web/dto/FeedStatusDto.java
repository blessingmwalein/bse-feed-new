package com.bse.feed.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for feed status information displayed on the dashboard.
 */
public class FeedStatusDto {

    private String channelName;
    private String state;
    private Instant lastMessageTime;
    private Instant connectionTime;
    private long totalMessages;
    private long totalGaps;
    private long totalDuplicates;
    private long lastSequence;
    private long heartbeats;
    private long snapshots;
    private long incrementals;
    private long securityDefinitions;
    private long decodeErrors;
    private String lastError;
    private double avgDecodeTimeMs;
    private long maxDecodeTimeMs;

    // System stats
    private int instrumentCount;
    private int orderBookCount;
    private long uptimeSeconds;
    private List<String> activeSymbols;

    // --- Getters and Setters ---

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Instant getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Instant lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public Instant getConnectionTime() { return connectionTime; }
    public void setConnectionTime(Instant connectionTime) { this.connectionTime = connectionTime; }
    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
    public long getTotalGaps() { return totalGaps; }
    public void setTotalGaps(long totalGaps) { this.totalGaps = totalGaps; }
    public long getTotalDuplicates() { return totalDuplicates; }
    public void setTotalDuplicates(long totalDuplicates) { this.totalDuplicates = totalDuplicates; }
    public long getLastSequence() { return lastSequence; }
    public void setLastSequence(long lastSequence) { this.lastSequence = lastSequence; }
    public long getHeartbeats() { return heartbeats; }
    public void setHeartbeats(long heartbeats) { this.heartbeats = heartbeats; }
    public long getSnapshots() { return snapshots; }
    public void setSnapshots(long snapshots) { this.snapshots = snapshots; }
    public long getIncrementals() { return incrementals; }
    public void setIncrementals(long incrementals) { this.incrementals = incrementals; }
    public long getSecurityDefinitions() { return securityDefinitions; }
    public void setSecurityDefinitions(long securityDefinitions) { this.securityDefinitions = securityDefinitions; }
    public long getDecodeErrors() { return decodeErrors; }
    public void setDecodeErrors(long decodeErrors) { this.decodeErrors = decodeErrors; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public double getAvgDecodeTimeMs() { return avgDecodeTimeMs; }
    public void setAvgDecodeTimeMs(double avgDecodeTimeMs) { this.avgDecodeTimeMs = avgDecodeTimeMs; }
    public long getMaxDecodeTimeMs() { return maxDecodeTimeMs; }
    public void setMaxDecodeTimeMs(long maxDecodeTimeMs) { this.maxDecodeTimeMs = maxDecodeTimeMs; }
    public int getInstrumentCount() { return instrumentCount; }
    public void setInstrumentCount(int instrumentCount) { this.instrumentCount = instrumentCount; }
    public int getOrderBookCount() { return orderBookCount; }
    public void setOrderBookCount(int orderBookCount) { this.orderBookCount = orderBookCount; }
    public long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    public List<String> getActiveSymbols() { return activeSymbols; }
    public void setActiveSymbols(List<String> activeSymbols) { this.activeSymbols = activeSymbols; }
}
