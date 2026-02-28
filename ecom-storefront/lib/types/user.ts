export interface UserProfileResponse {
  userId: number;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  defaultAddressId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResponse {
  id: number;
  userId: number;
  label?: string;
  line1: string;
  line2?: string;
  city: string;
  state?: string;
  postalCode?: string;
  country?: string;
  defaultAddress: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserPreferencesResponse {
  userId: number;
  marketingEmailsEnabled: boolean;
  smsEnabled: boolean;
  preferredLanguage: string;
  preferredCurrency: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertUserPreferencesRequest {
  marketingEmailsEnabled: boolean;
  smsEnabled: boolean;
  preferredLanguage: string;
  preferredCurrency: string;
}
