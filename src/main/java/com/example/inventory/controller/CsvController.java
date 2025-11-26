package com.example.inventory.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.inventory.service.OrderCsvService;
import com.example.inventory.service.ProductCsvService;

@Controller
@RequestMapping("/csv")
public class CsvController {

    private final ProductCsvService productCsvService;
    private final OrderCsvService orderCsvService;

    public CsvController(ProductCsvService productCsvService, OrderCsvService orderCsvService) {
        this.productCsvService = productCsvService;
        this.orderCsvService = orderCsvService;
    }

    // ===== PRODUCT CSV ENDPOINTS =====

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProducts() throws IOException {
        ByteArrayOutputStream outputStream = productCsvService.exportProductsToCsv();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "products_" + timestamp + ".csv";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(outputStream.toByteArray());
    }

    @GetMapping("/products/import")
    public String productImportForm() {
        return "csv/product_import";
    }

    @PostMapping("/products/import")
    public String importProducts(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("errorMessage", "Please select a file to upload");
                return "csv/product_import";
            }

            List<String> errors = productCsvService.importProductsFromCsv(file);
            
            if (errors.isEmpty()) {
                model.addAttribute("successMessage", "Products imported successfully!");
            } else {
                model.addAttribute("errorMessages", errors);
                model.addAttribute("warningMessage", "Some rows had errors. Successful rows were imported.");
            }
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error processing file: " + e.getMessage());
        }
        
        return "csv/product_import";
    }

    // ===== ORDER CSV ENDPOINTS =====

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() throws IOException {
        ByteArrayOutputStream outputStream = orderCsvService.exportOrdersToCsv();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "orders_" + timestamp + ".csv";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(outputStream.toByteArray());
    }

    @GetMapping("/orders/import")
    public String orderImportForm() {
        return "csv/order_import";
    }

    @PostMapping("/orders/import")
    public String importOrders(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("errorMessage", "Please select a file to upload");
                return "csv/order_import";
            }

            List<String> errors = orderCsvService.importOrdersFromCsv(file);
            
            if (errors.isEmpty()) {
                model.addAttribute("successMessage", "Orders imported successfully!");
            } else {
                model.addAttribute("errorMessages", errors);
                model.addAttribute("warningMessage", "Some rows had errors. Successful rows were imported.");
            }
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error processing file: " + e.getMessage());
        }
        
        return "csv/order_import";
    }
}
