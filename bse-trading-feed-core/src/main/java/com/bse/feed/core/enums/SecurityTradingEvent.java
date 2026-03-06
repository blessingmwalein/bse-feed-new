package com.bse.feed.core.enums;

/**
 * SecurityTradingEvent (tag 1174) values.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.2
 */
public enum SecurityTradingEvent {

    MARKET_ORDER_EXTENSION(1, "Market Order Extension"),
    PRICE_MONITORING_EXTENSION(100, "Price Monitoring Extension"),
    EXTENDED_BY_MARKET_OPERATIONS(101, "Extended by Market Operations"),
    SHORTENED_BY_MARKET_OPERATIONS(102, "Shortened by Market Operations"),
    CIRCUIT_BREAKER_TRIPPED(103, "Circuit Breaker Tripped");

    private final int code;
    private final String description;

    SecurityTradingEvent(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static SecurityTradingEvent fromCode(int code) {
        for (SecurityTradingEvent event : values()) {
            if (event.code == code) return event;
        }
        return null;
    }
}
