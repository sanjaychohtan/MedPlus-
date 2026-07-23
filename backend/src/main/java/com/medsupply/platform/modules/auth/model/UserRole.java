package com.medsupply.platform.modules.auth.model;

/**
 * Standard business-level role enumeration for Role-Based Access Control (RBAC).
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    WAREHOUSE_STAFF,
    SALESMAN,
    DELIVERY_BOY,
    B2B_CUSTOMER,
    B2C_CUSTOMER
}
