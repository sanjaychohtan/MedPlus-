package com.medsupply.platform.modules.order.service;

import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.model.Invoice;
import com.medsupply.platform.modules.order.model.Coupon;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<Order> getAllOrders();
    List<Order> getOrdersByCustomer(UUID customerId);
    Order createOrder(UUID customerId, String orderType, List<OrderItemInput> items, String paymentMethod, String deliveryAddress, String poNumber, String prescriptionUrl, String couponCode);
    Order updateOrderStatus(UUID orderId, String status);
    
    Invoice generateInvoice(UUID orderId);
    List<Invoice> getInvoicesByCustomer(UUID customerId);
    
    Coupon validateCoupon(String code, BigDecimal orderAmount);
}
