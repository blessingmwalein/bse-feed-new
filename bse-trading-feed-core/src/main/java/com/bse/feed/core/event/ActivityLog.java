package com.bse.feed.core.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe bounded activity log for recording feed events.
 * Keeps the most recent N entries in a ring-buffer style structure.
 * Used to display live feed activity on the web dashboard.
 */
public class ActivityLog {

    /**
     * A single log entry.
     */
    public static class Entry {
        private final long id;
        private final String timestamp;
        private final String source;   // "FeedA", "FeedB", "TCP-SNAPSHOT", etc.
        private final String type;     // "HEARTBEAT", "SNAPSHOT", "INCREMENTAL", etc.
        private final String detail;   // Human-readable detail
        private final String level;    // "info", "warn", "error"

        public Entry(long id, String timestamp, String source, String type, String detail, String level) {
            this.id = id;
            this.timestamp = timestamp;
            this.source = source;
            this.type = type;
            this.detail = detail;
            this.level = level;
        }

        public long getId() { return id; }
        public String getTimestamp() { return timestamp; }
        public String getSource() { return source; }
        public String getType() { return type; }
        public String getDetail() { return detail; }
        public String getLevel() { return level; }
    }

    private final int maxEntries;
    private final ConcurrentLinkedDeque<Entry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    public ActivityLog(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Add an info-level entry.
     */
    public void info(String source, String type, String detail) {
        add(source, type, detail, "info");
    }

    /**
     * Add a warning-level entry.
     */
    public void warn(String source, String type, String detail) {
        add(source, type, detail, "warn");
    }

    /**
     * Add an error-level entry.
     */
    public void error(String source, String type, String detail) {
        add(source, type, detail, "error");
    }

    private void add(String source, String type, String detail, String level) {
        String ts = Instant.now().toString().substring(11, 23); // HH:mm:ss.SSS
        Entry entry = new Entry(idCounter.incrementAndGet(), ts, source, type, detail, level);
        entries.addLast(entry);

        // Trim if over capacity
        while (entries.size() > maxEntries) {
            entries.pollFirst();
        }
    }

    /**
     * Get all entries (newest last).
     */
    public List<Entry> getAll() {
        return new ArrayList<>(entries);
    }

    /**
     * Get entries with id greater than the given id (for incremental polling).
     */
    public List<Entry> getSince(long afterId) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries) {
            if (e.getId() > afterId) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Get the most recent N entries.
     */
    public List<Entry> getRecent(int count) {
        List<Entry> all = getAll();
        if (all.size() <= count) return all;
        return all.subList(all.size() - count, all.size());
    }

    /**
     * Get the latest entry id (for incremental polling).
     */
    public long getLatestId() {
        return idCounter.get();
    }

    public int size() {
        return entries.size();
    }
}
