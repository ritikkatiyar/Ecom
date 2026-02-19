package com.ecom.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.user.entity.UserAddressRecord;

public interface UserAddressRepository extends JpaRepository<UserAddressRecord, Long> {

    List<UserAddressRecord> findByUserIdOrderByIdAsc(Long userId);

    Optional<UserAddressRecord> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
