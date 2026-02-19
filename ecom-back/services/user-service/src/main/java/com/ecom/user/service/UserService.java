package com.ecom.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.user.dto.AddressRequest;
import com.ecom.user.dto.AddressResponse;
import com.ecom.user.dto.UpsertUserPreferencesRequest;
import com.ecom.user.dto.UpsertUserProfileRequest;
import com.ecom.user.dto.UserPreferencesResponse;
import com.ecom.user.dto.UserProfileResponse;
import com.ecom.user.entity.UserAddressRecord;
import com.ecom.user.entity.UserPreferencesRecord;
import com.ecom.user.entity.UserProfileRecord;
import com.ecom.user.repository.UserAddressRepository;
import com.ecom.user.repository.UserPreferencesRepository;
import com.ecom.user.repository.UserProfileRepository;

@Service
public class UserService implements UserUseCases {

    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    public UserService(
            UserProfileRepository userProfileRepository,
            UserAddressRepository userAddressRepository,
            UserPreferencesRepository userPreferencesRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userAddressRepository = userAddressRepository;
        this.userPreferencesRepository = userPreferencesRepository;
    }

    @Override
    @Transactional
    public UserProfileResponse upsertProfile(Long userId, UpsertUserProfileRequest request) {
        validateUserId(userId);
        UserProfileRecord profile = userProfileRepository.findByUserId(userId).orElseGet(() -> {
            UserProfileRecord created = new UserProfileRecord();
            created.setUserId(userId);
            return created;
        });
        profile.setEmail(request.email().trim().toLowerCase());
        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setPhoneNumber(request.phoneNumber().trim());
        return toProfileResponse(userProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        validateUserId(userId);
        UserProfileRecord profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));
        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        validateUserId(userId);
        ensureProfileExists(userId);

        UserAddressRecord address = new UserAddressRecord();
        applyAddress(address, userId, request);

        UserAddressRecord saved = userAddressRepository.save(address);
        if (saved.isDefaultAddress()) {
            setDefaultAddressInternal(userId, saved.getId());
            saved = userAddressRepository.findByIdAndUserId(saved.getId(), userId)
                    .orElse(saved);
        }
        return toAddressResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        validateUserId(userId);
        UserAddressRecord address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found for userId/addressId"));
        applyAddress(address, userId, request);
        UserAddressRecord saved = userAddressRepository.save(address);
        if (saved.isDefaultAddress()) {
            setDefaultAddressInternal(userId, saved.getId());
            saved = userAddressRepository.findByIdAndUserId(saved.getId(), userId)
                    .orElse(saved);
        }
        return toAddressResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        validateUserId(userId);
        return userAddressRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        validateUserId(userId);
        UserAddressRecord address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found for userId/addressId"));
        userAddressRepository.deleteByIdAndUserId(addressId, userId);
        if (address.isDefaultAddress()) {
            userProfileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.setDefaultAddressId(null);
                userProfileRepository.save(profile);
            });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        validateUserId(userId);
        setDefaultAddressInternal(userId, addressId);
        UserAddressRecord address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found after default update"));
        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public UserPreferencesResponse upsertPreferences(Long userId, UpsertUserPreferencesRequest request) {
        validateUserId(userId);
        ensureProfileExists(userId);

        UserPreferencesRecord preferences = userPreferencesRepository.findByUserId(userId).orElseGet(() -> {
            UserPreferencesRecord created = new UserPreferencesRecord();
            created.setUserId(userId);
            return created;
        });
        preferences.setMarketingEmailsEnabled(request.marketingEmailsEnabled());
        preferences.setSmsEnabled(request.smsEnabled());
        preferences.setPreferredLanguage(request.preferredLanguage().trim().toLowerCase());
        preferences.setPreferredCurrency(request.preferredCurrency().trim().toUpperCase());
        return toPreferencesResponse(userPreferencesRepository.save(preferences));
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences(Long userId) {
        validateUserId(userId);
        UserPreferencesRecord preferences = userPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Preferences not found for userId: " + userId));
        return toPreferencesResponse(preferences);
    }

    private void setDefaultAddressInternal(Long userId, Long addressId) {
        List<UserAddressRecord> addresses = userAddressRepository.findByUserIdOrderByIdAsc(userId);
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException("No addresses available for userId: " + userId);
        }

        boolean found = false;
        for (UserAddressRecord address : addresses) {
            boolean isDefault = address.getId().equals(addressId);
            if (isDefault) {
                found = true;
            }
            if (address.isDefaultAddress() != isDefault) {
                address.setDefaultAddress(isDefault);
                userAddressRepository.save(address);
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Address not found for userId/addressId");
        }

        UserProfileRecord profile = ensureProfileExists(userId);
        profile.setDefaultAddressId(addressId);
        userProfileRepository.save(profile);
    }

    private void applyAddress(UserAddressRecord target, Long userId, AddressRequest request) {
        target.setUserId(userId);
        target.setLabel(request.label().trim());
        target.setLine1(request.line1().trim());
        target.setLine2(normalizeNullable(request.line2()));
        target.setCity(request.city().trim());
        target.setState(request.state().trim());
        target.setPostalCode(request.postalCode().trim());
        target.setCountry(request.country().trim());
        target.setDefaultAddress(request.defaultAddress());
    }

    private UserProfileRecord ensureProfileExists(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile must exist for userId: " + userId));
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be a positive number");
        }
    }

    private UserProfileResponse toProfileResponse(UserProfileRecord profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getDefaultAddressId(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private AddressResponse toAddressResponse(UserAddressRecord address) {
        return new AddressResponse(
                address.getId(),
                address.getUserId(),
                address.getLabel(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt());
    }

    private UserPreferencesResponse toPreferencesResponse(UserPreferencesRecord preferences) {
        return new UserPreferencesResponse(
                preferences.getUserId(),
                preferences.isMarketingEmailsEnabled(),
                preferences.isSmsEnabled(),
                preferences.getPreferredLanguage(),
                preferences.getPreferredCurrency(),
                preferences.getCreatedAt(),
                preferences.getUpdatedAt());
    }
}
