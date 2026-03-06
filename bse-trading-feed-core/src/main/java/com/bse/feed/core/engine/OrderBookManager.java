package com.bse.feed.core.engine;

import com.bse.feed.core.enums.MDEntryType;
import com.bse.feed.core.enums.MDSubBookType;
import com.bse.feed.core.enums.MDUpdateAction;
import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataListener;
import com.bse.feed.core.model.MarketDataEntry;
import com.bse.feed.core.model.OrderBook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central order book management engine.
 * Processes decoded market data events and maintains order books for all instruments.
 * Handles: New/Change/Delete at price levels, snapshot application, statistics updates.
 */
public class OrderBookManager implements MarketDataListener {

    private static final Logger log = LoggerFactory.getLogger(OrderBookManager.class);

    // Key: "SYMBOL:SubBookType" (e.g. "BIHL-EQO:REGULAR")
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    // Listeners for book updates (dashboard, SQL writer)
    private final java.util.List<OrderBookUpdateListener> updateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public void onMarketData(MarketDataEvent event) {
        switch (event.getEventType()) {
            case SNAPSHOT -> processSnapshot(event);
            case INCREMENTAL_REFRESH -> processIncremental(event);
            case SECURITY_STATUS -> processSecurityStatus(event);
            default -> { /* Heartbeat, SecurityDef, etc. handled elsewhere */ }
        }
    }

    /**
     * Process a full market data snapshot (MsgType=W).
     * Clears the book then applies all entries.
     */
    private void processSnapshot(MarketDataEvent event) {
        if (event.getEntries() == null || event.getEntries().isEmpty()) return;

        String symbol = event.getSymbol();
        if (symbol == null) return;

        // Group entries by sub-book type and clear/rebuild
        for (MarketDataEntry entry : event.getEntries()) {
            MDSubBookType subBook = entry.getSubBookType() != null
                    ? entry.getSubBookType() : MDSubBookType.REGULAR;

            OrderBook book = getOrCreateBook(symbol, subBook);

            // For the first entry, clear the book
            if (entry == event.getEntries().get(0)) {
                book.clearBook();
            }

            applyEntry(book, entry);
            book.setRptSeq(entry.getRptSeq());
        }

        notifyUpdate(symbol, OrderBookUpdateListener.UpdateType.SNAPSHOT);
    }

    /**
     * Process an incremental refresh (MsgType=X).
     * Applies individual add/change/delete operations.
     */
    private void processIncremental(MarketDataEvent event) {
        if (event.getEntries() == null || event.getEntries().isEmpty()) return;

        for (MarketDataEntry entry : event.getEntries()) {
            String symbol = entry.getSymbol();
            if (symbol == null) continue;

            MDSubBookType subBook = entry.getSubBookType() != null
                    ? entry.getSubBookType() : MDSubBookType.REGULAR;

            OrderBook book = getOrCreateBook(symbol, subBook);

            applyEntry(book, entry);
            book.setRptSeq(entry.getRptSeq());
        }

        if (event.getSymbol() != null) {
            notifyUpdate(event.getSymbol(), OrderBookUpdateListener.UpdateType.INCREMENTAL);
        }
    }

    /**
     * Apply a single market data entry to an order book.
     */
    private void applyEntry(OrderBook book, MarketDataEntry entry) {
        MDEntryType entryType = entry.getEntryType();
        if (entryType == null) return;

        // Order book entries (Bid/Offer)
        if (entryType.isOrderBookEntry()) {
            applyOrderBookEntry(book, entry);
            return;
        }

        // Statistics entries
        applyStatisticsEntry(book, entry, entryType);
    }

    /**
     * Apply bid/offer level updates.
     */
    private void applyOrderBookEntry(OrderBook book, MarketDataEntry entry) {
        MDUpdateAction action = entry.getUpdateAction();
        MDEntryType entryType = entry.getEntryType();
        boolean isBid = entryType.isBidSide();

        int level = entry.getMdPriceLevel() != null ? entry.getMdPriceLevel() : 1;
        BigDecimal price = entry.getPrice();
        BigDecimal size = entry.getSize();
        int numOrders = entry.getNumberOfOrders() != null ? entry.getNumberOfOrders() : 0;

        if (action == null) {
            // In snapshots, there's no update action - just set the level
            if (isBid) {
                book.setBidLevel(level, price, size, numOrders);
            } else {
                book.setOfferLevel(level, price, size, numOrders);
            }
            return;
        }

        switch (action) {
            case NEW -> {
                if (isBid) {
                    book.insertBidLevel(level, price, size, numOrders);
                } else {
                    book.insertOfferLevel(level, price, size, numOrders);
                }
            }
            case CHANGE -> {
                if (isBid) {
                    book.setBidLevel(level, price, size, numOrders);
                } else {
                    book.setOfferLevel(level, price, size, numOrders);
                }
            }
            case DELETE -> {
                if (isBid) {
                    book.deleteBidLevel(level);
                } else {
                    book.deleteOfferLevel(level);
                }
            }
        }
    }

