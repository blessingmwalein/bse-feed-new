package com.bse.feed.core.enums;

/**
 * PriceLimitEvent (tag 32027) values for Price Band messages.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.4
 */
public enum PriceLimitEvent {

    BREACH(0, "Breach"),
    RELAXATION(1, "Relaxation"),
    VWAP_COMPUTATION(2, "VWAP Computation"),
    OTHER(3, "Other");

    private final int code;
    private final String description;

    PriceLimitEvent(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static PriceLimitEvent fromCode(int code) {
        for (PriceLimitEvent event : values()) {
            if (event.code == code) return event;
        }
        return null;
    }
}
