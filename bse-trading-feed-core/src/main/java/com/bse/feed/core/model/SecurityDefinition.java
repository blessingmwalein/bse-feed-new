package com.bse.feed.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Full instrument definition from Security Definition message (MsgType=d).
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.1
 */
public class SecurityDefinition {

    // Header
    private String applId;
    private long applSeqNum;

    // Request fields (Snapshot channel only)
    private String securityReqId;
    private Integer securityResponseType;  // 4=List of Securities Returned, 6=Cannot Match
    private String lastRptRequested;       // "Y" = last message

    // Instrument identity
    private String symbol;
    private String securityStatus;         // 1=Active, 2=Inactive, 8=Halted, 9=Suspended
    private String isin;                   // SecurityAltID when source=4
    private String securityAltIdSource;
    private String cfiCode;
    private String securityType;           // CS, FUT, OPT, MLEG, CORP, etc.
    private String securitySubType;        // CS, BF, CD, OCS, etc.

    // Dates
    private String maturityDate;           // YYYYMMDD
    private String maturityMonthDate;      // YYYYMMDD (for futures/calendar spreads)
    private String issueDate;              // YYYYMMDD

    // Underlying
    private String underlyingSymbol;

    // Options
    private Integer putOrCall;             // 0=Put, 1=Call
    private BigDecimal strikePrice;
    private Integer exerciseStyle;         // 0=European, 1=American

    // Fixed income
    private String issuer;
    private BigDecimal couponRate;

    // Legs (strategies)
    private List<Leg> legs = new ArrayList<>();

    // Pricing
    private Integer priceType;             // 1=Percent of Par, 2=Price Per Unit, 4=Discount Rate, 9=Yield

    // Segment
    private String marketSegmentId;
    private String corporateAction;
    private String securityGroup;          // Sector

    // Instrument attributes
    private Integer instrAttribType;       // 100=Issued Quantity
    private String instrAttribValue;

    // Sub-book entries
    private List<SubBookEntry> subBookEntries = new ArrayList<>();

    // Other
    private Integer listMethod;            // 0=Pre-Listed, 1=User Defined
    private Integer partitionId;

    // --- Getters and Setters ---

    public String getApplId() { return applId; }
    public void setApplId(String applId) { this.applId = applId; }

    public long getApplSeqNum() { return applSeqNum; }
    public void setApplSeqNum(long applSeqNum) { this.applSeqNum = applSeqNum; }

    public String getSecurityReqId() { return securityReqId; }
    public void setSecurityReqId(String securityReqId) { this.securityReqId = securityReqId; }

    public Integer getSecurityResponseType() { return securityResponseType; }
    public void setSecurityResponseType(Integer securityResponseType) { this.securityResponseType = securityResponseType; }

    public String getLastRptRequested() { return lastRptRequested; }
    public void setLastRptRequested(String lastRptRequested) { this.lastRptRequested = lastRptRequested; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSecurityStatus() { return securityStatus; }
    public void setSecurityStatus(String securityStatus) { this.securityStatus = securityStatus; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getSecurityAltIdSource() { return securityAltIdSource; }
    public void setSecurityAltIdSource(String securityAltIdSource) { this.securityAltIdSource = securityAltIdSource; }

    public String getCfiCode() { return cfiCode; }
    public void setCfiCode(String cfiCode) { this.cfiCode = cfiCode; }

    public String getSecurityType() { return securityType; }
    public void setSecurityType(String securityType) { this.securityType = securityType; }

    public String getSecuritySubType() { return securitySubType; }
    public void setSecuritySubType(String securitySubType) { this.securitySubType = securitySubType; }

    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }

    public String getMaturityMonthDate() { return maturityMonthDate; }
    public void setMaturityMonthDate(String maturityMonthDate) { this.maturityMonthDate = maturityMonthDate; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public void setUnderlyingSymbol(String underlyingSymbol) { this.underlyingSymbol = underlyingSymbol; }

    public Integer getPutOrCall() { return putOrCall; }
    public void setPutOrCall(Integer putOrCall) { this.putOrCall = putOrCall; }

    public BigDecimal getStrikePrice() { return strikePrice; }
    public void setStrikePrice(BigDecimal strikePrice) { this.strikePrice = strikePrice; }

    public Integer getExerciseStyle() { return exerciseStyle; }
    public void setExerciseStyle(Integer exerciseStyle) { this.exerciseStyle = exerciseStyle; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public BigDecimal getCouponRate() { return couponRate; }
    public void setCouponRate(BigDecimal couponRate) { this.couponRate = couponRate; }

    public List<Leg> getLegs() { return legs; }
    public void setLegs(List<Leg> legs) { this.legs = legs; }

    public Integer getPriceType() { return priceType; }
    public void setPriceType(Integer priceType) { this.priceType = priceType; }

    public String getMarketSegmentId() { return marketSegmentId; }
    public void setMarketSegmentId(String marketSegmentId) { this.marketSegmentId = marketSegmentId; }

    public String getCorporateAction() { return corporateAction; }
    public void setCorporateAction(String corporateAction) { this.corporateAction = corporateAction; }

    public String getSecurityGroup() { return securityGroup; }
    public void setSecurityGroup(String securityGroup) { this.securityGroup = securityGroup; }

    public Integer getInstrAttribType() { return instrAttribType; }
    public void setInstrAttribType(Integer instrAttribType) { this.instrAttribType = instrAttribType; }

    public String getInstrAttribValue() { return instrAttribValue; }
    public void setInstrAttribValue(String instrAttribValue) { this.instrAttribValue = instrAttribValue; }

    public List<SubBookEntry> getSubBookEntries() { return subBookEntries; }
    public void setSubBookEntries(List<SubBookEntry> subBookEntries) { this.subBookEntries = subBookEntries; }

    public Integer getListMethod() { return listMethod; }
    public void setListMethod(Integer listMethod) { this.listMethod = listMethod; }

    public Integer getPartitionId() { return partitionId; }
    public void setPartitionId(Integer partitionId) { this.partitionId = partitionId; }

    public boolean isActive() { return "1".equals(securityStatus); }
    public boolean isHalted() { return "8".equals(securityStatus); }
    public boolean isSuspended() { return "9".equals(securityStatus); }

    @Override
    public String toString() {
        return "SecurityDefinition{symbol='" + symbol + "', status=" + securityStatus
                + ", type=" + securityType + ", segment=" + marketSegmentId + "}";
    }

    /**
     * Leg of a multi-legged instrument.
     */
    public static class Leg {
        private String legSymbol;
        private BigDecimal legRatioQty;
        private Integer legSide;  // 1=Buy, 2=Sell

        public String getLegSymbol() { return legSymbol; }
        public void setLegSymbol(String legSymbol) { this.legSymbol = legSymbol; }
        public BigDecimal getLegRatioQty() { return legRatioQty; }
        public void setLegRatioQty(BigDecimal legRatioQty) { this.legRatioQty = legRatioQty; }
        public Integer getLegSide() { return legSide; }
        public void setLegSide(Integer legSide) { this.legSide = legSide; }
    }

    /**
     * Sub-book entry from the Security Definition NoMDEntries group.
     */
    public static class SubBookEntry {
        private int mdSubBookType = 1;  // default Regular

        public int getMdSubBookType() { return mdSubBookType; }
        public void setMdSubBookType(int mdSubBookType) { this.mdSubBookType = mdSubBookType; }
    }
}
