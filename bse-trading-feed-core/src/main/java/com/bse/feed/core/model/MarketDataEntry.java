package com.bse.feed.core.model;

import com.bse.feed.core.enums.MDEntryType;
import com.bse.feed.core.enums.MDSubBookType;
import com.bse.feed.core.enums.MDUpdateAction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single market data entry decoded from Market Data Snapshot or Incremental Refresh.
 * This is the core data object that flows through the system.
 */
public class MarketDataEntry {

    // Receive timestamp (local)
    private Instant receivedAt = Instant.now();

    // Header fields
    private String applId;
    private long applSeqNum;
    private String msgType;           // W=Snapshot, X=Incremental, etc.
    private int templateId;

    // Message-level fields
    private Integer mdBookType;       // 1=TopOfBook, 2=PriceDepth, 3=OrderDepth
    private String mdReqId;           // For snapshot channel responses
    private String lastRptRequested;  // Y = last message in response
    private String lastFragment;      // N or Y (for multi-message snapshots)
    private Integer recoveryTrdIndicator;  // 1 = recovery trade

    // Entry-level fields
    private MDUpdateAction updateAction;
    private MDEntryType entryType;
    private String mdEntryTypeRaw;    // Raw code from message
    private MDSubBookType subBookType;
    private int mdSubBookTypeRaw = 1; // Default: Regular

    // Instrument
    private String symbol;
    private long rptSeq;              // Instrument-level sequence number

    // Price/Size data
    private BigDecimal price;         // MDEntryPx (270)
    private BigDecimal yield;         // Yield (236)
    private BigDecimal lastParPx;     // LastParPx (669)
    private BigDecimal size;          // MDEntrySize (271)
    private Integer numberOfOrders;   // NumberOfOrders (346)
    private Integer mdPriceLevel;     // MDPriceLevel (1023)
    private Integer mdEntryPositionNo; // MDEntryPositionNo (290)
    private String mdEntryId;         // MDEntryID (278) - trade ID or order ID

    // Time fields
    private String mdEntryDate;       // MDEntryDate (272)
    private String mdEntryTime;       // MDEntryTime (273)

    // Trade fields
    private String tradeCondition;    // TradeCondition (277)
    private String matchType;         // MatchType (574) - "5" = Auction
    private Integer mdQuoteType;      // MDQuoteType (1070) - 0 = Indicative
    private Integer trdSubType;       // TrdSubType (829) - off-book trade type
    private Integer trdType;          // TrdType (828) - 16=All or None
    private String secondaryTradeId;  // SecondaryTradeID (1040)

    // Party info
    private String partyId;
    private String partyIdSource;
    private Integer partyRole;

    // Open/Close indicator
    private Integer openCloseIndicator;  // 30002

    // Security status (in snapshots)
    private Integer mdSecurityTradingStatus;  // 1682
    private Integer mdHaltReason;             // 1684

    // AON fields
    private Integer aonStatus;        // 31002
    private Integer aonSide;          // 31003

    // Settlement
    private Integer settleType;       // 63
    private String stipulationType;   // 233
    private String stipulationValue;  // 234

    // Statistics
    private Integer mdStatType;       // 31001 - 1=Market, 2=Sector, 3=InstrumentType, 4=Instrument

    // Trade identifiers (MiFID II)
    private String securityAltId;     // 455 - ISIN
    private String securityAltIdSource;  // 456
    private String currency;          // 15
    private Integer priceNotation;    // 20002
    private String mdMkt;             // 275 - Exchange ID
    private BigDecimal underlyingNotional;  // 2614
    private String underlyingNotionalCurrency;  // 2615
    private String tzTransactTime;    // 1132
    private String tzPublicationTime; // 20001

    // Post-trade transparency flags
    private String ptBenchmarkTransactionFlag;
    private String ptPriceFormingTradesFlag;
    private String ptCancellationFlag;
    private String ptAmendmentFlag;

