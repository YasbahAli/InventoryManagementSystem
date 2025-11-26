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

import com.example.inventory.entity.Category;
import com.example.inventory.entity.Product;

@Service
public class ProductCsvService {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductCsvService(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /**
     * Export all products to CSV format
     */
    public ByteArrayOutputStream exportProductsToCsv() throws IOException {
        List<Product> products = productService.getAllProducts();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Name", "Description", "Quantity", "Price", "Category", "Created At"))) {
            
            for (Product product : products) {
                csvPrinter.printRecord(
                        product.getId(),
                        product.getName(),
                        product.getDescription() != null ? product.getDescription() : "",
                        product.getQuantity() != null ? product.getQuantity() : 0,
                        product.getPrice() != null ? product.getPrice() : 0,
                        product.getCategory() != null ? product.getCategory().getName() : "",
                        product.getCreatedAt()
                );
            }
            csvPrinter.flush();
        }
        
        return outputStream;
    }

    /**
     * Import products from CSV file
     */
    public List<String> importProductsFromCsv(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        int rowNum = 0;
        
        try (InputStream inputStream = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase())) {
            
            for (CSVRecord csvRecord : csvParser) {
                rowNum++;
                try {
                    String name = csvRecord.get("Name");
                    String description = csvRecord.get("Description");
                    String quantityStr = csvRecord.get("Quantity");
                    String priceStr = csvRecord.get("Price");
                    String categoryName = csvRecord.get("Category");

                    // Validation
                    if (name == null || name.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Product name is required");
                        continue;
                    }
                    if (quantityStr == null || quantityStr.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Quantity is required");
                        continue;
                    }
                    if (priceStr == null || priceStr.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Price is required");
                        continue;
                    }

                    try {
                        int quantity = Integer.parseInt(quantityStr);
                        double price = Double.parseDouble(priceStr);

                        if (quantity < 0) {
                            errors.add("Row " + (rowNum + 1) + ": Quantity cannot be negative");
                            continue;
                        }
                        if (price < 0) {
                            errors.add("Row " + (rowNum + 1) + ": Price cannot be negative");
                            continue;
                        }

                        // Find or create category
                        Category category = null;
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            category = categoryService.getAllCategories().stream()
                                    .filter(c -> c.getName().equalsIgnoreCase(categoryName.trim()))
                                    .findFirst()
                                    .orElse(null);
                        }

                        Product product = new Product();
                        product.setName(name);
                        product.setDescription(description);
                        product.setQuantity(quantity);
                        product.setPrice(price);
                        product.setCategory(category);

                        productService.saveOrUpdateProduct(product);
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (rowNum + 1) + ": Invalid quantity or price format");
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
