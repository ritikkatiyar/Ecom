package com.ecom.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.inventory.entity.InventoryReservation;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    List<InventoryReservation> findByReservationIdStartingWith(String prefix);
}
