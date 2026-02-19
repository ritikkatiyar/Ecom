package com.ecom.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.user.entity.UserProfileRecord;

public interface UserProfileRepository extends JpaRepository<UserProfileRecord, Long> {

    Optional<UserProfileRecord> findByUserId(Long userId);
}
