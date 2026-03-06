package com.bse.feed.core.event;

/**
 * Listener interface for receiving market data events.
 * Implementations should process events quickly to avoid blocking the feed thread.
 */
@FunctionalInterface
public interface MarketDataListener {

    /**
     * Called when a new market data event is available.
     *
     * @param event The decoded market data event
     */
    void onMarketData(MarketDataEvent event);
}
