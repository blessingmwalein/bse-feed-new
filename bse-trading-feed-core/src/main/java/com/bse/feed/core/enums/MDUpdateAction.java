package com.bse.feed.core.enums;

/**
 * MDUpdateAction (tag 279) values for incremental refresh messages.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.5.4
 */
public enum MDUpdateAction {

    NEW(0, "New"),
    CHANGE(1, "Change"),
    DELETE(2, "Delete");

    private final int code;
    private final String description;

    MDUpdateAction(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static MDUpdateAction fromCode(int code) {
        for (MDUpdateAction action : values()) {
            if (action.code == code) return action;
        }
        return null;
    }
}
