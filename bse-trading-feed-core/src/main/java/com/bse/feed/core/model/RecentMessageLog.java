package com.bse.feed.core.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe circular buffer that stores recent decoded message summaries.
 * Used to give dashboard visibility into exactly what the gateway is receiving and decoding.
 */
public class RecentMessageLog {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final int maxEntries;
    private final ConcurrentLinkedDeque<LogEntry> entries = new ConcurrentLinkedDeque<>();

    public RecentMessageLog(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Add a message to the log. Oldest entries are pruned automatically.
     */
    public void add(String feed, String type, int templateId, String applId,
                    long seqNum, String symbol, String details) {
        LogEntry entry = new LogEntry(
                Instant.now(), feed, type, templateId, applId, seqNum, symbol, details);
        entries.addFirst(entry);

        // Prune if over capacity
        while (entries.size() > maxEntries) {
            entries.pollLast();
        }
    }

    /**
     * Get the most recent N entries (newest first).
     */
    public List<LogEntry> getRecent(int count) {
        List<LogEntry> result = new ArrayList<>();
        int n = 0;
        for (LogEntry e : entries) {
            if (n++ >= count) break;
            result.add(e);
        }
        return result;
    }

    /**
     * Get all stored entries (newest first).
     */
    public List<LogEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * A single log entry representing one decoded message.
     */
    public static class LogEntry {
        private final Instant timestamp;
        private final String feed;       // "FeedA" or "FeedB"
        private final String type;       // "HEARTBEAT", "SNAPSHOT", etc.
        private final int templateId;
        private final String applId;
        private final long seqNum;
        private final String symbol;     // null for heartbeats
        private final String details;    // Human-readable extra info

        public LogEntry(Instant timestamp, String feed, String type, int templateId,
                        String applId, long seqNum, String symbol, String details) {
            this.timestamp = timestamp;
            this.feed = feed;
            this.type = type;
            this.templateId = templateId;
            this.applId = applId;
            this.seqNum = seqNum;
            this.symbol = symbol;
            this.details = details;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getTimestampFormatted() { return TIME_FMT.format(timestamp); }
        public String getFeed() { return feed; }
        public String getType() { return type; }
        public int getTemplateId() { return templateId; }
        public String getApplId() { return applId; }
        public long getSeqNum() { return seqNum; }
        public String getSymbol() { return symbol; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("[%s] %s %s tpl=%d applId=%s seq=%d sym=%s %s",
                    TIME_FMT.format(timestamp), feed, type, templateId,
                    applId, seqNum, symbol, details);
        }
    }
}
