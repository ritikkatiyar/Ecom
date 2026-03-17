package com.ecom.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.user.dto.AddressRequest;
import com.ecom.user.dto.AddressResponse;
import com.ecom.user.dto.UpsertUserPreferencesRequest;
import com.ecom.user.dto.UpsertUserProfileRequest;
import com.ecom.user.dto.UserPreferencesResponse;
import com.ecom.user.dto.UserProfileResponse;
import com.ecom.user.service.UserUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserUseCases userUseCases;

    public UserController(UserUseCases userUseCases) {
        this.userUseCases = userUseCases;
    }

    /**
     * Creates or updates the profile details for a user.
     */
    @PutMapping("/{userId}/profile")
    public UserProfileResponse upsertProfile(@PathVariable Long userId, @Valid @RequestBody UpsertUserProfileRequest request) {
        return userUseCases.upsertProfile(userId, request);
    }

    /**
     * Returns the profile details for a user.
     */
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return userUseCases.getProfile(userId);
    }

    /**
     * Creates or updates persisted user preferences.
     */
    @PutMapping("/{userId}/preferences")
    public UserPreferencesResponse upsertPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UpsertUserPreferencesRequest request) {
        return userUseCases.upsertPreferences(userId, request);
    }

    /**
     * Returns the stored preferences for a user.
     */
    @GetMapping("/{userId}/preferences")
    public UserPreferencesResponse getPreferences(@PathVariable Long userId) {
        return userUseCases.getPreferences(userId);
    }

    /**
     * Adds a new address to the user's saved address book.
     */
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userUseCases.addAddress(userId, request));
    }

    /**
     * Lists all saved addresses for a user.
     */
    @GetMapping("/{userId}/addresses")
    public List<AddressResponse> listAddresses(@PathVariable Long userId) {
        return userUseCases.listAddresses(userId);
    }

    /**
     * Updates one saved address for a user.
     */
    @PutMapping("/{userId}/addresses/{addressId}")
    public AddressResponse updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return userUseCases.updateAddress(userId, addressId, request);
    }

    /**
     * Marks one saved address as the default shipping or billing address.
     */
    @PostMapping("/{userId}/addresses/{addressId}/default")
    public AddressResponse setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return userUseCases.setDefaultAddress(userId, addressId);
    }

    /**
     * Deletes one saved address from the user's address book.
     */
    @DeleteMapping("/{userId}/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        userUseCases.deleteAddress(userId, addressId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
