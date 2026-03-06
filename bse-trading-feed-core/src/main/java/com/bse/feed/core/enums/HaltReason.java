package com.bse.feed.core.enums;

/**
 * HaltReason (tag 327) values.
 * Reference: BSE FAST-UDP Spec v2.02, Section 9
 */
public enum HaltReason {

    REASON_NOT_AVAILABLE(100, "Reason not available"),
    CIRCUIT_BREAKER_TRIPPED(101, "Instrument-level circuit breaker tripped"),
    INSTRUMENT_HALTED(102, "Instrument Status is Halted"),
    MATCHING_PARTITION_SUSPENDED(9998, "Matching partition suspended"),
    SYSTEM_SUSPENDED(9999, "System suspended");

    private final int code;
    private final String description;

    HaltReason(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static HaltReason fromCode(int code) {
        for (HaltReason reason : values()) {
            if (reason.code == code) return reason;
        }
        return REASON_NOT_AVAILABLE;
    }
}
