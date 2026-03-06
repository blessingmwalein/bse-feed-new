package com.bse.feed.gateway;

import com.bse.feed.core.engine.InstrumentRegistry;
import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.engine.SequenceTracker;
import com.bse.feed.core.event.ActivityLog;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.gateway.decoder.FastMessageDecoder;
import com.bse.feed.gateway.udp.UdpMulticastReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main gateway service that orchestrates:
 * - FAST template loading
 * - UDP multicast receivers (Feed A + B)
 * - Sequence tracking and gap detection
 * - Event distribution to order book engine
 *
 * Lifecycle: initialize() -> start() -> stop()
 */
public class FeedGatewayService {

    private static final Logger log = LoggerFactory.getLogger(FeedGatewayService.class);

    // Configuration
    private String templatePath = "/templates/FastGWMsgConfig.xml";
    private String multicastGroupFeedA = "239.255.190.100";
    private int portFeedA = 30540;
    private String multicastGroupFeedB = "239.255.190.100";
    private int portFeedB = 30541;
    private String networkInterface = null;

    // Components (can be injected or internally created)
    private FastMessageDecoder decoder;
    private MarketDataEventBus eventBus;
    private SequenceTracker sequenceTracker;
    private OrderBookManager orderBookManager;
    private InstrumentRegistry instrumentRegistry;

    // UDP receivers
    private UdpMulticastReceiver feedAReceiver;
    private UdpMulticastReceiver feedBReceiver;
    private Thread feedAThread;
    private Thread feedBThread;

    private ActivityLog activityLog;
    private volatile boolean running = false;

    /** No-arg constructor - components will be created in initialize(). */
    public FeedGatewayService() {}

    /**
     * Constructor for Spring dependency injection.
     * Components are externally managed; initialize() still loads templates and wires UDP.
     */
    public FeedGatewayService(
            FastMessageDecoder decoder,
            MarketDataEventBus eventBus,
            SequenceTracker sequenceTracker,
            OrderBookManager orderBookManager,
            InstrumentRegistry instrumentRegistry,
            String multicastGroupFeedA, int portFeedA,
            String multicastGroupFeedB, int portFeedB,
            String networkInterface,
            String templatePath) {
        this.decoder = decoder;
        this.eventBus = eventBus;
        this.sequenceTracker = sequenceTracker;
        this.orderBookManager = orderBookManager;
        this.instrumentRegistry = instrumentRegistry;
        this.multicastGroupFeedA = multicastGroupFeedA;
        this.portFeedA = portFeedA;
        this.multicastGroupFeedB = multicastGroupFeedB;
        this.portFeedB = portFeedB;
        this.networkInterface = networkInterface;
        this.templatePath = templatePath;
    }

    /**
     * Initialize components. If injected via constructor, only loads templates and wires UDP.
     * If using no-arg constructor, creates all components from scratch.
     */
    public void initialize() {
        log.info("Initializing Feed Gateway Service...");

        // Create components if not injected
        if (decoder == null) {
            decoder = new FastMessageDecoder();
        }
        // Convert classpath: prefix to resource path for getResourceAsStream()
        String resolvedTemplatePath = templatePath.replace("classpath:", "/");
        if (!resolvedTemplatePath.startsWith("/")) {
            resolvedTemplatePath = "/" + resolvedTemplatePath;
        }
        decoder.initialize(resolvedTemplatePath);

        if (eventBus == null) {
            eventBus = new MarketDataEventBus();
        }

        if (sequenceTracker == null) {
            sequenceTracker = new SequenceTracker();
        }
        sequenceTracker.setGapListener(new SequenceTracker.GapListener() {
            @Override
            public void onGapDetected(String applId, long expectedSeq, long receivedSeq) {
                log.warn("GAP: ApplID={}, expected={}, received={}, requesting recovery...",
                        applId, expectedSeq, receivedSeq);
                // TODO: Trigger snapshot/replay recovery
            }

            @Override
            public void onDuplicateDetected(String applId, long seqNum) {
                // Normal for Feed A/B redundancy
            }
        });

        if (orderBookManager == null) {
            orderBookManager = new OrderBookManager();
            eventBus.addListener(orderBookManager);
        }

        if (instrumentRegistry == null) {
            instrumentRegistry = new InstrumentRegistry();
            eventBus.addListener(instrumentRegistry);
        }

        // Create UDP receivers
        feedAReceiver = new UdpMulticastReceiver(
                "FeedA", multicastGroupFeedA, portFeedA, networkInterface,
                decoder, eventBus, sequenceTracker);
        if (activityLog != null) feedAReceiver.setActivityLog(activityLog);

        feedBReceiver = new UdpMulticastReceiver(
                "FeedB", multicastGroupFeedB, portFeedB, networkInterface,
                decoder, eventBus, sequenceTracker);
        if (activityLog != null) feedBReceiver.setActivityLog(activityLog);

        log.info("Feed Gateway Service initialized");
    }

