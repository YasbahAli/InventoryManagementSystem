package com.example.inventory.service;

import com.example.inventory.entity.Category;
import com.example.inventory.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> getAllCategories() {
        return repository.findAll();
    }

    public Category getCategoryById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
    }

    public Category saveOrUpdateCategory(Category category) {
    return repository.save(category);
    }

    public void deleteCategoryById(Long id) {
        repository.deleteById(id);
    }
}
