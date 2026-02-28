import { apiClient } from "../apiClient";
import type {
  AddressResponse,
  UpsertUserPreferencesRequest,
  UserPreferencesResponse,
  UserProfileResponse,
} from "../types/user";

export async function getUserProfile(userId: number): Promise<UserProfileResponse> {
  return apiClient<UserProfileResponse>(`/api/users/${userId}/profile`);
}

export async function listUserAddresses(userId: number): Promise<AddressResponse[]> {
  return apiClient<AddressResponse[]>(`/api/users/${userId}/addresses`);
}

export async function getUserPreferences(userId: number): Promise<UserPreferencesResponse> {
  return apiClient<UserPreferencesResponse>(`/api/users/${userId}/preferences`);
}

export async function updateUserPreferences(
  userId: number,
  request: UpsertUserPreferencesRequest
): Promise<UserPreferencesResponse> {
  return apiClient<UserPreferencesResponse>(`/api/users/${userId}/preferences`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}
