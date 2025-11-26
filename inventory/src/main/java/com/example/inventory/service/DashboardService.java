package com.example.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.SupplierRepository;

@Service
public class DashboardService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    // Get total count of products
    public long getProductCount() {
        return productRepository.count();
    }

    // Get total count of suppliers
    public long getSupplierCount() {
        return supplierRepository.count();
    }
}
