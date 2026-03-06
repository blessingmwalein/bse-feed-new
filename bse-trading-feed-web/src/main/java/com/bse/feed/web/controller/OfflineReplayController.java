package com.bse.feed.web.controller;

import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.model.MarketDataEntry;
import com.bse.feed.gateway.decoder.FastMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST controller for offline packet replay and decode testing.
 * Reads offline_data.txt, decodes each FAST packet, and returns
 * detailed decode results — no live connection or logon needed.
 */
@RestController
@RequestMapping("/api/replay")
public class OfflineReplayController {

    private static final Logger log = LoggerFactory.getLogger(OfflineReplayController.class);

    // Pattern to strip [HH:MM:SS] prefix from offline lines
    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("^\\[([^\\]]+)\\]\\s*");

    private final FastMessageDecoder decoder;

    public OfflineReplayController(FastMessageDecoder decoder) {
        this.decoder = decoder;
    }

    // ==================== DTOs ====================

    public static class ReplayResult {
        public int lineNumber;
        public String captureTime;
        public String rawHex;          // first 60 hex chars for display
        public int rawByteCount;
        public boolean success;
        public String error;

        // Decoded fields
        public String eventType;
        public int templateId;
        public String symbol;
        public long sequenceNumber;
        public String applId;
        public String feedSource;
        public int entryCount;
        public List<EntryInfo> entries;
    }

    public static class EntryInfo {
        public String entryType;
        public String updateAction;
        public String subBookType;
        public String symbol;
        public String price;
        public String size;
        public Integer priceLevel;
        public Integer numberOfOrders;
        public String tradeCondition;
        public String mdEntryTime;
        public String msgType;
    }

    public static class ReplaySummary {
        public int totalLines;
        public int totalPackets;       // non-empty, non-comment lines
        public int decoded;
        public int failed;
        public long elapsedMs;
        public Map<String, Integer> eventTypeCounts = new LinkedHashMap<>();
        public Map<String, Integer> symbolCounts = new LinkedHashMap<>();
        public List<ReplayResult> results;
    }

    // ==================== ENDPOINTS ====================

    /**
     * GET /api/replay/run - Decode all packets from offline_data.txt
     * Returns full decode results for each line.
     */
    @GetMapping("/run")
    public ResponseEntity<ReplaySummary> runReplay(
            @RequestParam(defaultValue = "/offline_data.txt") String resourcePath) {

        long startTime = System.currentTimeMillis();
        ReplaySummary summary = new ReplaySummary();
        summary.results = new ArrayList<>();

        // Read lines from classpath resource
        List<String> lines;
        try {
            lines = readResourceLines(resourcePath);
        } catch (IOException e) {
            log.error("Failed to read offline data: {}", e.getMessage());
            summary.totalLines = 0;
            summary.totalPackets = 0;
            summary.elapsedMs = System.currentTimeMillis() - startTime;
            // Return a single error result
            ReplayResult errResult = new ReplayResult();
            errResult.lineNumber = 0;
            errResult.success = false;
            errResult.error = "Failed to read resource: " + resourcePath + " - " + e.getMessage();
            summary.results.add(errResult);
            return ResponseEntity.ok(summary);
        }

        summary.totalLines = lines.size();
        int packetNum = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            packetNum++;
            ReplayResult result = new ReplayResult();
            result.lineNumber = i + 1;

            try {
                // Strip [timestamp] prefix
                String captureTime = null;
                String hexPart = line;

                Matcher m = TIMESTAMP_PREFIX.matcher(line);
                if (m.find()) {
                    captureTime = m.group(1);
                    hexPart = line.substring(m.end());
                }
                result.captureTime = captureTime;

                // Clean hex: remove dashes, spaces, colons
                String hexClean = hexPart.replaceAll("[\\s\\-:]+", "").toUpperCase();
                result.rawHex = hexClean.length() > 60 ? hexClean.substring(0, 60) + "..." : hexClean;

                // Parse hex to bytes
                byte[] data = hexStringToBytes(hexClean);
                result.rawByteCount = data.length;

                // Decode with FAST decoder
                MarketDataEvent event = decoder.decode(data, "REPLAY");

                if (event != null) {
                    result.success = true;
                    result.eventType = event.getEventType().name();
                    result.templateId = event.getTemplateId();
                    result.symbol = event.getSymbol();
                    result.sequenceNumber = event.getApplSeqNum();
                    result.applId = event.getApplId();
                    result.feedSource = event.getFeedSource();
                    result.entryCount = event.getEntries() != null ? event.getEntries().size() : 0;

                    // Map entries
                    result.entries = new ArrayList<>();
                    if (event.getEntries() != null) {
                        for (MarketDataEntry entry : event.getEntries()) {
                            EntryInfo info = new EntryInfo();
                            info.entryType = entry.getEntryType() != null ? entry.getEntryType().name() : entry.getMdEntryTypeRaw();
                            info.updateAction = entry.getUpdateAction() != null ? entry.getUpdateAction().name() : null;
                            info.subBookType = entry.getSubBookType() != null ? entry.getSubBookType().name() : null;
                            info.symbol = entry.getSymbol();
                            info.price = entry.getPrice() != null ? entry.getPrice().toPlainString() : null;
                            info.size = entry.getSize() != null ? entry.getSize().toPlainString() : null;
                            info.priceLevel = entry.getMdPriceLevel();
                            info.numberOfOrders = entry.getNumberOfOrders();
                            info.tradeCondition = entry.getTradeCondition();
                            info.mdEntryTime = entry.getMdEntryTime();
                            info.msgType = entry.getMsgType();
                            result.entries.add(info);
                        }
                    }

                    // Tally per event type
                    summary.eventTypeCounts.merge(result.eventType, 1, Integer::sum);

                    // Tally per symbol
                    if (result.symbol != null) {
                        summary.symbolCounts.merge(result.symbol, 1, Integer::sum);
                    }
                } else {
                    result.success = false;
                    result.error = "Decoder returned null (unknown template or parse failure)";
                }

            } catch (Exception e) {
                result.success = false;
                result.error = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.warn("Replay decode error on line {}: {}", i + 1, e.getMessage());
            }

            summary.results.add(result);
        }

