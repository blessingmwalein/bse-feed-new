package com.bse.feed.core.engine;

import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataListener;
import com.bse.feed.core.model.SecurityDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all known instruments (securities) received from
 * Security Definition messages (MsgType=d, Template ID=18).
 */
public class InstrumentRegistry implements MarketDataListener {

    private static final Logger log = LoggerFactory.getLogger(InstrumentRegistry.class);

    // Key: Symbol (e.g. "BIHL-EQO")
    private final Map<String, SecurityDefinition> instruments = new ConcurrentHashMap<>();

    // Key: SecurityID (numeric exchange ID)
    private final Map<String, SecurityDefinition> instrumentsBySecurityId = new ConcurrentHashMap<>();

    @Override
    public void onMarketData(MarketDataEvent event) {
        if (event.getEventType() == MarketDataEvent.EventType.SECURITY_DEFINITION) {
            // The decoder should have created SecurityDefinition entries
            // This is handled separately via addInstrument()
        }
    }

    /**
     * Register or update an instrument definition.
     */
    public void addInstrument(SecurityDefinition def) {
        if (def == null) return;

        String symbol = def.getSymbol();
        if (symbol != null) {
            SecurityDefinition prev = instruments.put(symbol, def);
            if (prev == null) {
                log.info("Registered instrument: {} ({})", symbol, def.getSecurityType());
            } else {
                log.debug("Updated instrument: {}", symbol);
            }
        }

        String secId = def.getIsin();
        if (secId != null) {
            instrumentsBySecurityId.put(secId, def);
        }
    }

    /**
     * Look up instrument by symbol.
     */
    public SecurityDefinition getBySymbol(String symbol) {
        return instruments.get(symbol);
    }

    /**
     * Look up instrument by SecurityID.
     */
    public SecurityDefinition getBySecurityId(String securityId) {
        return instrumentsBySecurityId.get(securityId);
    }

    /**
     * Get all registered instruments.
     */
    public Collection<SecurityDefinition> getAllInstruments() {
        return instruments.values();
    }

    /**
     * Get total number of registered instruments.
     */
    public int getInstrumentCount() {
        return instruments.size();
    }

    /**
     * Check if an instrument is registered.
     */
    public boolean hasInstrument(String symbol) {
        return instruments.containsKey(symbol);
    }

    /**
     * Clear all instruments (used during daily reset).
     */
    public void clear() {
        instruments.clear();
        instrumentsBySecurityId.clear();
        log.info("Cleared instrument registry");
    }

    @Override
    public String toString() {
        return String.format("InstrumentRegistry{count=%d}", instruments.size());
    }
}
