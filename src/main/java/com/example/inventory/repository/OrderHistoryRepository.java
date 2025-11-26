package com.example.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.inventory.entity.OrderHistory;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
}
