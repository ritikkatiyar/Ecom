package com.ecom.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.inventory.entity.InventoryReservation;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
}
