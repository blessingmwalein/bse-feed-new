package com.bse.feed.core.enums;

/**
 * MDSubBookType (tag 1173) values.
 * Each instrument is traded across multiple sub books.
 * Reference: BSE FAST-UDP Spec v2.02, multiple sections
 */
public enum MDSubBookType {

    REGULAR(1, "Regular"),
    OFF_BOOK(2, "Off-Book"),
    ODD_LOT(3, "Odd Lot"),
    BLOCK_TRADE(4, "Block Trade"),
    ALL_OR_NONE(5, "All or None"),
    EARLY_SETTLEMENT(6, "Early Settlement"),
    AUCTION(7, "Auction"),
    BULLETIN_BOARD(9, "Bulletin Board"),
    NEGOTIATED_TRADES(11, "Negotiated Trades");

    private final int code;
    private final String description;

    MDSubBookType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static MDSubBookType fromCode(int code) {
        for (MDSubBookType type : values()) {
            if (type.code == code) return type;
        }
        return REGULAR; // default per spec
    }
}
