# Users API

The Users API provides endpoints for managing user accounts in the feature flag system. It allows administrators to view, update, and delete users.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Data Model](#data-model)
- [Endpoints](#endpoints)
  - [List Users](#list-users)
  - [Get User](#get-user)
  - [Update User](#update-user)
  - [Delete User](#delete-user)
- [Error Responses](#error-responses)
- [Business Rules](#business-rules)

---

## Overview

The Users API enables administrators to manage user accounts, including viewing user details, updating roles and personal information, and removing users from the system.

**Base URL:** `/users`

> **Note:** User creation is handled through the [Authentication API](authentication.md) (`POST /auth/register`).

---

## Authentication

All endpoints require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

Requests without a valid token will receive a `401 Unauthorized` response.

---

## Authorization

All Users API endpoints require the **ADMIN** role. Users without this role will receive a `403 Forbidden` response.

---

## Data Model

### User Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier (read-only, auto-generated) |
| `email` | `string` | User's email address (read-only after creation) |
| `firstName` | `string` | User's first name |
| `lastName` | `string` | User's last name |
| `role` | `string` | User's role: `ADMIN` or `GUEST` |
| `enabled` | `boolean` | Whether the user account is active |
| `createdAt` | `ISO 8601` | Timestamp of creation (read-only) |
| `updatedAt` | `ISO 8601` | Timestamp of last update (read-only) |

### Field Constraints (for updates)

| Field | Constraints |
|-------|-------------|
| `firstName` | 1-100 characters |
| `lastName` | 1-100 characters |
| `role` | Must be `ADMIN` or `GUEST` |
| `enabled` | Boolean value |

### Roles

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full access to all API endpoints |
| `GUEST` | Limited access (read-only for most resources) |

---

## Endpoints

### List Users

Retrieve all users in the system.

**Request**

```
GET /users
```

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2026-01-15T10:00:00Z",
    "updatedAt": "2026-01-15T10:00:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "email": "guest@example.com",
    "firstName": "Guest",
    "lastName": "User",
    "role": "GUEST",
    "enabled": true,
    "createdAt": "2026-01-16T11:00:00Z",
    "updatedAt": "2026-01-16T11:00:00Z"
  }
]
```

---

### Get User

Retrieve a single user by ID.

**Request**

```
GET /users/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The user's unique identifier |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@example.com",
  "firstName": "Admin",
  "lastName": "User",
  "role": "ADMIN",
  "enabled": true,
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | User not found |

---

### Update User

Update an existing user's information.

**Request**

```
PATCH /users/{id}
Content-Type: application/json
```

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "role": "ADMIN",
  "enabled": true
}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The user's unique identifier |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `firstName` | `string` | No | New first name |
| `lastName` | `string` | No | New last name |
| `role` | `string` | No | New role: `ADMIN` or `GUEST` |
| `enabled` | `boolean` | No | Enable or disable the account |

> **Note:** The `email` field is **immutable** and will be ignored if included in the request body.

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "email": "guest@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "role": "ADMIN",
  "enabled": true,
  "createdAt": "2026-01-16T11:00:00Z",
  "updatedAt": "2026-01-28T15:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | User not found |
| `400 Bad Request` | Invalid role value |
| `409 Conflict` | Cannot change enabled status of your own account |

---

### Delete User

Permanently delete a user from the system.

**Request**

```
DELETE /users/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The user's unique identifier |

**Response**

```
HTTP/1.1 204 No Content
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | User not found |
| `409 Conflict` | Cannot delete your own account |

> ⚠️ **Warning:** This is a permanent deletion. Consider disabling the user (`enabled: false`) instead if you want to preserve the account.

---

## Error Responses

### Standard Error Format

All error responses follow this format:

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message"
}
```

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User lacks required ADMIN role |
| `404 Not Found` | User not found |
| `405 Method Not Allowed` | HTTP method not supported for endpoint |
| `409 Conflict` | Self-operation not allowed (e.g., deleting or disabling own account) |
| `500 Internal Server Error` | Unexpected server error |

---

## Business Rules

### Email Immutability

The `email` field cannot be changed after registration:

- Email serves as the unique identifier for authentication
- To change email, create a new account and delete the old one

### Role Management

- The **first registered user** automatically receives `ADMIN` role
- Only `ADMIN` users can modify roles
- Available roles: `ADMIN`, `GUEST`
- Be cautious when demoting the last admin (could lock out administrative access)

### Account Status

- Users can be **disabled** by setting `enabled: false`
- Disabled users:
  - Cannot authenticate (login returns `401 Unauthorized`)
  - Remain in the system with their data intact
  - Can be re-enabled by an admin
- **Deleting** a user permanently removes them from the system

### Self-Operation Prevention

Users **cannot** perform the following actions on their own account:

- **Delete** their own account (`DELETE /users/{own-id}` → `409 Conflict`)
- **Change their own enabled status** (`PATCH /users/{own-id}` with `enabled: true` or `false` → `409 Conflict`)

This prevents accidental admin lockout and ensures at least one active administrator can always manage the system. Other self-updates (e.g., changing name or role) are still permitted.

### Hard Delete

Unlike environments, users are **permanently deleted** (hard delete):

- Once deleted, the user data cannot be recovered
- The email becomes available for new registrations
- Consider disabling (`enabled: false`) instead of deleting for audit purposes

### Audit Trail

All users automatically track:

- `createdAt`: Timestamp when the user was registered
- `updatedAt`: Timestamp of the last modification
- `createdBy`: User who created the record (stored internally)
- `updatedBy`: User who last modified the record (stored internally)

---

## Examples

### cURL Examples

**List all users:**
```bash
curl -X GET "http://localhost:8080/users" \
  -H "Authorization: Bearer <token>"
```

**Get a specific user:**
```bash
curl -X GET "http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>"
```

**Update user role to ADMIN:**
```bash
curl -X PATCH "http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ADMIN"
  }'
```

**Disable a user:**
```bash
curl -X PATCH "http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'
```

**Delete a user:**
```bash
curl -X DELETE "http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer <token>"
```

---

## Related Documentation

- [Authentication API](authentication.md) - Register and login endpoints
- [Environments API](environments.md) - Environment management endpoints