    // Price Band fields
    private Integer priceLimitEvent;  // 32027
    private Integer securityTradingStatus;  // 326 (within price band context)
    private Integer pbAffectedSide;   // 32031
    private Integer priceBandLevel;   // 32030
    private BigDecimal highLimitPrice; // 1149
    private BigDecimal lowLimitPrice;  // 1148
    private Integer priceLimitType;   // 1306
    private Integer priceLimitUpdate; // 32028
    private Integer impliedTradeFlag; // 31004

    // Last sequence processed (snapshots)
    private Long lastMsgSeqNumProcessed;  // 369

    // --- Getters and Setters ---

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public String getApplId() { return applId; }
    public void setApplId(String applId) { this.applId = applId; }

    public long getApplSeqNum() { return applSeqNum; }
    public void setApplSeqNum(long applSeqNum) { this.applSeqNum = applSeqNum; }

    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    public Integer getMdBookType() { return mdBookType; }
    public void setMdBookType(Integer mdBookType) { this.mdBookType = mdBookType; }

    public String getMdReqId() { return mdReqId; }
    public void setMdReqId(String mdReqId) { this.mdReqId = mdReqId; }

    public String getLastRptRequested() { return lastRptRequested; }
    public void setLastRptRequested(String lastRptRequested) { this.lastRptRequested = lastRptRequested; }

    public String getLastFragment() { return lastFragment; }
    public void setLastFragment(String lastFragment) { this.lastFragment = lastFragment; }

    public Integer getRecoveryTrdIndicator() { return recoveryTrdIndicator; }
    public void setRecoveryTrdIndicator(Integer recoveryTrdIndicator) { this.recoveryTrdIndicator = recoveryTrdIndicator; }

    public MDUpdateAction getUpdateAction() { return updateAction; }
    public void setUpdateAction(MDUpdateAction updateAction) { this.updateAction = updateAction; }

    public MDEntryType getEntryType() { return entryType; }
    public void setEntryType(MDEntryType entryType) { this.entryType = entryType; }

    public String getMdEntryTypeRaw() { return mdEntryTypeRaw; }
    public void setMdEntryTypeRaw(String mdEntryTypeRaw) { this.mdEntryTypeRaw = mdEntryTypeRaw; }

    public MDSubBookType getSubBookType() { return subBookType; }
    public void setSubBookType(MDSubBookType subBookType) { this.subBookType = subBookType; }