    /**
     * Apply statistics-related entries (trades, OHLC, volume, etc.).
     */
    private void applyStatisticsEntry(OrderBook book, MarketDataEntry entry, MDEntryType entryType) {
        BigDecimal price = entry.getPrice();
        BigDecimal size = entry.getSize();

        switch (entryType) {
            case TRADE -> {
                if (price != null) book.setLastTradePrice(price);
                if (size != null) book.setLastTradeSize(size);
                book.setLastTradeTime(entry.getReceivedAt());
                if (entry.getTradeCondition() != null) {
                    book.setLastTradeCondition(entry.getTradeCondition());
                }
            }
            case OPENING_PRICE -> {
                if (price != null) book.setOpenPrice(price);
            }
            case CLOSING_PRICE -> {
                if (price != null) book.setClosePrice(price);
            }
            case HIGH_PRICE -> {
                if (price != null) book.setHighPrice(price);
            }
            case LOW_PRICE -> {
                if (price != null) book.setLowPrice(price);
            }
            case VOLUME -> {
                if (size != null) book.setTotalTradedVolume(size);
                if (entry.getNumberOfOrders() != null) {
                    book.setTotalNumberOfTrades(entry.getNumberOfOrders());
                }
            }
            case PREVIOUS_CLOSE -> {
                if (price != null) book.setPreviousClosePrice(price);
            }
            case VWAP -> {
                if (price != null) book.setVwap(price);
            }
            case LIFETIME_HIGH, LIFETIME_LOW -> {
                // Additional high/low tracking if needed
            }
            default -> {
                if (log.isTraceEnabled()) {
                    log.trace("Unhandled entry type {} for {}", entryType, book.getSymbol());
                }
            }
        }
    }

    /**
     * Process security trading status updates (MsgType=f).
     */
    private void processSecurityStatus(MarketDataEvent event) {
        if (event.getEntries() == null || event.getEntries().isEmpty()) return;

        for (MarketDataEntry entry : event.getEntries()) {
            String symbol = entry.getSymbol();
            if (symbol == null) continue;

            OrderBook book = getOrCreateBook(symbol, MDSubBookType.REGULAR);
            if (entry.getSecurityTradingStatus() != null) {
                book.setSecurityTradingStatus(entry.getSecurityTradingStatus());
            }
            book.setRptSeq(entry.getRptSeq());
        }
    }

    // ==================== BOOK ACCESS ====================

    /**
     * Get or create an order book for the given symbol and sub-book type.
     */
    public OrderBook getOrCreateBook(String symbol, MDSubBookType subBookType) {
        String key = buildKey(symbol, subBookType);
        return orderBooks.computeIfAbsent(key, k -> {
            log.info("Created order book for {} [{}]", symbol, subBookType);
            return new OrderBook(symbol, subBookType);
        });
    }

    /**
     * Get an existing order book, or null if not found.
     */
    public OrderBook getBook(String symbol) {
        return getBook(symbol, MDSubBookType.REGULAR);
    }

    /**
     * Get an existing order book for specific sub-book type.
     */
    public OrderBook getBook(String symbol, MDSubBookType subBookType) {
        return orderBooks.get(buildKey(symbol, subBookType));
    }

    /**
     * Get all order books.
     */
    public Collection<OrderBook> getAllBooks() {
        return orderBooks.values();
    }

    /**
     * Get total number of instruments tracked.
     */
    public int getInstrumentCount() {
        return orderBooks.size();
    }

    /**
     * Get all tracked symbols.
     */
    public java.util.Set<String> getSymbols() {
        java.util.Set<String> symbols = new java.util.TreeSet<>();
        for (OrderBook book : orderBooks.values()) {
            symbols.add(book.getSymbol());
        }
        return symbols;
    }

    private String buildKey(String symbol, MDSubBookType subBookType) {
        return symbol + ":" + (subBookType != null ? subBookType.name() : "REGULAR");
    }

    // ==================== UPDATE LISTENERS ====================

    public void addUpdateListener(OrderBookUpdateListener listener) {
        updateListeners.add(listener);
    }

    public void removeUpdateListener(OrderBookUpdateListener listener) {
        updateListeners.remove(listener);
    }

    private void notifyUpdate(String symbol, OrderBookUpdateListener.UpdateType type) {
        for (OrderBookUpdateListener listener : updateListeners) {
            try {
                listener.onOrderBookUpdate(symbol, type);
            } catch (Exception e) {
                log.error("Error notifying order book update listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Listener interface for order book updates.
     */
    public interface OrderBookUpdateListener {
        enum UpdateType {
            SNAPSHOT,
            INCREMENTAL,
            STATUS_CHANGE
        }

        void onOrderBookUpdate(String symbol, UpdateType type);
    }
}
