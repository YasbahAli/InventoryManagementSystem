package com.example.inventory.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.inventory.entity.Order;
import com.example.inventory.service.OrderService;
import com.example.inventory.service.ProductService;
import com.example.inventory.service.SupplierService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final SupplierService supplierService;

    public OrderController(OrderService orderService, ProductService productService, SupplierService supplierService) {
        this.orderService = orderService;
        this.productService = productService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listOrders(Model model,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "10") int size,
                             @RequestParam(name = "sortField", defaultValue = "orderDate") String sortField,
                             @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<Order> ordersPage = orderService.getOrders(pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", "asc".equalsIgnoreCase(sortDir) ? "desc" : "asc");

        return "orders";
    }

    @GetMapping("/add")
    public String addOrderForm(Model model) {
        model.addAttribute("order", new Order());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "order_form";
    }

    @GetMapping("/edit/{id}")
    public String editOrderForm(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "order_form";
    }

    @PostMapping("/save")
    public String saveOrUpdateOrder(@Valid @ModelAttribute Order order, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            return "order_form";
        }
        // Sanitize supplier: if supplier provided but id is null or <=0, treat as no supplier
        if (order.getSupplier() != null) {
            Long supId = null;
            try {
                supId = order.getSupplier().getId();
            } catch (Exception ignored) {}
            if (supId == null || supId <= 0L) {
                order.setSupplier(null);
            }
        }

        // Validate product selection
        if (order.getProduct() == null || order.getProduct().getId() == null) {
            model.addAttribute("errorMessage", "Please select a product for the order.");
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            return "order_form";
        }

        try {
            orderService.saveOrUpdateOrder(order);
            return "redirect:/orders";
        } catch (IllegalStateException ex) {
            // validation failed (e.g., insufficient inventory)
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            return "order_form";
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // A foreign key constraint (supplier/product mismatch) — error message
            model.addAttribute("errorMessage", "Database constraint error: please ensure selected supplier and product exist.");
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            return "order_form";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unexpected error: " + ex.getMessage());
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            return "order_form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return "redirect:/orders";
    }
}
