package com.example.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.inventory.entity.Category;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.Supplier;

class ReportingServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ReportingService reportingService;

    private List<Order> testOrders;
    private List<Product> testProducts;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        Supplier testSupplier = new Supplier();
        testSupplier.setId(1L);
        testSupplier.setName("Test Supplier");

        testProducts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setId((long) i);
            product.setName("Product " + i);
            product.setCategory(testCategory);
            product.setQuantity(50 - (i * 10));
            product.setPrice(100.0 * i);
            testProducts.add(product);
        }

        testOrders = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setProduct(testProducts.get((i - 1) % 3));
            order.setQuantity(10);
            order.setTotalPrice(1000.0 * i);
            order.setSupplier(testSupplier);
            
            if (i <= 2) {
                order.setStatus(OrderStatus.COMPLETED);
            } else if (i <= 4) {
                order.setStatus(OrderStatus.PENDING);
            } else {
                order.setStatus(OrderStatus.CANCELLED);
            }
            
            testOrders.add(order);
        }
    }

    @Test
    void testGetSalesByProduct() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> sales = reportingService.getSalesByProduct();

        assertNotNull(sales);
        assertTrue(sales.containsKey("labels"));
        assertTrue(sales.containsKey("data"));
        assertTrue(sales.containsKey("totalSales"));
    }

    @Test
    void testGetSalesByProductOnlyCompletedOrders() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> sales = reportingService.getSalesByProduct();

        assertNotNull(sales);
        assertTrue((Double) sales.get("totalSales") > 0);
    }

    @Test
    void testGetSalesByProductEmptyOrders() {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());

        Map<String, Object> sales = reportingService.getSalesByProduct();

        assertNotNull(sales);
        assertEquals(0.0, sales.get("totalSales"));
    }

    @Test
    void testGetSalesByCategory() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> sales = reportingService.getSalesByCategory();

        assertNotNull(sales);
        assertTrue(sales.containsKey("labels"));
        assertTrue(sales.containsKey("data"));
        assertTrue(sales.containsKey("totalSales"));
    }

    @Test
    void testGetSalesByCategoryOnlyCompletedOrders() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> sales = reportingService.getSalesByCategory();

        assertNotNull(sales);
        assertTrue((Double) sales.get("totalSales") > 0);
    }

    @Test
    void testGetSalesByCategoryEmptyOrders() {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());

        Map<String, Object> sales = reportingService.getSalesByCategory();

        assertNotNull(sales);
        assertEquals(0.0, sales.get("totalSales"));
    }

    @Test
    void testGetLowStockProductsWithThreshold() {
        when(productService.getAllProducts()).thenReturn(testProducts);

        List<Map<String, Object>> lowStock = reportingService.getLowStockProducts(20);

        assertNotNull(lowStock);
        for (Map<String, Object> item : lowStock) {
            assertTrue((Integer) item.get("quantity") < 20);
        }
    }

    @Test
    void testGetLowStockProductsEmptyProducts() {
        when(productService.getAllProducts()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> lowStock = reportingService.getLowStockProducts(20);

        assertNotNull(lowStock);
        assertTrue(lowStock.isEmpty());
    }

    @Test
    void testGetOrderStatusDistribution() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> distribution = reportingService.getOrderStatusDistribution();

        assertNotNull(distribution);
        assertTrue(distribution.containsKey("labels"));
        assertTrue(distribution.containsKey("data"));
        assertTrue(distribution.containsKey("totalOrders"));
        assertEquals(5L, distribution.get("totalOrders"));
    }

    @Test
    void testGetOrderStatusDistributionEmptyOrders() {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());

        Map<String, Object> distribution = reportingService.getOrderStatusDistribution();

        assertNotNull(distribution);
        assertEquals(0L, distribution.get("totalOrders"));
    }

    @Test
    void testGetMonthlySalesSummary() {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        Map<String, Object> monthlySales = reportingService.getMonthlySalesSummary(12);

        assertNotNull(monthlySales);
        assertTrue(monthlySales.containsKey("labels"));
        assertTrue(monthlySales.containsKey("data"));
        assertTrue(monthlySales.containsKey("totalSales"));
    }

    @Test
    void testGetMonthlySalesSummaryEmptyOrders() {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());

        Map<String, Object> monthlySales = reportingService.getMonthlySalesSummary(12);

        assertNotNull(monthlySales);
        assertEquals(0.0, monthlySales.get("totalSales"));
    }

    @Test
    void testGetInventoryValueSummary() {
        when(productService.getAllProducts()).thenReturn(testProducts);

        Map<String, Object> summary = reportingService.getInventoryValueSummary();

        assertNotNull(summary);
        assertTrue(summary.containsKey("totalValue"));
        assertTrue(summary.containsKey("categoryValues"));
        assertTrue(summary.containsKey("totalProducts"));
        assertTrue((Double) summary.get("totalValue") > 0);
    }

    @Test
    void testGetInventoryValueSummaryEmptyProducts() {
        when(productService.getAllProducts()).thenReturn(new ArrayList<>());

        Map<String, Object> summary = reportingService.getInventoryValueSummary();

        assertNotNull(summary);
        assertEquals(0.0, summary.get("totalValue"));
        assertEquals(0, summary.get("totalProducts"));
    }

    @Test
    void testGetDashboardSummary() {
        when(orderService.getAllOrders()).thenReturn(testOrders);
        when(productService.getAllProducts()).thenReturn(testProducts);

        Map<String, Object> dashboard = reportingService.getDashboardSummary();

        assertNotNull(dashboard);
        assertTrue(dashboard.containsKey("totalOrders"));
        assertTrue(dashboard.containsKey("totalProducts"));
        assertTrue(dashboard.containsKey("completedOrders"));
        assertTrue(dashboard.containsKey("pendingOrders"));
        assertTrue(dashboard.containsKey("lowStockCount"));
        assertTrue(dashboard.containsKey("totalCompletedSales"));
    }

    @Test
    void testGetDashboardSummaryKPIValues() {
        when(orderService.getAllOrders()).thenReturn(testOrders);
        when(productService.getAllProducts()).thenReturn(testProducts);

        Map<String, Object> dashboard = reportingService.getDashboardSummary();

        assertNotNull(dashboard);
        assertEquals(5, dashboard.get("totalOrders"));
        assertEquals(3, dashboard.get("totalProducts"));
        assertEquals(2, dashboard.get("completedOrders"));
    }

    @Test
    void testGetDashboardSummaryEmptyData() {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());
        when(productService.getAllProducts()).thenReturn(new ArrayList<>());

        Map<String, Object> dashboard = reportingService.getDashboardSummary();

        assertNotNull(dashboard);
        assertEquals(0, dashboard.get("totalOrders"));
        assertEquals(0, dashboard.get("totalProducts"));
        assertEquals(0.0, dashboard.get("totalCompletedSales"));
    }
}
