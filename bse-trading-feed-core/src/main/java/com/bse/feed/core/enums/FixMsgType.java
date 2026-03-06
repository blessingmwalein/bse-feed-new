package com.bse.feed.core.enums;

/**
 * FIX MsgType (tag 35) values used on the BSE market data feed.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.2.1
 */
public enum FixMsgType {

    HEARTBEAT("0", "Heartbeat"),
    LOGOUT("5", "Logout"),
    LOGON("A", "Logon"),
    NEWS("B", "News"),
    QUOTE_REQUEST("R", "Quote Request"),
    MARKET_DATA_REQUEST("V", "Market Data Request"),
    MARKET_DATA_SNAPSHOT("W", "Market Data Snapshot (Full Refresh)"),
    MARKET_DATA_INCREMENTAL_REFRESH("X", "Market Data Incremental Refresh"),
    MARKET_DATA_REQUEST_REJECT("Y", "Market Data Request Reject"),
    APPLICATION_MESSAGE_REQUEST("BW", "Application Message Request"),
    APPLICATION_MESSAGE_REQUEST_ACK("BX", "Application Message Request Ack"),
    APPLICATION_MESSAGE_REPORT("BY", "Application Message Report"),
    SECURITY_DEFINITION_REQUEST("c", "Security Definition Request"),
    SECURITY_DEFINITION("d", "Security Definition"),
    SECURITY_STATUS("f", "Security Status"),
    BUSINESS_MESSAGE_REJECT("j", "Business Message Reject");

    private final String code;
    private final String description;

    FixMsgType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static FixMsgType fromCode(String code) {
        for (FixMsgType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return null;
    }
}
