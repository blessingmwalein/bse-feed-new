package com.bse.feed.gateway.udp;

import com.bse.feed.core.engine.SequenceTracker;
import com.bse.feed.core.event.ActivityLog;
import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.gateway.decoder.FastMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.time.Instant;

/**
 * UDP Multicast listener for BSE FAST-UDP market data feed.
 * Supports Feed A and Feed B with automatic duplicate detection.
 *
 * BSE Configuration:
 *   Test:       239.255.190.100:30540 (Feed A), port 30541 (Feed B)
 *   Production: 239.255.181.100:5540 (Feed A), 239.255.181.101:5541 (Feed B)
 */
public class UdpMulticastReceiver implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(UdpMulticastReceiver.class);
    private static final int BUFFER_SIZE = 65536;  // Max UDP packet size

    private final String feedName;           // "FeedA" or "FeedB"
    private final String multicastGroup;     // e.g. "239.255.190.100"
    private final int port;                  // e.g. 30540
    private final String networkInterface;   // e.g. "eth0" or null for default

    private final FastMessageDecoder decoder;
    private final MarketDataEventBus eventBus;
    private final SequenceTracker sequenceTracker;
    private final FeedStatus feedStatus;
    private ActivityLog activityLog;

    private volatile boolean running = false;
    private DatagramChannel channel;
    private MembershipKey membershipKey;

    public UdpMulticastReceiver(String feedName, String multicastGroup, int port,
                                 String networkInterface, FastMessageDecoder decoder,
                                 MarketDataEventBus eventBus, SequenceTracker sequenceTracker) {
        this.feedName = feedName;
        this.multicastGroup = multicastGroup;
        this.port = port;
        this.networkInterface = networkInterface;
        this.decoder = decoder;
        this.eventBus = eventBus;
        this.sequenceTracker = sequenceTracker;
        this.feedStatus = new FeedStatus("UDP-" + feedName);
    }

    public void setActivityLog(ActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    /**
     * Start receiving multicast data in the current thread.
     */
    @Override
    public void run() {
        log.info("Starting UDP multicast receiver: {} on {}:{}", feedName, multicastGroup, port);
        running = true;

        try {
            setupChannel();
            feedStatus.setState(FeedStatus.ConnectionState.CONNECTED);
            feedStatus.setConnectionTime(Instant.now());

            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            while (running) {
                buffer.clear();
                SocketAddress source = channel.receive(buffer);

                if (source != null) {
                    buffer.flip();
                    int length = buffer.remaining();

                    byte[] data = new byte[length];
                    buffer.get(data);

                    feedStatus.setLastMessageTime(Instant.now());
                    feedStatus.incrementMessagesReceived();
                    feedStatus.setState(FeedStatus.ConnectionState.RECEIVING);

                    processPacket(data);
                }
            }
        } catch (IOException e) {
            if (running) {
                log.error("UDP receiver {} error: {}", feedName, e.getMessage(), e);
                feedStatus.setState(FeedStatus.ConnectionState.ERROR);
                feedStatus.setLastError(e.getMessage());
                feedStatus.setLastErrorTime(Instant.now());
            }
        } finally {
            cleanup();
        }

        log.info("UDP multicast receiver {} stopped", feedName);
    }

    /**
     * Set up the NIO multicast channel.
     */
    private void setupChannel() throws IOException {
        // Find appropriate network interface
        NetworkInterface ni = null;
        if (networkInterface != null && !networkInterface.isEmpty()
                && !"0.0.0.0".equals(networkInterface)) {
            ni = NetworkInterface.getByName(networkInterface);
            if (ni == null) {
                ni = NetworkInterface.getByInetAddress(InetAddress.getByName(networkInterface));
            }
            if (ni == null) {
                log.warn("Configured interface '{}' not found, falling back to auto-detect", networkInterface);
            }
        }

        // Auto-detect: find first non-loopback, multicast-capable interface
        if (ni == null) {
            ni = findDefaultInterface();
        }

        if (ni == null) {
            logAvailableInterfaces();
            throw new IOException("No suitable network interface found for multicast");
        }

        log.info("Using network interface: {} ({})", ni.getName(), ni.getDisplayName());

        InetAddress groupAddr = InetAddress.getByName(multicastGroup);

        channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
        channel.bind(new InetSocketAddress(port));

        membershipKey = channel.join(groupAddr, ni);

        log.info("Joined multicast group {}:{} on interface {}",
                multicastGroup, port, ni.getName());
    }

    /**
     * Find the first suitable non-loopback, up, multicast-capable interface.
     * Falls back to any up non-loopback IPv4 interface if none report multicast support.
     */
    private NetworkInterface findDefaultInterface() throws SocketException {
        NetworkInterface fallback = null;
        var interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (!ni.isUp() || ni.isLoopback()) continue;

            boolean hasIpv4 = false;
            var addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement() instanceof Inet4Address) {
                    hasIpv4 = true;
                    break;
                }
            }

            if (hasIpv4) {
                if (ni.supportsMulticast()) {
                    log.info("Auto-detected multicast interface: {} ({})", ni.getName(), ni.getDisplayName());
                    return ni;
                }
                if (fallback == null) {
                    fallback = ni;
                }
            }
        }

        // On Windows, supportsMulticast() can return false even when multicast works
        if (fallback != null) {
            log.warn("No interface reports multicast support; using fallback: {} ({})",
                    fallback.getName(), fallback.getDisplayName());
        }
        return fallback;
    }

    /**
     * Log all available network interfaces for diagnostic purposes.
     */
    private void logAvailableInterfaces() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            log.error("Available network interfaces:");
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                var addrs = ni.getInetAddresses();
                StringBuilder addrStr = new StringBuilder();
                while (addrs.hasMoreElements()) {
                    if (addrStr.length() > 0) addrStr.append(", ");
                    addrStr.append(addrs.nextElement().getHostAddress());
                }
                log.error("  {} ({}) - up={}, loopback={}, multicast={}, addrs=[{}]",
                        ni.getName(), ni.getDisplayName(),
                        ni.isUp(), ni.isLoopback(), ni.supportsMulticast(), addrStr);
            }
        } catch (SocketException e) {
            log.error("Failed to enumerate network interfaces", e);
        }
    }

    /**
     * Process a received UDP packet containing one or more FAST messages.
     */
    private void processPacket(byte[] data) {
        long startNanos = System.nanoTime();
        try {
            String feedSource = feedName.contains("A") ? "A" : "B";
            MarketDataEvent event = decoder.decode(data, feedSource);

            if (event == null) {
                feedStatus.incrementDecodeErrors();
                if (activityLog != null) {
                    activityLog.warn(feedName, "DECODE_NULL",
                            "Packet decoded to null (unhandled template), " + data.length + " bytes");
                }
                return;
            }

            // Sequence check (Feed A/B dedup)
            boolean shouldProcess = sequenceTracker.processSequence(
                    event.getApplId(), event.getApplSeqNum());

            if (!shouldProcess) {
                feedStatus.incrementDuplicates();
                if (activityLog != null) {
                    activityLog.info(feedName, "DUPLICATE",
                            "Duplicate seq=" + event.getApplSeqNum() + " (already processed)");
                }
                return;
            }

            // Update feed status counters and log
            switch (event.getEventType()) {
                case HEARTBEAT -> {
                    feedStatus.incrementHeartbeats();
                    if (activityLog != null) {
                        activityLog.info(feedName, "HEARTBEAT",
                                "Heartbeat seq=" + event.getApplSeqNum()
                                        + " template=" + event.getTemplateId());
                    }
                }
                case SNAPSHOT -> {
                    feedStatus.incrementSnapshots();
                    if (activityLog != null) {
                        activityLog.info(feedName, "SNAPSHOT",
                                "Snapshot seq=" + event.getApplSeqNum()
                                        + " symbol=" + event.getSymbol()
                                        + " entries=" + (event.getEntries() != null ? event.getEntries().size() : 0));
                    }
                }
                case INCREMENTAL_REFRESH -> {
                    feedStatus.incrementIncrementals();
                    if (activityLog != null) {
                        activityLog.info(feedName, "INCREMENTAL",
                                "Incremental seq=" + event.getApplSeqNum()
                                        + " symbol=" + event.getSymbol()
                                        + " entries=" + (event.getEntries() != null ? event.getEntries().size() : 0));
                    }
                }
                case SECURITY_DEFINITION -> {
                    feedStatus.incrementSecurityDefinitions();
                    if (activityLog != null) {
                        activityLog.info(feedName, "SEC_DEF",
                                "SecurityDefinition seq=" + event.getApplSeqNum()
                                        + " symbol=" + event.getSymbol());
                    }
                }
                case SECURITY_STATUS -> {
                    feedStatus.incrementTradingStatuses();
                    if (activityLog != null) {
                        activityLog.info(feedName, "SEC_STATUS",
                                "SecurityStatus seq=" + event.getApplSeqNum()
                                        + " symbol=" + event.getSymbol());
                    }
                }
                default -> {
                    if (activityLog != null) {
                        activityLog.info(feedName, event.getEventType().name(),
                                "type=" + event.getEventType() + " seq=" + event.getApplSeqNum());
                    }
                }
            }

            feedStatus.setLastSequenceNumber(event.getApplSeqNum());

            // Publish to event bus
            eventBus.publish(event);

        } catch (Exception e) {
            feedStatus.incrementDecodeErrors();
            feedStatus.setLastError(e.getMessage());
            feedStatus.setLastErrorTime(Instant.now());
            if (activityLog != null) {
                activityLog.error(feedName, "ERROR", e.getMessage());
            }
            log.error("Error processing packet on {}: {}", feedName, e.getMessage(), e);
        } finally {
            long decodeTimeNanos = System.nanoTime() - startNanos;
            double decodeTimeMs = decodeTimeNanos / 1_000_000.0;
            // Simple running average
            double avg = feedStatus.getAvgDecodeTimeMs();
            feedStatus.setAvgDecodeTimeMs(avg * 0.99 + decodeTimeMs * 0.01);
            if (decodeTimeNanos / 1_000_000 > feedStatus.getMaxDecodeTimeMs()) {
                feedStatus.setMaxDecodeTimeMs(decodeTimeNanos / 1_000_000);
            }
        }
    }

    /**
     * Stop the receiver gracefully.
     */
    public void stop() {
        running = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (membershipKey != null) {
                membershipKey.drop();
                membershipKey = null;
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            feedStatus.setState(FeedStatus.ConnectionState.DISCONNECTED);
        } catch (IOException e) {
            log.warn("Error closing channel: {}", e.getMessage());
        }
    }

    public boolean isRunning() { return running; }
    public FeedStatus getFeedStatus() { return feedStatus; }
    public String getFeedName() { return feedName; }
}
