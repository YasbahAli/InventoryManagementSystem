package com.example.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.inventory.service.ReportingService;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /**
     * Main reporting dashboard page - accessible to all authenticated users
     */
    @GetMapping
    public String reportsDashboard(Model model) {
        model.addAttribute("summary", reportingService.getDashboardSummary());
        model.addAttribute("lowStockProducts", reportingService.getLowStockProducts(10));
        model.addAttribute("inventoryValue", reportingService.getInventoryValueSummary());
        return "reports/dashboard";
    }

    /**
     * Sales by product data (JSON for Chart.js)
     */
    @GetMapping("/api/sales-by-product")
    @ResponseBody
    public Map<String, Object> getSalesByProduct() {
        return reportingService.getSalesByProduct();
    }

    /**
     * Sales by category data (JSON for Chart.js)
     */
    @GetMapping("/api/sales-by-category")
    @ResponseBody
    public Map<String, Object> getSalesByCategory() {
        return reportingService.getSalesByCategory();
    }

    /**
     * Order status distribution data (JSON for Chart.js)
     */
    @GetMapping("/api/order-status")
    @ResponseBody
    public Map<String, Object> getOrderStatus() {
        return reportingService.getOrderStatusDistribution();
    }

    /**
     * Monthly sales summary data (JSON for Chart.js)
     */
    @GetMapping("/api/monthly-sales")
    @ResponseBody
    public Map<String, Object> getMonthlySales() {
        return reportingService.getMonthlySalesSummary(12);
    }

    /**
     * Low stock alert page - accessible to all authenticated users
     */
    @GetMapping("/low-stock")
    public String lowStockReport(Model model) {
        List<Map<String, Object>> lowStock = reportingService.getLowStockProducts(10);
        model.addAttribute("lowStockProducts", lowStock);
        model.addAttribute("lowStockTotal", lowStock.size());
        long outOfStock = lowStock.stream()
                .filter(m -> {
                    Object q = m.get("quantity");
                    return q instanceof Number && ((Number) q).intValue() == 0;
                })
                .count();
        long criticalCount = lowStock.stream()
                .filter(m -> {
                    Object q = m.get("quantity");
                    int val = q instanceof Number ? ((Number) q).intValue() : -1;
                    return val > 0 && val < 5;
                })
                .count();
        model.addAttribute("outOfStockCount", outOfStock);
        model.addAttribute("criticalCount", criticalCount);
        return "reports/low_stock";
    }

    /**
     * Sales report page - accessible to all authenticated users
     */
    @GetMapping("/sales")
    public String salesReport(Model model) {
        model.addAttribute("salesByProduct", reportingService.getSalesByProduct());
        model.addAttribute("salesByCategory", reportingService.getSalesByCategory());
        model.addAttribute("monthlySales", reportingService.getMonthlySalesSummary(12));
        return "reports/sales";
    }

    /**
     * Inventory summary page - admin-only
     */
    @GetMapping("/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public String inventoryReport(Model model) {
        model.addAttribute("inventoryValue", reportingService.getInventoryValueSummary());
        model.addAttribute("summary", reportingService.getDashboardSummary());
        // compute category percentages to avoid unsupported SpEL operations in templates
        Object iv = model.getAttribute("inventoryValue");
        if (iv instanceof Map) {
            Map<?, ?> inv = (Map<?, ?>) iv;
            Object totalObj = inv.get("totalValue");
            double total = 0.0;
            if (totalObj instanceof Number) {
                total = ((Number) totalObj).doubleValue();
            }
            Map<String, Double> percentages = new java.util.HashMap<>();
            Object catValsObj = inv.get("categoryValues");
            if (catValsObj instanceof Map) {
                Map<?, ?> catVals = (Map<?, ?>) catValsObj;
                for (Map.Entry<?, ?> e : catVals.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    double val = 0.0;
                    if (e.getValue() instanceof Number) {
                        val = ((Number) e.getValue()).doubleValue();
                    }
                    double pct = total > 0.0 ? (val / total) : 0.0;
                    percentages.put(key, pct);
                }
            }
            model.addAttribute("categoryPercentages", percentages);
        }
        return "reports/inventory";
    }
}
