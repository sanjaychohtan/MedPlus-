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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportsServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    private ReportsServiceImpl reportsService;

    @BeforeEach
    void setUp() {
        reportsService = new ReportsServiceImpl(productRepository, batchRepository, orderRepository, userRepository);
    }

    @Test
    void testGetPlatformMetrics() {
        when(productRepository.count()).thenReturn(10L);
        when(orderRepository.count()).thenReturn(5L);
        when(userRepository.count()).thenReturn(3L);

        Product p = Product.builder().name("Aspirin").build();
        Batch b = Batch.builder()
                .product(p)
                .b2bPrice(BigDecimal.valueOf(100))
                .quantityOnHand(10)
                .status("ACTIVE")
                .build();
        when(batchRepository.findAll()).thenReturn(Collections.singletonList(b));

        Order o = Order.builder()
                .orderStatus("PENDING")
                .totalAmount(BigDecimal.valueOf(500))
                .build();
        when(orderRepository.findAll()).thenReturn(Collections.singletonList(o));

        PlatformMetricsDto dto = reportsService.getPlatformMetrics();

        assertEquals(10, dto.getTotalProducts());
        assertEquals(5, dto.getTotalOrders());
        assertEquals(3, dto.getTotalUsers());
        assertEquals(BigDecimal.valueOf(1000), dto.getTotalInventoryValuation());
        assertEquals(1, dto.getPendingOrders());
    }

    @Test
    void testGetNearExpiryLots() {
        Product p = Product.builder().name("Paracetamol").build();
        Batch b1 = Batch.builder()
                .product(p)
                .batchNumber("B123")
                .expiryDate(LocalDate.now().plusDays(30))
                .quantityOnHand(100)
                .status("ACTIVE")
                .build();
        Batch b2 = Batch.builder()
                .product(p)
                .batchNumber("B456")
                .expiryDate(LocalDate.now().plusDays(120)) // should be excluded (>90 days)
                .quantityOnHand(100)
                .status("ACTIVE")
                .build();

        when(batchRepository.findAll()).thenReturn(Arrays.asList(b1, b2));

        List<NearExpiryLotDto> lots = reportsService.getNearExpiryLots();

        assertEquals(1, lots.size());
        assertEquals("B123", lots.getFirst().getBatchNumber());
        assertTrue(lots.getFirst().getDaysToExpiry() <= 30);
    }

    @Test
    void testGetLowStockAlerts() {
        Product p1 = Product.builder().name("Ibuporfen").sku("IBU-001").build();
        p1.setId("prod-1");
        Product p2 = Product.builder().name("Amoxicillin").sku("AMX-002").build();
        p2.setId("prod-2");

        when(productRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        // b1 available qty = 10 (low stock < 50)
        Batch b1 = Batch.builder()
                .product(p1)
                .quantityOnHand(20)
                .quantityReserved(10)
                .status("ACTIVE")
                .build();
        b1.calculateAvailableQuantity();

        // b2 available qty = 100 (not low stock)
        Batch b2 = Batch.builder()
                .product(p2)
                .quantityOnHand(120)
                .quantityReserved(20)
                .status("ACTIVE")
                .build();
        b2.calculateAvailableQuantity();

        when(batchRepository.findAll()).thenReturn(Arrays.asList(b1, b2));

        List<LowStockAlertDto> alerts = reportsService.getLowStockAlerts();

        assertEquals(1, alerts.size());
        assertEquals("prod-1", alerts.getFirst().getProductId());
        assertEquals(10, alerts.getFirst().getAvailableQuantity());
    }

    @Test
    void testGetSalesSummary() {
        Order o1 = Order.builder()
                .orderStatus("DELIVERED")
                .totalAmount(BigDecimal.valueOf(1500))
                .paymentMethod("CREDIT_CARD")
                .build();
        Order o2 = Order.builder()
                .orderStatus("PENDING")
                .totalAmount(BigDecimal.valueOf(500))
                .paymentMethod("COD")
                .build();

        when(orderRepository.findAll()).thenReturn(Arrays.asList(o1, o2));

        SalesSummaryDto summary = reportsService.getSalesSummary();

        assertEquals(BigDecimal.valueOf(1500), summary.getTotalRevenue());
        assertEquals(2, summary.getTotalOrdersCount());
        assertEquals(1L, summary.getOrdersByStatus().get("DELIVERED"));
        assertEquals(1L, summary.getOrdersByStatus().get("PENDING"));
        assertEquals(BigDecimal.valueOf(1500), summary.getRevenueByPaymentMethod().get("CREDIT_CARD"));
        assertNull(summary.getRevenueByPaymentMethod().get("COD")); // COD should be ignored since order status is PENDING (not SHIPPED or DELIVERED)
    }
}
