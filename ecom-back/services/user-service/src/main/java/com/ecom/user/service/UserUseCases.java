package com.ecom.user.service;

import java.util.List;

import com.ecom.user.dto.AddressRequest;
import com.ecom.user.dto.AddressResponse;
import com.ecom.user.dto.UpsertUserPreferencesRequest;
import com.ecom.user.dto.UpsertUserProfileRequest;
import com.ecom.user.dto.UserPreferencesResponse;
import com.ecom.user.dto.UserProfileResponse;

public interface UserUseCases {

    UserProfileResponse upsertProfile(Long userId, UpsertUserProfileRequest request);

    UserProfileResponse getProfile(Long userId);

    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    List<AddressResponse> listAddresses(Long userId);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);

    UserPreferencesResponse upsertPreferences(Long userId, UpsertUserPreferencesRequest request);

    UserPreferencesResponse getPreferences(Long userId);
}
