package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Enterprise state machine tracking order execution stages.
 */
public enum OrderStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean isValidTransition(OrderStatus nextStatus) {
        switch (this) {
            case PENDING_APPROVAL:
                return nextStatus == APPROVED || nextStatus == REJECTED || nextStatus == CANCELLED;
            case APPROVED:
                return nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED:
                return nextStatus == DELIVERED || nextStatus == CANCELLED;
            case REJECTED:
            case CANCELLED:
            case DELIVERED:
                return false; // Terminal states
            default:
                return false;
        }
    }
}
