package com.example.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.inventory.service.ReportingService;

@Controller
public class DashboardController {

    private final ReportingService reportingService;

    public DashboardController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/")
    public String redirectToDashboard() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", reportingService.getDashboardSummary());
        model.addAttribute("inventoryValue", reportingService.getInventoryValueSummary());
        model.addAttribute("lowStockProducts", reportingService.getLowStockProducts(20));
        return "dashboard";
    }
}
