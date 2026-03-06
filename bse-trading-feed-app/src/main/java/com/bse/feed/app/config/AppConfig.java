package com.bse.feed.app.config;

import com.bse.feed.core.engine.InstrumentRegistry;
import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.engine.SequenceTracker;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.gateway.FeedGatewayService;
import com.bse.feed.gateway.decoder.FastMessageDecoder;
import com.bse.feed.gateway.replay.OfflineReplayService;
import com.bse.feed.gateway.tcp.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for all BSE Trading Feed beans.
 * Wires together the core engine, gateway, and lifecycle management.
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    // --- Feed mode ---
    @Value("${bse.feed.mode:offline}")
    private String feedMode; // "live" or "offline"

    // --- UDP Multicast ---
    @Value("${bse.feed.udp.group-a:239.255.190.100}")
    private String udpGroupA;

    @Value("${bse.feed.udp.port-a:30540}")
    private int udpPortA;

    @Value("${bse.feed.udp.group-b:239.255.190.100}")
    private String udpGroupB;

    @Value("${bse.feed.udp.port-b:30541}")
    private int udpPortB;

    @Value("${bse.feed.udp.interface:0.0.0.0}")
    private String udpInterface;

    // --- TCP ---
    @Value("${bse.feed.tcp.snapshot.host:192.168.90.18}")
    private String snapshotHost;

    @Value("${bse.feed.tcp.snapshot.port:540}")
    private int snapshotPort;

    @Value("${bse.feed.tcp.replay.host:192.168.90.18}")
    private String replayHost;

    @Value("${bse.feed.tcp.replay.port:540}")
    private int replayPort;

    @Value("${bse.feed.tcp.username:ESC_FAST}")
    private String username;

    @Value("${bse.feed.tcp.password:Exchange2026!}")
    private String password;

    @Value("${bse.feed.tcp.comp-id:MDG}")
    private String compId;

    // --- Templates ---
    @Value("${bse.feed.templates-path:classpath:templates/FastGWMsgConfig.xml}")
    private String templatesPath;

    // --- Offline ---
    @Value("${bse.feed.offline.data-path:classpath:offline_data.txt}")
    private String offlineDataPath;

    @Value("${bse.feed.offline.delay-ms:50}")
    private long offlineDelayMs;

    // --- Core beans ---

    @Bean
    public MarketDataEventBus marketDataEventBus() {
        return new MarketDataEventBus();
    }

    @Bean
    public SequenceTracker sequenceTracker() {
        return new SequenceTracker();
    }

    @Bean
    public InstrumentRegistry instrumentRegistry(MarketDataEventBus eventBus) {
        InstrumentRegistry registry = new InstrumentRegistry();
        eventBus.addListener(registry);
        return registry;
    }

    @Bean
    public OrderBookManager orderBookManager(MarketDataEventBus eventBus) {
        OrderBookManager manager = new OrderBookManager();
        eventBus.addListener(manager);
        return manager;
    }

    @Bean
    public FastMessageDecoder fastMessageDecoder() {
        FastMessageDecoder decoder = new FastMessageDecoder();
        // The decoder loads templates in initialize() which is called by FeedGatewayService
        return decoder;
    }

    // --- Gateway beans ---

    @Bean
    public FeedGatewayService feedGatewayService(
            FastMessageDecoder decoder,
            MarketDataEventBus eventBus,
            SequenceTracker sequenceTracker,
            OrderBookManager orderBookManager,
            InstrumentRegistry instrumentRegistry) {
        return new FeedGatewayService(
                decoder, eventBus, sequenceTracker,
                orderBookManager, instrumentRegistry,
                udpGroupA, udpPortA, udpGroupB, udpPortB, udpInterface,
                templatesPath
        );
    }

    @Bean
    public OfflineReplayService offlineReplayService(
            FastMessageDecoder decoder,
            MarketDataEventBus eventBus) {
        return new OfflineReplayService(decoder, eventBus);
    }

    @Bean
    public SnapshotService snapshotService(
            FastMessageDecoder decoder,
            MarketDataEventBus eventBus) {
        return new SnapshotService(
                snapshotHost, snapshotPort,
                username, password,
                "1", 2,  // applId, heartbeatInterval
                decoder, eventBus);
    }

    // --- Lifecycle ---

    @Bean
    public CommandLineRunner feedStartup(
            FeedGatewayService feedGateway,
            OfflineReplayService offlineReplay) {
        return args -> {
            log.info("=== BSE Trading Feed Application Starting ===");
            log.info("Feed mode: {}", feedMode);

            if ("live".equalsIgnoreCase(feedMode)) {
                log.info("Starting LIVE feed gateway...");
                log.info("UDP Feed A: {}:{}", udpGroupA, udpPortA);
                log.info("UDP Feed B: {}:{}", udpGroupB, udpPortB);
                feedGateway.initialize();
                feedGateway.start();
            } else {
                log.info("Starting OFFLINE replay mode...");
                feedGateway.initialize(); // Still need decoder initialized
                // Convert classpath: prefix to resource path for getResourceAsStream()
                String resourcePath = offlineDataPath.replace("classpath:", "/");
                if (!resourcePath.startsWith("/")) {
                    resourcePath = "/" + resourcePath;
                }
                offlineReplay.replayFromResource(resourcePath, offlineDelayMs);
                log.info("Offline replay complete. Dashboard available at http://localhost:8080");
            }
        };
    }
}