    /**
     * Start receiving market data on Feed A and Feed B.
     * Uses Java 21 virtual threads for lightweight concurrency.
     */
    public void start() {
        if (running) {
            log.warn("Feed gateway already running");
            return;
        }

        running = true;
        log.info("Starting Feed Gateway...");

        // Start Feed A on daemon thread
        feedAThread = new Thread(feedAReceiver, "feed-a-receiver");
        feedAThread.setDaemon(true);
        feedAThread.start();

        // Start Feed B on daemon thread
        feedBThread = new Thread(feedBReceiver, "feed-b-receiver");
        feedBThread.setDaemon(true);
        feedBThread.start();

        log.info("Feed Gateway started - Feed A: {}:{}, Feed B: {}:{}",
                multicastGroupFeedA, portFeedA, multicastGroupFeedB, portFeedB);
    }

    /**
     * Stop all receivers and clean up.
     */
    public void stop() {
        if (!running) return;

        running = false;
        log.info("Stopping Feed Gateway...");

        if (feedAReceiver != null) feedAReceiver.stop();
        if (feedBReceiver != null) feedBReceiver.stop();

        try {
            if (feedAThread != null) feedAThread.join(5000);
            if (feedBThread != null) feedBThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Feed Gateway stopped");
    }

    // ==================== STATUS ====================

    /**
     * Get status of all feed channels.
     */
    public List<FeedStatus> getFeedStatuses() {
        List<FeedStatus> statuses = new ArrayList<>();
        if (feedAReceiver != null) statuses.add(feedAReceiver.getFeedStatus());
        if (feedBReceiver != null) statuses.add(feedBReceiver.getFeedStatus());
        return statuses;
    }

    // ==================== CONFIGURATION ====================

    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
    public void setMulticastGroupFeedA(String multicastGroupFeedA) { this.multicastGroupFeedA = multicastGroupFeedA; }
    public void setPortFeedA(int portFeedA) { this.portFeedA = portFeedA; }
    public void setMulticastGroupFeedB(String multicastGroupFeedB) { this.multicastGroupFeedB = multicastGroupFeedB; }
    public void setPortFeedB(int portFeedB) { this.portFeedB = portFeedB; }
    public void setNetworkInterface(String networkInterface) { this.networkInterface = networkInterface; }
    public void setActivityLog(ActivityLog activityLog) { this.activityLog = activityLog; }

    // ==================== ACCESSORS ====================

    public FastMessageDecoder getDecoder() { return decoder; }
    public MarketDataEventBus getEventBus() { return eventBus; }
    public SequenceTracker getSequenceTracker() { return sequenceTracker; }
    public OrderBookManager getOrderBookManager() { return orderBookManager; }
    public InstrumentRegistry getInstrumentRegistry() { return instrumentRegistry; }
    public ActivityLog getActivityLog() { return activityLog; }
    public boolean isRunning() { return running; }
}
