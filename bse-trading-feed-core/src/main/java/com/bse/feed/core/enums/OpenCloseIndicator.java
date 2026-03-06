package com.bse.feed.core.enums;

/**
 * OpenCloseIndicator (tag 30002) - method used to compute opening or closing price.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.3 & 6.5.4
 */
public enum OpenCloseIndicator {

    AUCTION_TRADE(1, "Auction Trade"),
    REGULAR_TRADE(2, "Regular Trade"),
    MID_POINT(3, "Mid-Point"),
    VWAP(6, "VWAP"),
    LAST_REGULAR_TRADE(7, "Last Regular Trade"),
    LAST_AUCTION(8, "Last Auction"),
    PREVIOUS_CLOSE(9, "Previous Close"),
    MANUAL(10, "Manual"),
    VWAP_OF_LAST_N_TRADES(19, "VWAP of Last n Trades"),
    REFERENCE_PRICE(20, "Reference Price"),
    PRICE_UNAVAILABLE(21, "Price Unavailable"),
    BEST_BID(22, "Best Bid"),
    BEST_OFFER(23, "Best Offer"),
    THEORETICAL_PRICE(24, "Theoretical Price"),
    VWAP_OF_N_MEMBER_TRADES(30, "VWAP of n Member Trades"),
    VWAP_OF_EXTENDED_TRADES(31, "VWAP of Extended Trades");

    private final int code;
    private final String description;

    OpenCloseIndicator(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static OpenCloseIndicator fromCode(int code) {
        for (OpenCloseIndicator indicator : values()) {
            if (indicator.code == code) return indicator;
        }
        return null;
    }
}
