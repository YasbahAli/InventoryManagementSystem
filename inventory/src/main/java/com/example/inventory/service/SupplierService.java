package com.example.inventory.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.inventory.entity.Supplier;
import com.example.inventory.repository.SupplierRepository;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    public void saveOrUpdateSupplier(Supplier supplier) {
        if (supplier.getId() != null) {
            // If ID exists, update the existing record
            Supplier existingSupplier = supplierRepository.findById(supplier.getId()).orElse(null);
            if (existingSupplier != null) {
                existingSupplier.setName(supplier.getName());
                existingSupplier.setAddress(supplier.getAddress());
                existingSupplier.setContactNumber(supplier.getContactNumber());
                supplierRepository.save(existingSupplier);
            }
        } else {
            // If ID doesn't exist, create a new record
            supplierRepository.save(supplier);
        }
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}
