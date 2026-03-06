package com.bse.feed.core.model;

import com.bse.feed.core.enums.MDSubBookType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Full order book for a single instrument (symbol) and sub-book type.
 * Maintains up to MAX_DEPTH price levels on both bid and offer sides.
 * Thread-safe via read-write lock for concurrent access from feed and dashboard.
 */
public class OrderBook {

    public static final int MAX_DEPTH = 10;

    private final String symbol;
    private final MDSubBookType subBookType;

    private final OrderBookLevel[] bids = new OrderBookLevel[MAX_DEPTH];
    private final OrderBookLevel[] offers = new OrderBookLevel[MAX_DEPTH];

    // Instrument-level sequence tracking
    private long rptSeq;
    private Instant lastUpdateTime;

    // Last trade information
    private BigDecimal lastTradePrice;
    private BigDecimal lastTradeSize;
    private Instant lastTradeTime;
    private String lastTradeCondition;

    // Statistics
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal previousClosePrice;
    private BigDecimal vwap;
    private BigDecimal totalTradedVolume;
    private BigDecimal totalTradedValue;
    private int totalNumberOfTrades;
    private BigDecimal netChange;
    private BigDecimal percentChange;

    // Security status
    private int securityTradingStatus;
    private String securityTradingStatusDesc;

    // Thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public OrderBook(String symbol, MDSubBookType subBookType) {
        this.symbol = symbol;
        this.subBookType = subBookType;
        this.lastUpdateTime = Instant.now();
        initLevels();
    }

    public OrderBook(String symbol) {
        this(symbol, MDSubBookType.REGULAR);
    }

    private void initLevels() {
        for (int i = 0; i < MAX_DEPTH; i++) {
            bids[i] = new OrderBookLevel(i + 1, null, BigDecimal.ZERO, 0);
            offers[i] = new OrderBookLevel(i + 1, null, BigDecimal.ZERO, 0);
        }
    }

    // ==================== BOOK UPDATE METHODS ====================

    /**
     * Add or update a bid level. Level is 1-based.
     */
    public void setBidLevel(int level, BigDecimal price, BigDecimal qty, int numOrders) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                OrderBookLevel lvl = bids[level - 1];
                lvl.setPrice(price);
                lvl.setQuantity(qty);
                lvl.setNumberOfOrders(numOrders);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add or update an offer level. Level is 1-based.
     */
    public void setOfferLevel(int level, BigDecimal price, BigDecimal qty, int numOrders) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                OrderBookLevel lvl = offers[level - 1];
                lvl.setPrice(price);
                lvl.setQuantity(qty);
                lvl.setNumberOfOrders(numOrders);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Insert a new bid level, shifting existing levels down.
     * Level is 1-based.
     */
    public void insertBidLevel(int level, BigDecimal price, BigDecimal qty, int numOrders) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                // Shift down from bottom
                for (int i = MAX_DEPTH - 1; i >= level; i--) {
                    copyLevel(bids[i - 1], bids[i]);
                }
                OrderBookLevel lvl = bids[level - 1];
                lvl.setPrice(price);
                lvl.setQuantity(qty);
                lvl.setNumberOfOrders(numOrders);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Insert a new offer level, shifting existing levels down.
     * Level is 1-based.
     */
    public void insertOfferLevel(int level, BigDecimal price, BigDecimal qty, int numOrders) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                for (int i = MAX_DEPTH - 1; i >= level; i--) {
                    copyLevel(offers[i - 1], offers[i]);
                }
                OrderBookLevel lvl = offers[level - 1];
                lvl.setPrice(price);
                lvl.setQuantity(qty);
                lvl.setNumberOfOrders(numOrders);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Delete a bid level, shifting remaining levels up.
     * Level is 1-based.
     */
    public void deleteBidLevel(int level) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                for (int i = level - 1; i < MAX_DEPTH - 1; i++) {
                    copyLevel(bids[i + 1], bids[i]);
                }
                clearLevel(bids[MAX_DEPTH - 1]);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Delete an offer level, shifting remaining levels up.
     * Level is 1-based.
     */
    public void deleteOfferLevel(int level) {
        lock.writeLock().lock();
        try {
            if (level >= 1 && level <= MAX_DEPTH) {
                for (int i = level - 1; i < MAX_DEPTH - 1; i++) {
                    copyLevel(offers[i + 1], offers[i]);
                }
                clearLevel(offers[MAX_DEPTH - 1]);
                lastUpdateTime = Instant.now();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear the entire book (used before applying a snapshot).
     */
    public void clearBook() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < MAX_DEPTH; i++) {
                clearLevel(bids[i]);
                clearLevel(offers[i]);
            }
            lastUpdateTime = Instant.now();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void copyLevel(OrderBookLevel src, OrderBookLevel dst) {
        dst.setPrice(src.getPrice());
        dst.setQuantity(src.getQuantity());
        dst.setNumberOfOrders(src.getNumberOfOrders());
    }

    private void clearLevel(OrderBookLevel lvl) {
        lvl.setPrice(null);
        lvl.setQuantity(BigDecimal.ZERO);
        lvl.setNumberOfOrders(0);
    }

    // ==================== SNAPSHOT ACCESS ====================

    /**
     * Get a thread-safe snapshot of current bid levels.
     */
    public OrderBookLevel[] getBidSnapshot() {
        lock.readLock().lock();
        try {
            OrderBookLevel[] snapshot = new OrderBookLevel[MAX_DEPTH];
            for (int i = 0; i < MAX_DEPTH; i++) {
                snapshot[i] = new OrderBookLevel(
                        bids[i].getLevel(), bids[i].getPrice(),
                        bids[i].getQuantity(), bids[i].getNumberOfOrders());
            }
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a thread-safe snapshot of current offer levels.
     */
    public OrderBookLevel[] getOfferSnapshot() {
        lock.readLock().lock();
        try {
            OrderBookLevel[] snapshot = new OrderBookLevel[MAX_DEPTH];
            for (int i = 0; i < MAX_DEPTH; i++) {
                snapshot[i] = new OrderBookLevel(
                        offers[i].getLevel(), offers[i].getPrice(),
                        offers[i].getQuantity(), offers[i].getNumberOfOrders());
            }
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the best bid price (top of book).
     */
    public BigDecimal getBestBid() {
        lock.readLock().lock();
        try {
            return bids[0].getPrice();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the best offer price (top of book).
     */
    public BigDecimal getBestOffer() {
        lock.readLock().lock();
        try {
            return offers[0].getPrice();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the bid-offer spread.
     */
    public BigDecimal getSpread() {
        lock.readLock().lock();
        try {
            BigDecimal bid = bids[0].getPrice();
            BigDecimal offer = offers[0].getPrice();
            if (bid != null && offer != null) {
                return offer.subtract(bid);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getSymbol() { return symbol; }
    public MDSubBookType getSubBookType() { return subBookType; }

    public long getRptSeq() {
        lock.readLock().lock();
        try { return rptSeq; } finally { lock.readLock().unlock(); }
    }

    public void setRptSeq(long rptSeq) {
        lock.writeLock().lock();
        try { this.rptSeq = rptSeq; } finally { lock.writeLock().unlock(); }
    }

    public Instant getLastUpdateTime() {
        lock.readLock().lock();
        try { return lastUpdateTime; } finally { lock.readLock().unlock(); }
    }

    public BigDecimal getLastTradePrice() {
        lock.readLock().lock();
        try { return lastTradePrice; } finally { lock.readLock().unlock(); }
    }

    public void setLastTradePrice(BigDecimal lastTradePrice) {
        lock.writeLock().lock();
        try { this.lastTradePrice = lastTradePrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getLastTradeSize() {
        lock.readLock().lock();
        try { return lastTradeSize; } finally { lock.readLock().unlock(); }
    }

    public void setLastTradeSize(BigDecimal lastTradeSize) {
        lock.writeLock().lock();
        try { this.lastTradeSize = lastTradeSize; } finally { lock.writeLock().unlock(); }
    }

    public Instant getLastTradeTime() {
        lock.readLock().lock();
        try { return lastTradeTime; } finally { lock.readLock().unlock(); }
    }

    public void setLastTradeTime(Instant lastTradeTime) {
        lock.writeLock().lock();
        try { this.lastTradeTime = lastTradeTime; } finally { lock.writeLock().unlock(); }
    }

    public String getLastTradeCondition() {
        lock.readLock().lock();
        try { return lastTradeCondition; } finally { lock.readLock().unlock(); }
    }

    public void setLastTradeCondition(String lastTradeCondition) {
        lock.writeLock().lock();
        try { this.lastTradeCondition = lastTradeCondition; } finally { lock.writeLock().unlock(); }
    }

    // --- Statistics ---

    public BigDecimal getOpenPrice() {
        lock.readLock().lock();
        try { return openPrice; } finally { lock.readLock().unlock(); }
    }

    public void setOpenPrice(BigDecimal openPrice) {
        lock.writeLock().lock();
        try { this.openPrice = openPrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getHighPrice() {
        lock.readLock().lock();
        try { return highPrice; } finally { lock.readLock().unlock(); }
    }

    public void setHighPrice(BigDecimal highPrice) {
        lock.writeLock().lock();
        try { this.highPrice = highPrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getLowPrice() {
        lock.readLock().lock();
        try { return lowPrice; } finally { lock.readLock().unlock(); }
    }

    public void setLowPrice(BigDecimal lowPrice) {
        lock.writeLock().lock();
        try { this.lowPrice = lowPrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getClosePrice() {
        lock.readLock().lock();
        try { return closePrice; } finally { lock.readLock().unlock(); }
    }

    public void setClosePrice(BigDecimal closePrice) {
        lock.writeLock().lock();
        try { this.closePrice = closePrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getPreviousClosePrice() {
        lock.readLock().lock();
        try { return previousClosePrice; } finally { lock.readLock().unlock(); }
    }

    public void setPreviousClosePrice(BigDecimal previousClosePrice) {
        lock.writeLock().lock();
        try { this.previousClosePrice = previousClosePrice; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getVwap() {
        lock.readLock().lock();
        try { return vwap; } finally { lock.readLock().unlock(); }
    }

    public void setVwap(BigDecimal vwap) {
        lock.writeLock().lock();
        try { this.vwap = vwap; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getTotalTradedVolume() {
        lock.readLock().lock();
        try { return totalTradedVolume; } finally { lock.readLock().unlock(); }
    }

    public void setTotalTradedVolume(BigDecimal totalTradedVolume) {
        lock.writeLock().lock();
        try { this.totalTradedVolume = totalTradedVolume; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getTotalTradedValue() {
        lock.readLock().lock();
        try { return totalTradedValue; } finally { lock.readLock().unlock(); }
    }

    public void setTotalTradedValue(BigDecimal totalTradedValue) {
        lock.writeLock().lock();
        try { this.totalTradedValue = totalTradedValue; } finally { lock.writeLock().unlock(); }
    }

    public int getTotalNumberOfTrades() {
        lock.readLock().lock();
        try { return totalNumberOfTrades; } finally { lock.readLock().unlock(); }
    }

    public void setTotalNumberOfTrades(int totalNumberOfTrades) {
        lock.writeLock().lock();
        try { this.totalNumberOfTrades = totalNumberOfTrades; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getNetChange() {
        lock.readLock().lock();
        try { return netChange; } finally { lock.readLock().unlock(); }
    }

    public void setNetChange(BigDecimal netChange) {
        lock.writeLock().lock();
        try { this.netChange = netChange; } finally { lock.writeLock().unlock(); }
    }

    public BigDecimal getPercentChange() {
        lock.readLock().lock();
        try { return percentChange; } finally { lock.readLock().unlock(); }
    }

    public void setPercentChange(BigDecimal percentChange) {
        lock.writeLock().lock();
        try { this.percentChange = percentChange; } finally { lock.writeLock().unlock(); }
    }

    // --- Security Status ---

    public int getSecurityTradingStatus() {
        lock.readLock().lock();
        try { return securityTradingStatus; } finally { lock.readLock().unlock(); }
    }

    public void setSecurityTradingStatus(int securityTradingStatus) {
        lock.writeLock().lock();
        try { this.securityTradingStatus = securityTradingStatus; } finally { lock.writeLock().unlock(); }
    }

    public String getSecurityTradingStatusDesc() {
        lock.readLock().lock();
        try { return securityTradingStatusDesc; } finally { lock.readLock().unlock(); }
    }

    public void setSecurityTradingStatusDesc(String securityTradingStatusDesc) {
        lock.writeLock().lock();
        try { this.securityTradingStatusDesc = securityTradingStatusDesc; } finally { lock.writeLock().unlock(); }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== %s [%s] RptSeq=%d ===\n", symbol, subBookType, rptSeq));
            sb.append("  BIDS:\n");
            for (OrderBookLevel bid : bids) {
                if (bid.isValid()) sb.append("    ").append(bid).append('\n');
            }
            sb.append("  OFFERS:\n");
            for (OrderBookLevel offer : offers) {
                if (offer.isValid()) sb.append("    ").append(offer).append('\n');
            }
            if (lastTradePrice != null) {
                sb.append(String.format("  LAST: %s x %s\n",
                        lastTradePrice.toPlainString(),
                        lastTradeSize != null ? lastTradeSize.toPlainString() : "?"));
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
