package com.medsupply.platform.modules.auth.model;

/**
 * Standard enum for representing a user's current account lifecycle status.
 */
public enum UserStatus {
    PENDING_APPROVAL,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
