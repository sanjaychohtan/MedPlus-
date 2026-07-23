package com.medsupply.platform.modules.reports.service.impl;

import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import com.medsupply.platform.modules.inventory.repository.BatchRepository;
import com.medsupply.platform.modules.inventory.repository.ProductRepository;
import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.repository.OrderRepository;
import com.medsupply.platform.modules.reports.dto.LowStockAlertDto;
import com.medsupply.platform.modules.reports.dto.NearExpiryLotDto;
import com.medsupply.platform.modules.reports.dto.PlatformMetricsDto;
import com.medsupply.platform.modules.reports.dto.SalesSummaryDto;
import com.medsupply.platform.modules.reports.service.ReportsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportsServiceImpl implements ReportsService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ReportsServiceImpl(ProductRepository productRepository,
                              BatchRepository batchRepository,
                              OrderRepository orderRepository,
                              UserRepository userRepository) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "metrics", key = "'platform_metrics'")
    public PlatformMetricsDto getPlatformMetrics() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();

        // Calculate total valuation across active batches
        List<Batch> batches = batchRepository.findAll();
        BigDecimal totalValuation = batches.stream()
                .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .map(b -> b.getB2bPrice().multiply(BigDecimal.valueOf(b.getQuantityOnHand())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate order states
        List<Order> orders = orderRepository.findAll();
        long pending = orders.stream()
                .filter(o -> "PENDING".equalsIgnoreCase(o.getOrderStatus()) || "PENDING_APPROVAL".equalsIgnoreCase(o.getOrderStatus()))
                .count();

        long completed = orders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus()))
                .count();

        return PlatformMetricsDto.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalInventoryValuation(totalValuation)
                .pendingOrders(pending)
                .completedOrders(completed)
                .build();
    }

    @Override
    @Cacheable(value = "products", key = "'near_expiry'")
    public List<NearExpiryLotDto> getNearExpiryLots() {
        LocalDate now = LocalDate.now();
        LocalDate boundary = now.plusDays(90);

        List<Batch> batches = batchRepository.findAll();
        List<NearExpiryLotDto> nearExpiryLots = new ArrayList<>();

        for (Batch batch : batches) {
            if ("ACTIVE".equalsIgnoreCase(batch.getStatus()) && batch.getExpiryDate() != null) {
                LocalDate expiry = batch.getExpiryDate();
                if (expiry.isAfter(now.minusDays(1)) && expiry.isBefore(boundary)) {
                    long daysToExpiry = ChronoUnit.DAYS.between(now, expiry);
                    nearExpiryLots.add(NearExpiryLotDto.builder()
                            .batchId(batch.getId())
                            .productName(batch.getProduct().getName())
                            .batchNumber(batch.getBatchNumber())
                            .expiryDate(expiry)
                            .daysToExpiry(daysToExpiry)
                            .quantityOnHand(batch.getQuantityOnHand())
                            .build());
                }
            }
        }

        // Sort by earliest expiry first
        nearExpiryLots.sort((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()));
        return nearExpiryLots;
    }

    @Override
    @Cacheable(value = "products", key = "'low_stock'")
    public List<LowStockAlertDto> getLowStockAlerts() {
        List<Product> products = productRepository.findAll();
        List<Batch> batches = batchRepository.findAll();

        // Group active batch quantities by product id
        Map<String, Integer> productQuantities = batches.stream()
                .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.groupingBy(
                        b -> b.getProduct().getId(),
                        Collectors.summingInt(Batch::getQuantityAvailable)
                ));

        List<LowStockAlertDto> lowStockAlerts = new ArrayList<>();
        int threshold = 50; // standard safety threshold

        for (Product product : products) {
            int qty = productQuantities.getOrDefault(product.getId(), 0);
            if (qty < threshold) {
                lowStockAlerts.add(LowStockAlertDto.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .sku(product.getSku())
                        .availableQuantity(qty)
                        .safetyThreshold(threshold)
                        .build());
            }
        }

        // Sort by lowest quantity first
        lowStockAlerts.sort((a, b) -> Integer.compare(a.getAvailableQuantity(), b.getAvailableQuantity()));
        return lowStockAlerts;
    }

    @Override
    @Cacheable(value = "metrics", key = "'sales_summary'")
    public SalesSummaryDto getSalesSummary() {
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus()) || "SHIPPED".equalsIgnoreCase(o.getOrderStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> orderBreakdown = orders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));

        // Aggregate revenue by payment method
        Map<String, BigDecimal> revenueByPayment = new HashMap<>();
        for (Order order : orders) {
            if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus()) || "SHIPPED".equalsIgnoreCase(order.getOrderStatus())) {
                String method = order.getPaymentMethod();
                if (method == null || method.trim().isEmpty()) {
                    method = "UNKNOWN";
                }
                BigDecimal existing = revenueByPayment.getOrDefault(method, BigDecimal.ZERO);
                revenueByPayment.put(method, existing.add(order.getTotalAmount()));
            }
        }

        return SalesSummaryDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrdersCount(orders.size())
                .ordersByStatus(orderBreakdown)
                .revenueByPaymentMethod(revenueByPayment)
                .build();
    }
}