        summary.totalPackets = packetNum;
        summary.decoded = (int) summary.results.stream().filter(r -> r.success).count();
        summary.failed = (int) summary.results.stream().filter(r -> !r.success).count();
        summary.elapsedMs = System.currentTimeMillis() - startTime;

        log.info("Offline replay complete: total={}, decoded={}, failed={}, elapsed={}ms",
                summary.totalPackets, summary.decoded, summary.failed, summary.elapsedMs);

        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/replay/decode - Decode a single hex string (for ad-hoc testing)
     */
    @GetMapping("/decode")
    public ResponseEntity<ReplayResult> decodeSingle(@RequestParam String hex) {
        ReplayResult result = new ReplayResult();
        result.lineNumber = 0;

        try {
            String hexClean = hex.replaceAll("[\\s\\-:]+", "").toUpperCase();
            result.rawHex = hexClean.length() > 60 ? hexClean.substring(0, 60) + "..." : hexClean;

            byte[] data = hexStringToBytes(hexClean);
            result.rawByteCount = data.length;

            MarketDataEvent event = decoder.decode(data, "TEST");

            if (event != null) {
                result.success = true;
                result.eventType = event.getEventType().name();
                result.templateId = event.getTemplateId();
                result.symbol = event.getSymbol();
                result.sequenceNumber = event.getApplSeqNum();
                result.applId = event.getApplId();
                result.entryCount = event.getEntries() != null ? event.getEntries().size() : 0;

                result.entries = new ArrayList<>();
                if (event.getEntries() != null) {
                    for (MarketDataEntry entry : event.getEntries()) {
                        EntryInfo info = new EntryInfo();
                        info.entryType = entry.getEntryType() != null ? entry.getEntryType().name() : entry.getMdEntryTypeRaw();
                        info.updateAction = entry.getUpdateAction() != null ? entry.getUpdateAction().name() : null;
                        info.subBookType = entry.getSubBookType() != null ? entry.getSubBookType().name() : null;
                        info.symbol = entry.getSymbol();
                        info.price = entry.getPrice() != null ? entry.getPrice().toPlainString() : null;
                        info.size = entry.getSize() != null ? entry.getSize().toPlainString() : null;
                        info.priceLevel = entry.getMdPriceLevel();
                        info.numberOfOrders = entry.getNumberOfOrders();
                        info.msgType = entry.getMsgType();
                        result.entries.add(info);
                    }
                }
            } else {
                result.success = false;
                result.error = "Decoder returned null";
            }
        } catch (Exception e) {
            result.success = false;
            result.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/replay/raw - Get raw offline data lines (for viewing the file)
     */
    @GetMapping("/raw")
    public ResponseEntity<Map<String, Object>> getRawLines(
            @RequestParam(defaultValue = "/offline_data.txt") String resourcePath) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            List<String> lines = readResourceLines(resourcePath);
            response.put("path", resourcePath);
            response.put("lineCount", lines.size());
            response.put("lines", lines);
        } catch (IOException e) {
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ==================== HELPERS ====================

    private List<String> readResourceLines(String resourcePath) throws IOException {
        // Try classpath first
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null && !resourcePath.startsWith("/")) {
            is = getClass().getResourceAsStream("/" + resourcePath);
        }
        if (is == null) {
            throw new IOException("Resource not found on classpath: " + resourcePath);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Convert hex string to byte array (Java 17 compatible).
     */
    private static byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Odd-length hex string: " + hex.length());
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
