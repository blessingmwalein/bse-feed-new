package com.bse.feed.gateway.replay;

import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.gateway.decoder.FastMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replays captured FAST packets from offline_data.txt for testing.
 * Each line in the file is a hex-encoded FAST packet.
 *
 * Usage:
 *   OfflineReplayService replay = new OfflineReplayService(decoder, eventBus);
 *   replay.replayFromFile(Path.of("offline_data.txt"));
 */
public class OfflineReplayService {

    private static final Logger log = LoggerFactory.getLogger(OfflineReplayService.class);

    // Pattern to strip [HH:MM:SS] prefix from offline lines
    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("^\\[([^\\]]+)\\]\\s*");

    private final FastMessageDecoder decoder;
    private final MarketDataEventBus eventBus;

    // Statistics
    private int totalPackets = 0;
    private int decodedPackets = 0;
    private int failedPackets = 0;

    public OfflineReplayService(FastMessageDecoder decoder, MarketDataEventBus eventBus) {
        this.decoder = decoder;
        this.eventBus = eventBus;
    }

    /**
     * Replay packets from a file on disk.
     *
     * @param filePath Path to the offline data file
     * @param delayMs  Delay between packets in milliseconds (0 = no delay)
     * @return Number of successfully decoded packets
     */
    public int replayFromFile(Path filePath, long delayMs) throws IOException {
        log.info("Starting offline replay from: {}", filePath);
        List<String> lines = Files.readAllLines(filePath);
        return replayLines(lines, delayMs);
    }

    /**
     * Replay packets from a classpath resource.
     *
     * @param resourcePath Resource path (e.g., "/offline_data.txt")
     * @param delayMs      Delay between packets
     * @return Number of successfully decoded packets
     */
    public int replayFromResource(String resourcePath, long delayMs) throws IOException {
        log.info("Starting offline replay from resource: {}", resourcePath);

        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return replayLines(lines, delayMs);
    }

    /**
     * Core replay logic: decode hex lines and publish events.
     */
    private int replayLines(List<String> lines, long delayMs) {
        totalPackets = 0;
        decodedPackets = 0;
        failedPackets = 0;

        HexFormat hexFormat = HexFormat.of();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue;  // Skip empty lines and comments
            }

            totalPackets++;

            try {
                // Strip [timestamp] prefix if present
                String hexPart = trimmed;
                Matcher m = TIMESTAMP_PREFIX.matcher(trimmed);
                if (m.find()) {
                    hexPart = trimmed.substring(m.end());
                }

                // Remove any spaces, dashes, or colons from hex string
                String hexClean = hexPart.replaceAll("[\\s:-]", "");
                byte[] data = hexFormat.parseHex(hexClean);

                MarketDataEvent event = decoder.decode(data, "REPLAY");

                if (event != null) {
                    decodedPackets++;
                    eventBus.publish(event);

                    if (log.isDebugEnabled()) {
                        log.debug("Replayed packet {}: {} seq={} symbol={}",
                                totalPackets, event.getEventType(),
                                event.getApplSeqNum(), event.getSymbol());
                    }
                } else {
                    failedPackets++;
                    log.warn("Failed to decode packet {}: first bytes = {}",
                            totalPackets, hexClean.substring(0, Math.min(20, hexClean.length())));
                }

                // Optional delay for simulating real-time
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }

            } catch (IllegalArgumentException e) {
                failedPackets++;
                log.warn("Invalid hex in line {}: {}", totalPackets, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Replay interrupted at packet {}", totalPackets);
                break;
            } catch (Exception e) {
                failedPackets++;
                log.error("Error replaying packet {}: {}", totalPackets, e.getMessage(), e);
            }
        }

        log.info("Offline replay complete: total={}, decoded={}, failed={}",
                totalPackets, decodedPackets, failedPackets);
        return decodedPackets;
    }

    public int getTotalPackets() { return totalPackets; }
    public int getDecodedPackets() { return decodedPackets; }
    public int getFailedPackets() { return failedPackets; }
}
