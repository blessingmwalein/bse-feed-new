package com.bse.feed.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple event bus for distributing market data events to registered listeners.
 * Uses CopyOnWriteArrayList for thread-safe listener management.
 * For ultra-low latency, consider replacing with LMAX Disruptor.
 */
public class MarketDataEventBus {

    private static final Logger log = LoggerFactory.getLogger(MarketDataEventBus.class);

    private final List<MarketDataListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Register a listener to receive market data events.
     */
    public void addListener(MarketDataListener listener) {
        listeners.add(listener);
        log.info("Registered market data listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Remove a previously registered listener.
     */
    public void removeListener(MarketDataListener listener) {
        listeners.remove(listener);
        log.info("Removed market data listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Publish an event to all registered listeners.
     * Exceptions in one listener do not affect others.
     */
    public void publish(MarketDataEvent event) {
        for (MarketDataListener listener : listeners) {
            try {
                listener.onMarketData(event);
            } catch (Exception e) {
                log.error("Error in market data listener {}: {}",
                        listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Get the number of registered listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
