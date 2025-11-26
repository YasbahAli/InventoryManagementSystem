package com.example.inventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.Supplier;

@Service
public class OrderCsvService {

    private final OrderService orderService;
    private final ProductService productService;
    private final SupplierService supplierService;

    public OrderCsvService(OrderService orderService, ProductService productService, SupplierService supplierService) {
        this.orderService = orderService;
        this.productService = productService;
        this.supplierService = supplierService;
    }

    /**
     * Export all orders to CSV format
     */
    public ByteArrayOutputStream exportOrdersToCsv() throws IOException {
        List<Order> orders = orderService.getAllOrders();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Product", "Quantity", "Status", "Total Price", "Supplier", "Order Date"))) {
            
            for (Order order : orders) {
                csvPrinter.printRecord(
                        order.getId(),
                        order.getProduct() != null ? order.getProduct().getName() : "",
                        order.getQuantity() != null ? order.getQuantity() : 0,
                        order.getStatus() != null ? order.getStatus().name() : "",
                        order.getTotalPrice() != null ? order.getTotalPrice() : 0,
                        order.getSupplier() != null ? order.getSupplier().getName() : "",
                        order.getOrderDate()
                );
            }
            csvPrinter.flush();
        }
        
        return outputStream;
    }

    /**
     * Import orders from CSV file
     */
    public List<String> importOrdersFromCsv(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        int rowNum = 0;
        
        try (InputStream inputStream = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase())) {
            
            for (CSVRecord csvRecord : csvParser) {
                rowNum++;
                try {
                    String productName = csvRecord.get("Product");
                    String quantityStr = csvRecord.get("Quantity");
                    String statusStr = csvRecord.get("Status");
                    String supplierName = csvRecord.get("Supplier");

                    // Validation
                    if (productName == null || productName.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Product name is required");
                        continue;
                    }
                    if (quantityStr == null || quantityStr.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Quantity is required");
                        continue;
                    }

                    try {
                        int quantity = Integer.parseInt(quantityStr);

                        if (quantity <= 0) {
                            errors.add("Row " + (rowNum + 1) + ": Quantity must be greater than 0");
                            continue;
                        }

                        // Find product
                        Product product = productService.getAllProducts().stream()
                                .filter(p -> p.getName().equalsIgnoreCase(productName.trim()))
                                .findFirst()
                                .orElse(null);

                        if (product == null) {
                            errors.add("Row " + (rowNum + 1) + ": Product '" + productName + "' not found");
                            continue;
                        }

                        // Find supplier (optional)
                        Supplier supplier = null;
                        if (supplierName != null && !supplierName.trim().isEmpty()) {
                            supplier = supplierService.getAllSuppliers().stream()
                                    .filter(s -> s.getName().equalsIgnoreCase(supplierName.trim()))
                                    .findFirst()
                                    .orElse(null);
                        }

                        // Parse status (optional, defaults to PENDING)
                        OrderStatus status = OrderStatus.PENDING;
                        if (statusStr != null && !statusStr.trim().isEmpty()) {
                            try {
                                status = OrderStatus.valueOf(statusStr.trim().toUpperCase());
                            } catch (IllegalArgumentException e) {
                                errors.add("Row " + (rowNum + 1) + ": Invalid status '" + statusStr + "'");
                                continue;
                            }
                        }

                        Order order = new Order();
                        order.setProduct(product);
                        order.setQuantity(quantity);
                        order.setStatus(status);
                        order.setSupplier(supplier);

                        orderService.saveOrUpdateOrder(order);
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (rowNum + 1) + ": Invalid quantity format");
                    }
                } catch (Exception e) {
                    errors.add("Row " + (rowNum + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Error reading file: " + e.getMessage());
        }
        
        return errors;
    }
}
