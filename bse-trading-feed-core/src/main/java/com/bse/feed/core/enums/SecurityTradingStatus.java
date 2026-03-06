package com.bse.feed.core.enums;

/**
 * SecurityTradingStatus (tag 326) values.
 * Indicates the current trading session for an instrument.
 * Reference: BSE FAST-UDP Spec v2.02, Sections 6.5.2
 */
public enum SecurityTradingStatus {

    HALT(2, "Halt"),
    REGULAR_TRADING(17, "Regular Trading"),
    MARKET_CLOSE(18, "Market Close"),
    OPENING_AUCTION_CALL(21, "Opening Auction Call"),
    POST_CLOSE(26, "Post-Close"),
    PRE_TRADING(100, "Pre-Trading"),
    CLOSING_AUCTION_CALL(102, "Closing Auction Call"),
    END_OF_POST_CLOSE(103, "End of Post Close"),
    RE_OPENING_AUCTION_CALL(104, "Re-Opening Auction Call"),
    PAUSE(111, "Pause"),
    ORDER_ENTRY(113, "Order Entry"),
    CLOSING_PRICE_CROSS(120, "Closing Price Cross"),
    INTRADAY_AUCTION_CALL(122, "Intraday Auction Call"),
    RESERVED(130, "Reserved"),
    START_OF_AON_ORDER_ENTRY(131, "Start of AON Order Entry"),
    END_OF_AON_ORDER_ENTRY(132, "End of AON Order Entry"),
    AUCTION_INITIATION(133, "Auction Initiation"),
    START_OF_AUCTION_CALL(134, "Start of Auction Call"),
    END_OF_AUCTION_CALL(135, "End of Auction Call"),
    AUCTION_EXECUTION(136, "Auction Execution"),
    NO_ACTIVE_SESSION(199, "No Active Session");

    private final int code;
    private final String description;

    SecurityTradingStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean isTrading() {
        return this == REGULAR_TRADING || this == CLOSING_PRICE_CROSS;
    }

    public boolean isAuction() {
        return this == OPENING_AUCTION_CALL || this == CLOSING_AUCTION_CALL
                || this == RE_OPENING_AUCTION_CALL || this == INTRADAY_AUCTION_CALL;
    }

    public static SecurityTradingStatus fromCode(int code) {
        for (SecurityTradingStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
