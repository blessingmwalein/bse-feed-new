package com.bse.feed.core.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks application-level sequence numbers for gap detection.
 * BSE FAST-UDP uses ApplSeqNum for sequencing; Feed A and B carry identical sequences.
 * When a gap is detected (expected != received), recovery via Snapshot/Replay is needed.
 */
public class SequenceTracker {

    private static final Logger log = LoggerFactory.getLogger(SequenceTracker.class);

    /**
     * Callback interface when gaps or duplicates are detected.
     */
    public interface GapListener {
        void onGapDetected(String applId, long expectedSeq, long receivedSeq);
        void onDuplicateDetected(String applId, long seqNum);
    }

    // Per-channel expected sequence: Key = ApplID
    private final ConcurrentMap<String, Long> expectedSequences = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastProcessedSequences = new ConcurrentHashMap<>();

    // Statistics
    private volatile long totalGaps = 0;
    private volatile long totalDuplicates = 0;
    private volatile long totalProcessed = 0;

    private GapListener gapListener;

    public void setGapListener(GapListener listener) {
        this.gapListener = listener;
    }

    /**
     * Process a received sequence number. Returns true if it should be processed
     * (new message), false if it's a duplicate.
     *
     * @param applId The application ID
     * @param seqNum The received ApplSeqNum
     * @return true if this is a new message that should be processed
     */
    public boolean processSequence(String applId, long seqNum) {
        Long expected = expectedSequences.get(applId);

        // First message for this channel
        if (expected == null) {
            expectedSequences.put(applId, seqNum + 1);
            lastProcessedSequences.put(applId, seqNum);
            totalProcessed++;
            log.info("Initialized sequence tracking for ApplID={} at seq={}", applId, seqNum);
            return true;
        }

        if (seqNum == expected) {
            // Normal sequential message
            expectedSequences.put(applId, seqNum + 1);
            lastProcessedSequences.put(applId, seqNum);
            totalProcessed++;
            return true;
        } else if (seqNum > expected) {
            // Gap detected
            long gapSize = seqNum - expected;
            totalGaps++;
            log.warn("GAP DETECTED on ApplID={}: expected={}, received={}, gap_size={}",
                    applId, expected, seqNum, gapSize);

            if (gapListener != null) {
                gapListener.onGapDetected(applId, expected, seqNum);
            }

            // Accept the message and reset expected
            expectedSequences.put(applId, seqNum + 1);
            lastProcessedSequences.put(applId, seqNum);
            totalProcessed++;
            return true;
        } else {
            // Duplicate (seqNum < expected) - from Feed B or retransmission
            totalDuplicates++;
            if (log.isTraceEnabled()) {
                log.trace("Duplicate on ApplID={}: seq={}, expected={}", applId, seqNum, expected);
            }
            if (gapListener != null) {
                gapListener.onDuplicateDetected(applId, seqNum);
            }
            return false;
        }
    }

    /**
     * Reset sequence tracking for a channel (e.g., after recovery).
     */
    public void resetSequence(String applId, long newExpected) {
        expectedSequences.put(applId, newExpected);
        log.info("Reset sequence for ApplID={} to expected={}", applId, newExpected);
    }

    /**
     * Reset all tracking (e.g., on daily reset).
     */
    public void resetAll() {
        expectedSequences.clear();
        lastProcessedSequences.clear();
        totalGaps = 0;
        totalDuplicates = 0;
        totalProcessed = 0;
        log.info("Reset all sequence tracking");
    }

    /**
     * Get the last processed sequence for a channel.
     */
    public Long getLastProcessedSequence(String applId) {
        return lastProcessedSequences.get(applId);
    }

    /**
     * Get the expected next sequence for a channel.
     */
    public Long getExpectedSequence(String applId) {
        return expectedSequences.get(applId);
    }

    // --- Statistics ---

    public long getTotalGaps() { return totalGaps; }
    public long getTotalDuplicates() { return totalDuplicates; }
    public long getTotalProcessed() { return totalProcessed; }

    @Override
    public String toString() {
        return String.format("SequenceTracker{processed=%d, gaps=%d, duplicates=%d, channels=%d}",
                totalProcessed, totalGaps, totalDuplicates, expectedSequences.size());
    }
}
