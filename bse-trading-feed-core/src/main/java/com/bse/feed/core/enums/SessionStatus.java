package com.bse.feed.core.enums;

/**
 * SessionStatus (tag 1409) values used in Logon/Logout messages.
 * Reference: BSE FAST-UDP Spec v2.02, Section 6.3
 */
public enum SessionStatus {

    SESSION_ACTIVE(0, "Session Active"),
    PASSWORD_DUE_TO_EXPIRE(2, "Password Due to Expire"),
    NEW_PASSWORD_NOT_COMPLIANT(3, "New session password does not comply with policy"),
    SESSION_LOGOUT_COMPLETE(4, "Session Logout Complete"),
    ACCOUNT_LOCKED(6, "Account Locked"),
    LOGONS_NOT_ALLOWED(7, "Logons Not Allowed"),
    PASSWORD_EXPIRED(8, "Password Expired"),
    OTHER(100, "Other");

    private final int code;
    private final String description;

    SessionStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static SessionStatus fromCode(int code) {
        for (SessionStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
