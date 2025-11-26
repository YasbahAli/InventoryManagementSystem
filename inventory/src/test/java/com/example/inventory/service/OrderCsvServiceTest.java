package com.example.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.Supplier;

class OrderCsvServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private OrderCsvService orderCsvService;

    private List<Order> testOrders;
    private Product testProduct;
    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setQuantity(100);
        testProduct.setPrice(50.0);

        testSupplier = new Supplier();
        testSupplier.setId(1L);
        testSupplier.setName("Test Supplier");

        testOrders = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setProduct(testProduct);
            order.setQuantity(10 * i);
            order.setStatus(OrderStatus.PENDING);
            order.setTotalPrice(500.0 * i);
            order.setSupplier(testSupplier);
            testOrders.add(order);
        }
    }

    @Test
    void testExportOrdersToCsv() throws IOException {
        when(orderService.getAllOrders()).thenReturn(testOrders);

        ByteArrayOutputStream result = orderCsvService.exportOrdersToCsv();

        assertNotNull(result);
        assertTrue(result.size() > 0);
        String csvContent = result.toString();
        assertTrue(csvContent.contains("ID"));
        assertTrue(csvContent.contains("Product"));
        assertTrue(csvContent.contains("Test Product"));
        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void testExportEmptyOrderList() throws IOException {
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>());

        ByteArrayOutputStream result = orderCsvService.exportOrdersToCsv();

        assertNotNull(result);
        String csvContent = result.toString();
        assertTrue(csvContent.contains("ID"));
        assertTrue(csvContent.contains("Product"));
    }

    @Test
    void testImportOrdersWithValidCsv() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,PENDING,Test Supplier\n" +
                "Test Product,100,CONFIRMED,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(supplierService.getAllSuppliers()).thenReturn(List.of(testSupplier));
        when(orderService.saveOrUpdateOrder(any())).thenReturn(new Order());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
        verify(orderService, times(2)).saveOrUpdateOrder(any());
    }

    @Test
    void testImportOrdersWithMissingProduct() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                ",50,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Product name is required"));
    }

    @Test
    void testImportOrdersWithInvalidProduct() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Non-existent Product,50,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("not found"));
    }

    @Test
    void testImportOrdersWithMissingQuantity() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Quantity is required"));
    }

    @Test
    void testImportOrdersWithInvalidQuantity() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,invalid,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Invalid quantity") || errors.get(0).contains("format"));
    }

    @Test
    void testImportOrdersWithZeroQuantity() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,0,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("greater than 0"));
    }

    @Test
    void testImportOrdersWithValidStatus() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,COMPLETED,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(supplierService.getAllSuppliers()).thenReturn(List.of(testSupplier));
        when(orderService.saveOrUpdateOrder(any())).thenReturn(new Order());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
    }

    @Test
    void testImportOrdersWithInvalidStatus() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,INVALID_STATUS,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertTrue(errors.size() > 0);
        assertTrue(errors.get(0).contains("Invalid status"));
    }

    @Test
    void testImportOrdersWithDefaultStatus() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(supplierService.getAllSuppliers()).thenReturn(List.of(testSupplier));
        when(orderService.saveOrUpdateOrder(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertNotNull(order.getStatus());
            assertEquals(OrderStatus.PENDING, order.getStatus());
            return order;
        });

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
    }

    @Test
    void testImportOrdersWithoutSupplier() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,PENDING,\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(orderService.saveOrUpdateOrder(any())).thenReturn(new Order());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
        verify(orderService, times(1)).saveOrUpdateOrder(any());
    }

    @Test
    void testImportOrdersWithValidSupplierLookup() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n" +
                "Test Product,50,PENDING,Test Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(supplierService.getAllSuppliers()).thenReturn(List.of(testSupplier));
        when(orderService.saveOrUpdateOrder(any())).thenReturn(new Order());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
        verify(supplierService, times(1)).getAllSuppliers();
    }

    @Test
    void testImportEmptyFile() throws IOException {
        String csvContent = "Product,Quantity,Status,Supplier\n";

        MultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", 
                csvContent.getBytes());

        List<String> errors = orderCsvService.importOrdersFromCsv(file);

        assertEquals(0, errors.size());
    }
}
