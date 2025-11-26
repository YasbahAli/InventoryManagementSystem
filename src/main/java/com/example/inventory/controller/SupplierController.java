package com.example.inventory.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.inventory.entity.Supplier;
import com.example.inventory.service.SupplierService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/list")
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "supplier_list";
    }

    @GetMapping("/add")
    public String addSupplier(Model model) {
        model.addAttribute("supplier", new Supplier());  // Empty supplier object for form
        return "supplier_form";  // Render the form template
    }

    @GetMapping("/edit/{id}")
    public String editSupplier(@PathVariable("id") Long id, Model model) {
        Optional<Supplier> supplier = supplierService.getSupplierById(id);
        if (supplier.isPresent()) {
            model.addAttribute("supplier", supplier.get());
        } else {
            model.addAttribute("message", "Supplier not found!");
        }
        return "supplier_form";
    }

    @PostMapping("/save")
    public String saveOrUpdateSupplier(@ModelAttribute("supplier") @Valid Supplier supplier, BindingResult result) {
        if (result.hasErrors()) {
            return "supplier_form";
        }
        supplierService.saveOrUpdateSupplier(supplier);
        return "redirect:/suppliers/list";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable("id") Long id) {
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers/list";
    }
}
