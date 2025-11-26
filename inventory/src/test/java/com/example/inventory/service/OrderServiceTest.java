package com.example.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;
import com.example.inventory.repository.OrderHistoryRepository;
import com.example.inventory.repository.OrderRepository;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderHistoryRepository orderHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setQuantity(100);
        testProduct.setPrice(50.0);

        // Create test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(10);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalPrice(500.0);

        // Mock ProductService
        when(productService.getProductById(1L)).thenReturn(testProduct);
    }

    @Test
    void testSaveOrder() {
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        Order savedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertNotNull(savedOrder);
        assertEquals(testOrder.getId(), savedOrder.getId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testGetAllOrders() {
        List<Order> orderList = new ArrayList<>();
        orderList.add(testOrder);

        when(orderRepository.findAll()).thenReturn(orderList);

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getId(), result.get(0).getId());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        Order result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderByIdNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(999L));
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void testDeleteOrder() {
        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void testOrderStatusTransitionFromPendingToConfirmed() {
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Transition to CONFIRMED
        testOrder.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testOrderStatusTransitionFromConfirmedToShipped() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Transition to SHIPPED
        testOrder.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertEquals(OrderStatus.SHIPPED, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testOrderStatusTransitionFromShippedToCompleted() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Transition to COMPLETED
        testOrder.setStatus(OrderStatus.COMPLETED);
        Order updatedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertEquals(OrderStatus.COMPLETED, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testOrderCanBeCancelled() {
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Cancel order
        testOrder.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testOrderTotalPriceCalculation() {
        testOrder.setQuantity(5);
        testOrder.getProduct().setPrice(25.0);
        testOrder.setTotalPrice(testOrder.getQuantity() * testOrder.getProduct().getPrice());

        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        Order savedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertEquals(125.0, savedOrder.getTotalPrice());
    }

    @Test
    void testOrderWithNullProduct() {
        testOrder.setProduct(null);

        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        Order savedOrder = orderService.saveOrUpdateOrder(testOrder);

        assertNull(savedOrder.getProduct());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testMultipleOrdersForSameProduct() {
        Order order2 = new Order();
        order2.setId(2L);
        order2.setProduct(testProduct);
        order2.setQuantity(20);
        order2.setStatus(OrderStatus.PENDING);

        List<Order> orderList = new ArrayList<>();
        orderList.add(testOrder);
        orderList.add(order2);

        when(orderRepository.findAll()).thenReturn(orderList);

        List<Order> result = orderService.getAllOrders();

        assertEquals(2, result.size());
        assertEquals(testProduct.getId(), result.get(0).getProduct().getId());
        assertEquals(testProduct.getId(), result.get(1).getProduct().getId());
    }
}
