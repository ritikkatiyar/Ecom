package com.ecom.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.user.entity.UserPreferencesRecord;

public interface UserPreferencesRepository extends JpaRepository<UserPreferencesRecord, Long> {

    Optional<UserPreferencesRecord> findByUserId(Long userId);
}
