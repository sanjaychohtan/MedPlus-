package com.medsupply.platform.modules.order.service;

import java.util.UUID;

public class OrderItemInput {
    private UUID productId;
    private int quantity;

    public OrderItemInput() {}

    public OrderItemInput(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
