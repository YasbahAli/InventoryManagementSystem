package com.example.inventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.inventory.entity.Category;
import com.example.inventory.entity.Product;

class ProductCsvServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductCsvService productCsvService;

    private List<Product> testProducts;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testProducts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setId((long) i);
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setQuantity(10 * i);
            product.setPrice(10.0 * i);
            product.setCategory(testCategory);
            testProducts.add(product);
        }
    }

    @Test
    void testExportProductsToCsv() throws IOException {
        when(productService.getAllProducts()).thenReturn(testProducts);

        ByteArrayOutputStream result = productCsvService.exportProductsToCsv();

        assertNotNull(result);
        assertTrue(result.size() > 0);
        String csvContent = result.toString();
        assertTrue(csvContent.contains("ID"));
        assertTrue(csvContent.contains("Name"));
        assertTrue(csvContent.contains("Product 1"));
        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void testExportEmptyProductList() throws IOException {
        when(productService.getAllProducts()).thenReturn(new ArrayList<>());

        ByteArrayOutputStream result = productCsvService.exportProductsToCsv();

        assertNotNull(result);
        String csvContent = result.toString();
        // Should still have headers
        assertTrue(csvContent.contains("ID"));
        assertTrue(csvContent.contains("Name"));
    }

    @Test
    void testImportProductsWithValidCsv() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product 1,Description 1,50,25.0,Electronics\n" +
                "Test Product 2,Description 2,100,30.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        when(categoryService.getAllCategories()).thenReturn(List.of(testCategory));
        when(productService.saveOrUpdateProduct(any())).thenReturn(new Product());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertEquals(0, errors.size());
        verify(productService, times(2)).saveOrUpdateProduct(any());
    }

    @Test
    void testImportProductsWithMissingName() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                ",Description 1,50,25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("name is required"));
    }

    @Test
    void testImportProductsWithInvalidQuantity() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,invalid,25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Invalid quantity") || errors.get(0).contains("format"));
    }

    @Test
    void testImportProductsWithNegativePrice() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,50,-25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("cannot be negative") || errors.get(0).contains("Price"));
    }

    @Test
    void testImportProductsWithMissingQuantity() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,,25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Quantity is required"));
    }

    @Test
    void testImportProductsWithMissingPrice() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,50,,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Price is required"));
    }

    @Test
    void testImportProductsWithValidCategoryLookup() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,50,25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        when(categoryService.getAllCategories()).thenReturn(List.of(testCategory));
        when(productService.saveOrUpdateProduct(any())).thenReturn(new Product());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertEquals(0, errors.size());
        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void testImportProductsWithoutCategory() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,50,25.0,\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.saveOrUpdateProduct(any())).thenReturn(new Product());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertEquals(0, errors.size());
        verify(productService, times(1)).saveOrUpdateProduct(any());
    }

    @Test
    void testImportEmptyFile() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        assertEquals(0, errors.size());
    }

    @Test
    void testImportProductsWithZeroQuantity() throws IOException {
        String csvContent = "Name,Description,Quantity,Price,Category\n" +
                "Test Product,Description 1,0,25.0,Electronics\n";

        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.saveOrUpdateProduct(any())).thenReturn(new Product());

        List<String> errors = productCsvService.importProductsFromCsv(file);

        // Zero quantity should be allowed
        assertEquals(0, errors.size());
        verify(productService, times(1)).saveOrUpdateProduct(any());
    }
}
