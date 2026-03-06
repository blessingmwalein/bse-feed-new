package com.bse.feed.core.enums;

/**
 * TradeCondition (tag 277) values.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.4
 */
public enum TradeCondition {

    BUY_IMBALANCE("P", "Buy Imbalance"),
    SELL_IMBALANCE("Q", "Sell Imbalance"),
    OPENING_PRICE("R", "Opening Price"),
    RE_OPENING_PRICE("w", "Re-Opening Price"),
    CLOSING_PRICE("AJ", "Closing Price/Index"),
    PREVIOUS_CLOSING_INDEX("ZZ", "Previous Closing Index"),
    INTRA_DAY_AUCTION("7", "Intra-Day Auction"),
    LEG_TRADE("3", "Leg Trade of a Synthetic Trade");

    private final String code;
    private final String description;

    TradeCondition(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static TradeCondition fromCode(String code) {
        if (code == null) return null;
        for (TradeCondition tc : values()) {
            if (tc.code.equals(code)) return tc;
        }
        return null;
    }
}