    public int getMdSubBookTypeRaw() { return mdSubBookTypeRaw; }
    public void setMdSubBookTypeRaw(int mdSubBookTypeRaw) { this.mdSubBookTypeRaw = mdSubBookTypeRaw; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public long getRptSeq() { return rptSeq; }
    public void setRptSeq(long rptSeq) { this.rptSeq = rptSeq; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getYield() { return yield; }
    public void setYield(BigDecimal yield) { this.yield = yield; }

    public BigDecimal getLastParPx() { return lastParPx; }
    public void setLastParPx(BigDecimal lastParPx) { this.lastParPx = lastParPx; }

    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }

    public Integer getNumberOfOrders() { return numberOfOrders; }
    public void setNumberOfOrders(Integer numberOfOrders) { this.numberOfOrders = numberOfOrders; }

    public Integer getMdPriceLevel() { return mdPriceLevel; }
    public void setMdPriceLevel(Integer mdPriceLevel) { this.mdPriceLevel = mdPriceLevel; }

    public Integer getMdEntryPositionNo() { return mdEntryPositionNo; }
    public void setMdEntryPositionNo(Integer mdEntryPositionNo) { this.mdEntryPositionNo = mdEntryPositionNo; }

    public String getMdEntryId() { return mdEntryId; }
    public void setMdEntryId(String mdEntryId) { this.mdEntryId = mdEntryId; }

    public String getMdEntryDate() { return mdEntryDate; }
    public void setMdEntryDate(String mdEntryDate) { this.mdEntryDate = mdEntryDate; }

    public String getMdEntryTime() { return mdEntryTime; }
    public void setMdEntryTime(String mdEntryTime) { this.mdEntryTime = mdEntryTime; }

    public String getTradeCondition() { return tradeCondition; }
    public void setTradeCondition(String tradeCondition) { this.tradeCondition = tradeCondition; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public Integer getMdQuoteType() { return mdQuoteType; }
    public void setMdQuoteType(Integer mdQuoteType) { this.mdQuoteType = mdQuoteType; }

    public Integer getTrdSubType() { return trdSubType; }
    public void setTrdSubType(Integer trdSubType) { this.trdSubType = trdSubType; }

    public Integer getTrdType() { return trdType; }
    public void setTrdType(Integer trdType) { this.trdType = trdType; }

    public String getSecondaryTradeId() { return secondaryTradeId; }
    public void setSecondaryTradeId(String secondaryTradeId) { this.secondaryTradeId = secondaryTradeId; }

    public String getPartyId() { return partyId; }
    public void setPartyId(String partyId) { this.partyId = partyId; }

    public String getPartyIdSource() { return partyIdSource; }
    public void setPartyIdSource(String partyIdSource) { this.partyIdSource = partyIdSource; }

    public Integer getPartyRole() { return partyRole; }
    public void setPartyRole(Integer partyRole) { this.partyRole = partyRole; }

    public Integer getOpenCloseIndicator() { return openCloseIndicator; }
    public void setOpenCloseIndicator(Integer openCloseIndicator) { this.openCloseIndicator = openCloseIndicator; }

    public Integer getMdSecurityTradingStatus() { return mdSecurityTradingStatus; }
    public void setMdSecurityTradingStatus(Integer mdSecurityTradingStatus) { this.mdSecurityTradingStatus = mdSecurityTradingStatus; }

    public Integer getMdHaltReason() { return mdHaltReason; }
    public void setMdHaltReason(Integer mdHaltReason) { this.mdHaltReason = mdHaltReason; }

    public Integer getAonStatus() { return aonStatus; }
    public void setAonStatus(Integer aonStatus) { this.aonStatus = aonStatus; }

    public Integer getAonSide() { return aonSide; }
    public void setAonSide(Integer aonSide) { this.aonSide = aonSide; }

    public Integer getSettleType() { return settleType; }
    public void setSettleType(Integer settleType) { this.settleType = settleType; }

    public String getStipulationType() { return stipulationType; }
    public void setStipulationType(String stipulationType) { this.stipulationType = stipulationType; }

    public String getStipulationValue() { return stipulationValue; }
    public void setStipulationValue(String stipulationValue) { this.stipulationValue = stipulationValue; }

    public Integer getMdStatType() { return mdStatType; }
    public void setMdStatType(Integer mdStatType) { this.mdStatType = mdStatType; }

    public String getSecurityAltId() { return securityAltId; }
    public void setSecurityAltId(String securityAltId) { this.securityAltId = securityAltId; }

    public String getSecurityAltIdSource() { return securityAltIdSource; }
    public void setSecurityAltIdSource(String securityAltIdSource) { this.securityAltIdSource = securityAltIdSource; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getPriceNotation() { return priceNotation; }
    public void setPriceNotation(Integer priceNotation) { this.priceNotation = priceNotation; }

    public String getMdMkt() { return mdMkt; }
    public void setMdMkt(String mdMkt) { this.mdMkt = mdMkt; }

    public BigDecimal getUnderlyingNotional() { return underlyingNotional; }
    public void setUnderlyingNotional(BigDecimal underlyingNotional) { this.underlyingNotional = underlyingNotional; }

    public String getUnderlyingNotionalCurrency() { return underlyingNotionalCurrency; }
    public void setUnderlyingNotionalCurrency(String underlyingNotionalCurrency) { this.underlyingNotionalCurrency = underlyingNotionalCurrency; }

    public String getTzTransactTime() { return tzTransactTime; }
    public void setTzTransactTime(String tzTransactTime) { this.tzTransactTime = tzTransactTime; }

    public String getTzPublicationTime() { return tzPublicationTime; }
    public void setTzPublicationTime(String tzPublicationTime) { this.tzPublicationTime = tzPublicationTime; }

    public String getPtBenchmarkTransactionFlag() { return ptBenchmarkTransactionFlag; }
    public void setPtBenchmarkTransactionFlag(String ptBenchmarkTransactionFlag) { this.ptBenchmarkTransactionFlag = ptBenchmarkTransactionFlag; }

    public String getPtPriceFormingTradesFlag() { return ptPriceFormingTradesFlag; }
    public void setPtPriceFormingTradesFlag(String ptPriceFormingTradesFlag) { this.ptPriceFormingTradesFlag = ptPriceFormingTradesFlag; }

    public String getPtCancellationFlag() { return ptCancellationFlag; }
    public void setPtCancellationFlag(String ptCancellationFlag) { this.ptCancellationFlag = ptCancellationFlag; }

    public String getPtAmendmentFlag() { return ptAmendmentFlag; }
    public void setPtAmendmentFlag(String ptAmendmentFlag) { this.ptAmendmentFlag = ptAmendmentFlag; }

    public Integer getPriceLimitEvent() { return priceLimitEvent; }
    public void setPriceLimitEvent(Integer priceLimitEvent) { this.priceLimitEvent = priceLimitEvent; }

    public Integer getSecurityTradingStatus() { return securityTradingStatus; }
    public void setSecurityTradingStatus(Integer securityTradingStatus) { this.securityTradingStatus = securityTradingStatus; }

    public Integer getPbAffectedSide() { return pbAffectedSide; }
    public void setPbAffectedSide(Integer pbAffectedSide) { this.pbAffectedSide = pbAffectedSide; }

    public Integer getPriceBandLevel() { return priceBandLevel; }
    public void setPriceBandLevel(Integer priceBandLevel) { this.priceBandLevel = priceBandLevel; }

    public BigDecimal getHighLimitPrice() { return highLimitPrice; }
    public void setHighLimitPrice(BigDecimal highLimitPrice) { this.highLimitPrice = highLimitPrice; }

    public BigDecimal getLowLimitPrice() { return lowLimitPrice; }
    public void setLowLimitPrice(BigDecimal lowLimitPrice) { this.lowLimitPrice = lowLimitPrice; }

    public Integer getPriceLimitType() { return priceLimitType; }
    public void setPriceLimitType(Integer priceLimitType) { this.priceLimitType = priceLimitType; }

    public Integer getPriceLimitUpdate() { return priceLimitUpdate; }
    public void setPriceLimitUpdate(Integer priceLimitUpdate) { this.priceLimitUpdate = priceLimitUpdate; }

    public Integer getImpliedTradeFlag() { return impliedTradeFlag; }
    public void setImpliedTradeFlag(Integer impliedTradeFlag) { this.impliedTradeFlag = impliedTradeFlag; }

    public Long getLastMsgSeqNumProcessed() { return lastMsgSeqNumProcessed; }
    public void setLastMsgSeqNumProcessed(Long lastMsgSeqNumProcessed) { this.lastMsgSeqNumProcessed = lastMsgSeqNumProcessed; }

    /**
     * Compact display string for logging/dashboard.
     */
    public String toDisplayString() {
        String typeStr = entryType != null ? entryType.getDescription() : mdEntryTypeRaw;
        String priceStr = price != null ? price.toPlainString() : "-";
        String sizeStr = size != null ? size.toPlainString() : "-";
        return String.format("[%s] %s %s Price=%s Size=%s Lvl=%s",
                symbol, updateAction, typeStr, priceStr, sizeStr, mdPriceLevel);
    }

    @Override
    public String toString() {
        return "MarketDataEntry{" +
                "symbol='" + symbol + '\'' +
                ", entryType=" + entryType +
                ", updateAction=" + updateAction +
                ", price=" + price +
                ", size=" + size +
                ", mdPriceLevel=" + mdPriceLevel +
                ", rptSeq=" + rptSeq +
                '}';
    }
}
