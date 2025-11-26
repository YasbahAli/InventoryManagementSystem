package com.example.inventory.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderHistory;
import com.example.inventory.entity.OrderStatus;
import com.example.inventory.entity.Product;
import com.example.inventory.repository.OrderHistoryRepository;
import com.example.inventory.repository.OrderRepository;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderHistoryRepository historyRepository;
    private final ProductService productService;

    public OrderService(OrderRepository repository, OrderHistoryRepository historyRepository, ProductService productService) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.productService = productService;
    }

    public List<Order> getAllOrders() {
        return repository.findAll();
    }

    public Page<Order> getOrders(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Order getOrderById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
    }

    public Order saveOrUpdateOrder(Order order) {
        // Compute total price from product price and quantity when possible
        if (order.getProduct() != null && order.getQuantity() != null) {
            Product p = productService.getProductById(order.getProduct().getId());
            if (p != null && p.getPrice() != null) {
                order.setTotalPrice(p.getPrice() * order.getQuantity());
            }
        }

        // Determine previous status (if existing)
        OrderStatus previousStatus = null;
        if (order.getId() != null) {
            Order existing = repository.findById(order.getId()).orElse(null);
            if (existing != null) previousStatus = existing.getStatus();
        }

        OrderStatus newStatus = order.getStatus() == null ? OrderStatus.PENDING : order.getStatus();

        // Inventory adjustments:
        // - When transitioning to CONFIRMED (and wasn't CONFIRMED before) -> decrement inventory
        // - When transitioning from CONFIRMED to CANCELLED -> increment inventory back
        if (newStatus == OrderStatus.CONFIRMED && previousStatus != OrderStatus.CONFIRMED) {
            Product p = productService.getProductById(order.getProduct().getId());
            if (p == null) throw new IllegalStateException("Product not found for this order");
            Integer availableI = p.getQuantity();
            int available = availableI != null ? availableI : 0;
            Integer qtyI = order.getQuantity();
            if (qtyI == null || qtyI <= 0) throw new IllegalStateException("Order quantity must be provided and greater than zero");
            int qty = qtyI;
            if (available < qty) throw new IllegalStateException("Insufficient inventory for product: " + p.getName());
            p.setQuantity(available - qty);
            productService.saveOrUpdateProduct(p);
        }

        if (previousStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.CANCELLED) {
            Product p = productService.getProductById(order.getProduct().getId());
            if (p == null) throw new IllegalStateException("Product not found for this order");
            Integer availableI = p.getQuantity();
            int available = availableI != null ? availableI : 0;
            Integer qtyI = order.getQuantity();
            int qty = qtyI != null ? qtyI : 0;
            p.setQuantity(available + qty);
            productService.saveOrUpdateProduct(p);
        }

        // Save order
        Order saved = repository.save(order);

        // Record history if status changed
        if (previousStatus != newStatus) {
            OrderHistory h = new OrderHistory();
            h.setOrder(saved);
            h.setPreviousStatus(previousStatus);
            h.setNewStatus(newStatus);
            h.setActor(null);
            h.setNote("Status changed");
            historyRepository.save(h);
        }

        return saved;
    }

    public void deleteOrder(Long id) {
        repository.deleteById(id);
    }
}
