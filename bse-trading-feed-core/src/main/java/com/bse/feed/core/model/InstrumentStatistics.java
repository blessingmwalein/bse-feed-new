package com.bse.feed.core.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregated instrument statistics from the BSE feed.
 * Populated from various MDEntryType entries (Opening, High, Low, Close, VWAP, etc.).
 */
public class InstrumentStatistics {

    private String symbol;
    private Instant lastUpdateTime;

    // OHLC
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;

    // Previous close
    private BigDecimal previousClosePrice;
    private String previousCloseDate;

    // Volume / Value
    private BigDecimal totalTradedVolume;
    private BigDecimal totalTradedValue;
    private int totalNumberOfTrades;

    // VWAP
    private BigDecimal vwap;

    // Change
    private BigDecimal netChange;
    private BigDecimal percentChange;

    // Auction
    private BigDecimal indicativePrice;       // Auction equilibrium price
    private BigDecimal indicativeVolume;       // Auction equilibrium volume
    private BigDecimal indicativeSurplus;      // Auction surplus volume
    private String indicativeSurplusSide;      // B=Buy, S=Sell
    private BigDecimal auctionClearingPrice;

    // Price bands
    private BigDecimal staticCircuitBreakerHigh;
    private BigDecimal staticCircuitBreakerLow;
    private BigDecimal dynamicCircuitBreakerHigh;
    private BigDecimal dynamicCircuitBreakerLow;

    // Settlement
    private BigDecimal settlementPrice;
    private BigDecimal settlementYield;

    // Other
    private BigDecimal bestBid;
    private BigDecimal bestOffer;
    private BigDecimal marginRate;

    public InstrumentStatistics() {}

    public InstrumentStatistics(String symbol) {
        this.symbol = symbol;
        this.lastUpdateTime = Instant.now();
    }

    // --- Getters and Setters ---

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Instant getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Instant lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }

    public BigDecimal getPreviousClosePrice() { return previousClosePrice; }
    public void setPreviousClosePrice(BigDecimal previousClosePrice) { this.previousClosePrice = previousClosePrice; }

    public String getPreviousCloseDate() { return previousCloseDate; }
    public void setPreviousCloseDate(String previousCloseDate) { this.previousCloseDate = previousCloseDate; }

    public BigDecimal getTotalTradedVolume() { return totalTradedVolume; }
    public void setTotalTradedVolume(BigDecimal totalTradedVolume) { this.totalTradedVolume = totalTradedVolume; }

    public BigDecimal getTotalTradedValue() { return totalTradedValue; }
    public void setTotalTradedValue(BigDecimal totalTradedValue) { this.totalTradedValue = totalTradedValue; }

    public int getTotalNumberOfTrades() { return totalNumberOfTrades; }
    public void setTotalNumberOfTrades(int totalNumberOfTrades) { this.totalNumberOfTrades = totalNumberOfTrades; }

    public BigDecimal getVwap() { return vwap; }
    public void setVwap(BigDecimal vwap) { this.vwap = vwap; }

    public BigDecimal getNetChange() { return netChange; }
    public void setNetChange(BigDecimal netChange) { this.netChange = netChange; }

    public BigDecimal getPercentChange() { return percentChange; }
    public void setPercentChange(BigDecimal percentChange) { this.percentChange = percentChange; }

    public BigDecimal getIndicativePrice() { return indicativePrice; }
    public void setIndicativePrice(BigDecimal indicativePrice) { this.indicativePrice = indicativePrice; }

    public BigDecimal getIndicativeVolume() { return indicativeVolume; }
    public void setIndicativeVolume(BigDecimal indicativeVolume) { this.indicativeVolume = indicativeVolume; }

    public BigDecimal getIndicativeSurplus() { return indicativeSurplus; }
    public void setIndicativeSurplus(BigDecimal indicativeSurplus) { this.indicativeSurplus = indicativeSurplus; }

    public String getIndicativeSurplusSide() { return indicativeSurplusSide; }
    public void setIndicativeSurplusSide(String indicativeSurplusSide) { this.indicativeSurplusSide = indicativeSurplusSide; }

    public BigDecimal getAuctionClearingPrice() { return auctionClearingPrice; }
    public void setAuctionClearingPrice(BigDecimal auctionClearingPrice) { this.auctionClearingPrice = auctionClearingPrice; }

    public BigDecimal getStaticCircuitBreakerHigh() { return staticCircuitBreakerHigh; }
    public void setStaticCircuitBreakerHigh(BigDecimal staticCircuitBreakerHigh) { this.staticCircuitBreakerHigh = staticCircuitBreakerHigh; }

    public BigDecimal getStaticCircuitBreakerLow() { return staticCircuitBreakerLow; }
    public void setStaticCircuitBreakerLow(BigDecimal staticCircuitBreakerLow) { this.staticCircuitBreakerLow = staticCircuitBreakerLow; }

    public BigDecimal getDynamicCircuitBreakerHigh() { return dynamicCircuitBreakerHigh; }
    public void setDynamicCircuitBreakerHigh(BigDecimal dynamicCircuitBreakerHigh) { this.dynamicCircuitBreakerHigh = dynamicCircuitBreakerHigh; }

    public BigDecimal getDynamicCircuitBreakerLow() { return dynamicCircuitBreakerLow; }
    public void setDynamicCircuitBreakerLow(BigDecimal dynamicCircuitBreakerLow) { this.dynamicCircuitBreakerLow = dynamicCircuitBreakerLow; }

    public BigDecimal getSettlementPrice() { return settlementPrice; }
    public void setSettlementPrice(BigDecimal settlementPrice) { this.settlementPrice = settlementPrice; }

    public BigDecimal getSettlementYield() { return settlementYield; }
    public void setSettlementYield(BigDecimal settlementYield) { this.settlementYield = settlementYield; }

    public BigDecimal getBestBid() { return bestBid; }
    public void setBestBid(BigDecimal bestBid) { this.bestBid = bestBid; }

    public BigDecimal getBestOffer() { return bestOffer; }
    public void setBestOffer(BigDecimal bestOffer) { this.bestOffer = bestOffer; }

    public BigDecimal getMarginRate() { return marginRate; }
    public void setMarginRate(BigDecimal marginRate) { this.marginRate = marginRate; }

    @Override
    public String toString() {
        return String.format("Stats{%s O=%s H=%s L=%s C=%s Vol=%s Trades=%d VWAP=%s}",
                symbol,
                openPrice, highPrice, lowPrice, closePrice,
                totalTradedVolume, totalNumberOfTrades, vwap);
    }
}
