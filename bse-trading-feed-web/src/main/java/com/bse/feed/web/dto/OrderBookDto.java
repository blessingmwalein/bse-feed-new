package com.bse.feed.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for order book data sent to the dashboard.
 * Combines bid/offer levels with instrument statistics.
 */
public class OrderBookDto {

    private String symbol;
    private String subBookType;
    private LevelDto[] bids;
    private LevelDto[] offers;

    // Statistics
    private BigDecimal lastPrice;
    private BigDecimal lastSize;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal previousClose;
    private BigDecimal netChange;
    private BigDecimal percentChange;
    private BigDecimal vwap;
    private BigDecimal totalVolume;
    private BigDecimal totalValue;
    private int totalTrades;

    // Spread
    private BigDecimal bestBid;
    private BigDecimal bestOffer;
    private BigDecimal spread;

    // Status
    private int tradingStatus;
    private String tradingStatusDesc;
    private Instant lastUpdateTime;
    private long rptSeq;

    public static class LevelDto {
        private int level;
        private BigDecimal price;
        private BigDecimal quantity;
        private int orders;

        public LevelDto() {}

        public LevelDto(int level, BigDecimal price, BigDecimal quantity, int orders) {
            this.level = level;
            this.price = price;
            this.quantity = quantity;
            this.orders = orders;
        }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
    }

    // --- Getters and Setters ---

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSubBookType() { return subBookType; }
    public void setSubBookType(String subBookType) { this.subBookType = subBookType; }
    public LevelDto[] getBids() { return bids; }
    public void setBids(LevelDto[] bids) { this.bids = bids; }
    public LevelDto[] getOffers() { return offers; }
    public void setOffers(LevelDto[] offers) { this.offers = offers; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }
    public BigDecimal getLastSize() { return lastSize; }
    public void setLastSize(BigDecimal lastSize) { this.lastSize = lastSize; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
    public BigDecimal getNetChange() { return netChange; }
    public void setNetChange(BigDecimal netChange) { this.netChange = netChange; }
    public BigDecimal getPercentChange() { return percentChange; }
    public void setPercentChange(BigDecimal percentChange) { this.percentChange = percentChange; }
    public BigDecimal getVwap() { return vwap; }
    public void setVwap(BigDecimal vwap) { this.vwap = vwap; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }
    public BigDecimal getBestBid() { return bestBid; }
    public void setBestBid(BigDecimal bestBid) { this.bestBid = bestBid; }
    public BigDecimal getBestOffer() { return bestOffer; }
    public void setBestOffer(BigDecimal bestOffer) { this.bestOffer = bestOffer; }
    public BigDecimal getSpread() { return spread; }
    public void setSpread(BigDecimal spread) { this.spread = spread; }
    public int getTradingStatus() { return tradingStatus; }
    public void setTradingStatus(int tradingStatus) { this.tradingStatus = tradingStatus; }
    public String getTradingStatusDesc() { return tradingStatusDesc; }
    public void setTradingStatusDesc(String tradingStatusDesc) { this.tradingStatusDesc = tradingStatusDesc; }
    public Instant getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Instant lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    public long getRptSeq() { return rptSeq; }
    public void setRptSeq(long rptSeq) { this.rptSeq = rptSeq; }
}
