package com.bse.feed.core.enums;

/**
 * MDEntryType (tag 269) values.
 * Reference: BSE FAST-UDP Spec v2.02, Sections 6.5.3 & 6.5.4
 */
public enum MDEntryType {

    BID("0", "Bid"),
    OFFER("1", "Offer"),
    TRADE("2", "Trade"),
    INDEX_VALUE("3", "Index Value"),
    OPENING_PRICE("4", "Opening Price"),
    CLOSING_PRICE("5", "Closing Price"),
    HIGH_PRICE("7", "High Price"),
    LOW_PRICE("8", "Low Price"),
    VWAP("9", "VWAP"),
    THEORETICAL_PRICE("10", "Theoretical Price"),
    DELTA("11", "Delta"),
    GAMMA("12", "Gamma"),
    VEGA("13", "Vega"),
    THETA("14", "Theta"),
    RHO("15", "Rho"),
    VOLATILITY("16", "Volatility"),
    LIFETIME_HIGH("17", "Lifetime High"),
    LIFETIME_LOW("18", "Lifetime Low"),
    IMBALANCE("A", "Imbalance"),
    VOLUME("B", "Volume"),
    OPEN_INTEREST("C", "Open Interest"),
    SPOT_PRICE("D", "Spot Price"),
    EMPTY_ORDER_BOOK("J", "Empty Order Book"),
    HIGHEST_BID_PRICE("N", "Highest Bid Price"),
    LOWEST_OFFER_PRICE("O", "Lowest Offer Price"),
    AUCTION_CLEARING_PRICE("Q", "Auction Clearing Price"),
    MARKET_BID("b", "Market Bid"),
    MARKET_OFFER("c", "Market Offer"),
    PREVIOUS_CLOSE("f", "Previous Close"),
    PRICE_BAND("g", "Price Band"),
    AON_STATISTICS("o", "AON Statistics"),
    BUY_ORDER_QTY("u", "Buy Order Qty"),
    SELL_ORDER_QTY("v", "Sell Order Qty"),
    MARKET_CAPITALIZATION("w", "Market Capitalization"),
    NEWS("w2", "News"),  // Used in MarketDataRequest only
    TURNOVER("x", "Turnover"),
    NUMBER_OF_TRADES("y", "Number of Trades"),
    NO_STATISTICS("z", "No Statistics");

    private final String code;
    private final String description;

    MDEntryType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public boolean isBidSide() {
        return this == BID || this == MARKET_BID;
    }

    public boolean isOfferSide() {
        return this == OFFER || this == MARKET_OFFER;
    }

    public boolean isOrderBookEntry() {
        return this == BID || this == OFFER || this == MARKET_BID || this == MARKET_OFFER;
    }

    public boolean isStatistic() {
        return this == OPENING_PRICE || this == CLOSING_PRICE || this == HIGH_PRICE
                || this == LOW_PRICE || this == VWAP || this == VOLUME || this == TURNOVER
                || this == NUMBER_OF_TRADES || this == HIGHEST_BID_PRICE || this == LOWEST_OFFER_PRICE
                || this == PREVIOUS_CLOSE || this == BUY_ORDER_QTY || this == SELL_ORDER_QTY
                || this == LIFETIME_HIGH || this == LIFETIME_LOW;
    }

    public static MDEntryType fromCode(String code) {
        if (code == null) return null;
        for (MDEntryType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return null;
    }
}
