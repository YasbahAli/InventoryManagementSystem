package com.example.inventory.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;

@Service
public class ReportingService {

    private final ProductService productService;
    private final OrderService orderService;

    public ReportingService(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
    }

    /**
     * Get sales by product (orders completed)
     */
    public Map<String, Object> getSalesByProduct() {
        List<Order> completedOrders = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<String, Double> salesByProduct = new LinkedHashMap<>();
        completedOrders.forEach(order -> {
            String productName = order.getProduct() != null ? order.getProduct().getName() : "Unknown";
            Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
            salesByProduct.put(productName, salesByProduct.getOrDefault(productName, 0.0) + totalPrice);
        });

        // Sort by sales descending
        Map<String, Double> sortedSales = salesByProduct.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("labels", sortedSales.keySet().stream().collect(Collectors.toList()));
        result.put("data", sortedSales.values().stream().collect(Collectors.toList()));
        result.put("totalSales", sortedSales.values().stream().mapToDouble(Double::doubleValue).sum());
        return result;
    }

    /**
     * Get sales by category (orders completed)
     */
    public Map<String, Object> getSalesByCategory() {
        List<Order> completedOrders = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<String, Double> salesByCategory = new HashMap<>();
        completedOrders.forEach(order -> {
            if (order.getProduct() != null && order.getProduct().getCategory() != null) {
                String categoryName = order.getProduct().getCategory().getName();
                Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
                salesByCategory.put(categoryName, salesByCategory.getOrDefault(categoryName, 0.0) + totalPrice);
            }
        });

        // Sort by sales descending
        Map<String, Double> sortedSales = salesByCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("labels", sortedSales.keySet().stream().collect(Collectors.toList()));
        result.put("data", sortedSales.values().stream().collect(Collectors.toList()));
        result.put("totalSales", sortedSales.values().stream().mapToDouble(Double::doubleValue).sum());
        return result;
    }

    /**
     * Get low stock products (quantity below 10)
     */
    public List<Map<String, Object>> getLowStockProducts(int threshold) {
        return productService.getAllProducts().stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() < threshold)
                .sorted((a, b) -> Integer.compare(a.getQuantity(), b.getQuantity()))
                .map(product -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", product.getId());
                    item.put("name", product.getName());
                    item.put("quantity", product.getQuantity());
                    item.put("categoryName", product.getCategory() != null ? product.getCategory().getName() : "N/A");
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get order status distribution
     */
    public Map<String, Object> getOrderStatusDistribution() {
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("PENDING", 0L);
        statusCount.put("CONFIRMED", 0L);
        statusCount.put("SHIPPED", 0L);
        statusCount.put("COMPLETED", 0L);
        statusCount.put("CANCELLED", 0L);

        orderService.getAllOrders().forEach(order -> {
            String status = order.getStatus() != null ? order.getStatus().name() : "PENDING";
            statusCount.put(status, statusCount.get(status) + 1);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("labels", statusCount.keySet().stream().collect(Collectors.toList()));
        result.put("data", statusCount.values().stream().collect(Collectors.toList()));
        result.put("totalOrders", statusCount.values().stream().mapToLong(Long::longValue).sum());
        return result;
    }

    /**
     * Get monthly sales summary
     */
    public Map<String, Object> getMonthlySalesSummary(int months) {
        Map<YearMonth, Double> monthlySales = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Initialize months
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            monthlySales.put(ym, 0.0);
        }

        // Aggregate completed orders by month
        List<Order> completedOrders = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        completedOrders.forEach(order -> {
            if (order.getOrderDate() != null) {
                java.time.LocalDate orderLocalDate = order.getOrderDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                YearMonth orderMonth = YearMonth.from(orderLocalDate);
                if (monthlySales.containsKey(orderMonth)) {
                    Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
                    monthlySales.put(orderMonth, monthlySales.get(orderMonth) + totalPrice);
                }
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("labels", monthlySales.keySet().stream()
                .map(YearMonth::toString)
                .collect(Collectors.toList()));
        result.put("data", monthlySales.values().stream().collect(Collectors.toList()));
        result.put("totalSales", monthlySales.values().stream().mapToDouble(Double::doubleValue).sum());
        return result;
    }

    /**
     * Get inventory value summary
     */
    public Map<String, Object> getInventoryValueSummary() {
        List<Product> products = productService.getAllProducts();

        double totalInventoryValue = products.stream()
                .mapToDouble(p -> {
                    Integer qty = p.getQuantity() != null ? p.getQuantity() : 0;
                    Double price = p.getPrice() != null ? p.getPrice() : 0.0d;
                    return (qty != null ? qty : 0) * (price != null ? price : 0.0d);
                })
                .sum();

        Map<String, Double> valueByCategory = new HashMap<>();
        products.forEach(p -> {
            String categoryName = p.getCategory() != null ? p.getCategory().getName() : "Uncategorized";
            Integer qty = p.getQuantity() != null ? p.getQuantity() : 0;
            Double price = p.getPrice() != null ? p.getPrice() : 0.0d;
            double value = (qty != null ? qty : 0) * (price != null ? price : 0.0d);
            valueByCategory.put(categoryName, valueByCategory.getOrDefault(categoryName, 0.0d) + value);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("totalValue", totalInventoryValue);
        result.put("categoryValues", valueByCategory);
        result.put("totalProducts", products.size());
        result.put("averageValue", products.isEmpty() ? 0 : totalInventoryValue / products.size());
        return result;
    }

    /**
     * Get dashboard summary statistics
     */
    public Map<String, Object> getDashboardSummary() {
        List<Product> products = productService.getAllProducts();
        List<Order> allOrders = orderService.getAllOrders();
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        long lowStockCount = products.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() < 10)
                .count();

        double totalCompletedSales = completedOrders.stream()
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0d)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProducts", products.size());
        summary.put("totalOrders", allOrders.size());
        summary.put("completedOrders", completedOrders.size());
        summary.put("pendingOrders", pendingOrders);
        summary.put("lowStockCount", lowStockCount);
        summary.put("totalCompletedSales", totalCompletedSales);
        return summary;
    }
}
