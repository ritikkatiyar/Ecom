# API Docs - User Service

Base path: `/api/users`

## Endpoints
- `PUT /{userId}/profile` - create/update user profile.
- `GET /{userId}/profile` - get user profile.
- `PUT /{userId}/preferences` - create/update user preferences.
- `GET /{userId}/preferences` - get user preferences.
- `POST /{userId}/addresses` - add address.
- `GET /{userId}/addresses` - list addresses.
- `PUT /{userId}/addresses/{addressId}` - update address.
- `POST /{userId}/addresses/{addressId}/default` - set default address.
- `DELETE /{userId}/addresses/{addressId}` - delete address.

## Entities
- `UserProfileRecord` (MySQL)
- `UserAddressRecord` (MySQL)
- `UserPreferencesRecord` (MySQL)

## Data Stores
- MySQL: profile/address/preferences persistence.
- Redis: not engaged.
- Kafka: not engaged.

## Flow
1. Controller depends on `UserUseCases` interface (DIP-aligned boundary).
2. `UserService` enforces user ownership per address/preference operation.
3. Address default selection is normalized: setting one default address clears others for the same user.
4. Profile stores `defaultAddressId` for fast read on user identity payloads.
