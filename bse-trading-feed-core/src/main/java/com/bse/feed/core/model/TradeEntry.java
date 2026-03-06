package com.bse.feed.core.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a single trade execution from the BSE feed.
 * Populated from MDEntryType=2 (Trade) entries.
 */
public class TradeEntry {

    private String symbol;
    private String tradeId;              // MDEntryID (278)
    private String secondaryTradeId;     // SecondaryTradeID (1040)
    private BigDecimal price;
    private BigDecimal size;
    private Instant timestamp;
    private String tradeCondition;       // TradeCondition (277)
    private String matchType;            // MatchType (574) - "5" = Auction
    private int trdType;                 // TrdType (828)
    private int trdSubType;             // TrdSubType (829)
    private int mdSubBookType;           // MDSubBookType (1173)
    private long rptSeq;
    private long applSeqNum;
    private int recoveryTrdIndicator;    // 1 = recovery trade

    // Open/Close indicator
    private int openCloseIndicator;

    // Settlement
    private int settleType;
    private String stipulationType;
    private String stipulationValue;

    // MiFID II fields
    private String securityAltId;        // ISIN
    private String currency;
    private String mdMkt;                // Exchange ID

    public TradeEntry() {}

    public TradeEntry(String symbol, BigDecimal price, BigDecimal size) {
        this.symbol = symbol;
        this.price = price;
        this.size = size;
        this.timestamp = Instant.now();
    }

    // --- Getters and Setters ---

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getSecondaryTradeId() { return secondaryTradeId; }
    public void setSecondaryTradeId(String secondaryTradeId) { this.secondaryTradeId = secondaryTradeId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTradeCondition() { return tradeCondition; }
    public void setTradeCondition(String tradeCondition) { this.tradeCondition = tradeCondition; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public int getTrdType() { return trdType; }
    public void setTrdType(int trdType) { this.trdType = trdType; }

    public int getTrdSubType() { return trdSubType; }
    public void setTrdSubType(int trdSubType) { this.trdSubType = trdSubType; }

    public int getMdSubBookType() { return mdSubBookType; }
    public void setMdSubBookType(int mdSubBookType) { this.mdSubBookType = mdSubBookType; }

    public long getRptSeq() { return rptSeq; }
    public void setRptSeq(long rptSeq) { this.rptSeq = rptSeq; }

    public long getApplSeqNum() { return applSeqNum; }
    public void setApplSeqNum(long applSeqNum) { this.applSeqNum = applSeqNum; }

    public int getRecoveryTrdIndicator() { return recoveryTrdIndicator; }
    public void setRecoveryTrdIndicator(int recoveryTrdIndicator) { this.recoveryTrdIndicator = recoveryTrdIndicator; }

    public int getOpenCloseIndicator() { return openCloseIndicator; }
    public void setOpenCloseIndicator(int openCloseIndicator) { this.openCloseIndicator = openCloseIndicator; }

    public int getSettleType() { return settleType; }
    public void setSettleType(int settleType) { this.settleType = settleType; }

    public String getStipulationType() { return stipulationType; }
    public void setStipulationType(String stipulationType) { this.stipulationType = stipulationType; }

    public String getStipulationValue() { return stipulationValue; }
    public void setStipulationValue(String stipulationValue) { this.stipulationValue = stipulationValue; }

    public String getSecurityAltId() { return securityAltId; }
    public void setSecurityAltId(String securityAltId) { this.securityAltId = securityAltId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMdMkt() { return mdMkt; }
    public void setMdMkt(String mdMkt) { this.mdMkt = mdMkt; }

    /**
     * Check if this is an auction trade.
     */
    public boolean isAuctionTrade() {
        return "5".equals(matchType);
    }

    /**
     * Check if this is a recovery trade (from snapshot/replay).
     */
    public boolean isRecoveryTrade() {
        return recoveryTrdIndicator == 1;
    }

    @Override
    public String toString() {
        return String.format("Trade{%s %s @ %s id=%s seq=%d %s}",
                symbol,
                size != null ? size.toPlainString() : "?",
                price != null ? price.toPlainString() : "?",
                tradeId,
                rptSeq,
                tradeCondition != null ? tradeCondition : "");
    }
}
