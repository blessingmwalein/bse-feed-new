package com.bse.feed.core.event;

import com.bse.feed.core.model.MarketDataEntry;

import java.util.List;

/**
 * Event carrying decoded market data entries from the FAST decoder
 * to the order book engine and other consumers.
 */
public class MarketDataEvent {

    public enum EventType {
        HEARTBEAT,
        SNAPSHOT,              // W - Full snapshot
        INCREMENTAL_REFRESH,   // X - Incremental update
        SECURITY_DEFINITION,   // d - Instrument definition
        SECURITY_STATUS,       // f - Trading status change
        NEWS,                  // B - News/text
        TRADING_SESSION_STATUS // h - Session status
    }

    private final EventType eventType;
    private final List<MarketDataEntry> entries;
    private final long applSeqNum;
    private final String applId;
    private final int templateId;
    private final String feedSource;   // "A" or "B" (for duplicate detection)
    private final long receiveTimeNanos;

    public MarketDataEvent(EventType eventType, List<MarketDataEntry> entries,
                           long applSeqNum, String applId, int templateId, String feedSource) {
        this.eventType = eventType;
        this.entries = entries;
        this.applSeqNum = applSeqNum;
        this.applId = applId;
        this.templateId = templateId;
        this.feedSource = feedSource;
        this.receiveTimeNanos = System.nanoTime();
    }

    public EventType getEventType() { return eventType; }
    public List<MarketDataEntry> getEntries() { return entries; }
    public long getApplSeqNum() { return applSeqNum; }
    public String getApplId() { return applId; }
    public int getTemplateId() { return templateId; }
    public String getFeedSource() { return feedSource; }
    public long getReceiveTimeNanos() { return receiveTimeNanos; }

    /**
     * Helper to get the first entry's symbol (all entries in a message share the same symbol).
     */
    public String getSymbol() {
        if (entries != null && !entries.isEmpty()) {
            return entries.get(0).getSymbol();
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("MarketDataEvent{type=%s, seq=%d, entries=%d, symbol=%s, feed=%s}",
                eventType, applSeqNum, entries != null ? entries.size() : 0, getSymbol(), feedSource);
    }
}
