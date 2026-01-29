# Flags API

The Flags API allows you to manage feature flag definitions for the feature flag system. Flags represent toggleable features with typed default values that can later be overridden per environment.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Data Model](#data-model)
  - [Flag Object](#flag-object)
  - [Flag Types](#flag-types)
  - [Field Constraints](#field-constraints)
- [Endpoints](#endpoints)
  - [List Flags](#list-flags)
  - [Get Flag](#get-flag)
  - [Create Flag](#create-flag)
  - [Update Flag](#update-flag)
  - [Delete Flag](#delete-flag)
- [Error Responses](#error-responses)
- [Business Rules](#business-rules)
- [Examples](#examples)

---

## Overview

Flags are the core entities in the feature flag system. Each flag defines:

- A unique programmatic `key` used by SDKs to retrieve flag values
- A human-readable `name` for display in management interfaces
- A `type` that determines how the flag's value is interpreted (STRING, BOOLEAN, or NUMBER)
- A `defaultValue` that is returned when no environment-specific override exists

**Base URL:** `/flags`

---

## Authentication

All endpoints require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

Requests without a valid token will receive a `401 Unauthorized` response.

---

## Authorization

All Flags API endpoints require the **ADMIN** role. Users without this role will receive a `403 Forbidden` response.

---

## Data Model

### Flag Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier (read-only, auto-generated) |
| `key` | `string` | Programmatic key for SDK usage (immutable after creation) |
| `name` | `string` | Human-readable display name |
| `description` | `string` | Optional description of the flag's purpose |
| `type` | `FlagType` | The data type of the flag's value (immutable after creation) |
| `defaultValue` | `string` | The default value when no override exists (validated against type) |
| `isActive` | `boolean` | Whether the flag is active (read-only) |
| `createdAt` | `ISO 8601` | Timestamp of creation (read-only) |
| `updatedAt` | `ISO 8601` | Timestamp of last update (read-only) |

### Flag Types

The `type` field determines how the flag's `defaultValue` (and future overrides) are validated and interpreted:

| Type | Description | Valid Values |
|------|-------------|--------------|
| `STRING` | Free-form text value | Any string |
| `BOOLEAN` | True/false toggle | `"true"` or `"false"` (case-insensitive) |
| `NUMBER` | Numeric value | Any valid number (integer or decimal, e.g., `"42"`, `"3.14"`, `"-100"`) |

### Field Constraints

| Field | Constraints |
|-------|-------------|
| `key` | Required, 1-100 characters, lowercase letters, numbers, and hyphens only (`^[a-z0-9-]+$`) |
| `name` | Required, 1-200 characters |
| `description` | Optional, max 1000 characters |
| `type` | Required, must be one of: `STRING`, `BOOLEAN`, `NUMBER` |
| `defaultValue` | Required, max 500 characters, must be valid for the specified `type` |

---

## Endpoints

### List Flags

Retrieve all active flags, optionally filtered by search query.

**Request**

```
GET /flags
GET /flags?search={query}
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `search` | `string` | No | Filter flags by key, name, or description (case-insensitive partial match) |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "key": "dark-mode-enabled",
    "name": "Dark Mode",
    "description": "Enable dark mode theme for the application",
    "type": "BOOLEAN",
    "defaultValue": "false",
    "isActive": true,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-01-15T10:30:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "key": "max-upload-size-mb",
    "name": "Maximum Upload Size",
    "description": "Maximum file upload size in megabytes",
    "type": "NUMBER",
    "defaultValue": "10",
    "isActive": true,
    "createdAt": "2026-01-15T10:31:00Z",
    "updatedAt": "2026-01-15T10:31:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "key": "welcome-message",
    "name": "Welcome Message",
    "description": "Custom welcome message displayed to users",
    "type": "STRING",
    "defaultValue": "Welcome to our platform!",
    "isActive": true,
    "createdAt": "2026-01-15T10:32:00Z",
    "updatedAt": "2026-01-15T10:32:00Z"
  }
]
```

**Example: Search Filter**

```
GET /flags?search=upload
```

Returns flags where `key`, `name`, or `description` contains "upload" (case-insensitive).

---

### Get Flag

Retrieve a single flag by ID.

**Request**

```
GET /flags/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The flag's unique identifier |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "dark-mode-enabled",
  "name": "Dark Mode",
  "description": "Enable dark mode theme for the application",
  "type": "BOOLEAN",
  "defaultValue": "false",
  "isActive": true,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has been deleted |

---

### Create Flag

Create a new feature flag definition.

**Request**

```
POST /flags
Content-Type: application/json
```

```json
{
  "key": "new-checkout-flow",
  "name": "New Checkout Flow",
  "description": "Enable the redesigned checkout experience",
  "type": "BOOLEAN",
  "defaultValue": "false"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `key` | `string` | Yes | Unique programmatic key (lowercase, alphanumeric, hyphens) |
| `name` | `string` | Yes | Display name |
| `description` | `string` | No | Optional description |
| `type` | `FlagType` | Yes | One of: `STRING`, `BOOLEAN`, `NUMBER` |
| `defaultValue` | `string` | Yes | Default value (must be valid for the specified type) |

**Response**

```
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "key": "new-checkout-flow",
  "name": "New Checkout Flow",
  "description": "Enable the redesigned checkout experience",
  "type": "BOOLEAN",
  "defaultValue": "false",
  "isActive": true,
  "createdAt": "2026-01-28T14:00:00Z",
  "updatedAt": "2026-01-28T14:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed (see [Validation Errors](#validation-errors)) |
| `400 Bad Request` | Flag with the same `key` already exists (among active flags) |
| `400 Bad Request` | `defaultValue` is not valid for the specified `type` |

**Example: Duplicate Key Error**

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Flag with key 'dark-mode-enabled' already exists"
}
```

**Example: Invalid Default Value for Type**

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Default value for BOOLEAN type must be 'true' or 'false', got: 'yes'"
}
```

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Default value for NUMBER type must be a valid number, got: 'abc'"
}
```

---

### Update Flag

Update an existing flag. Only `name`, `description`, and `defaultValue` can be modified.

**Request**

```
PATCH /flags/{id}
Content-Type: application/json
```

```json
{
  "name": "Dark Mode Theme",
  "description": "Toggle dark mode appearance across the application",
  "defaultValue": "true"
}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The flag's unique identifier |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | No | New display name (if provided) |
| `description` | `string` | No | New description (if provided) |
| `defaultValue` | `string` | No | New default value (if provided, must be valid for the flag's type) |

> **Note:** The `key` and `type` fields are **immutable** and will be ignored if included in the request body.

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "dark-mode-enabled",
  "name": "Dark Mode Theme",
  "description": "Toggle dark mode appearance across the application",
  "type": "BOOLEAN",
  "defaultValue": "true",
  "isActive": true,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-28T15:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has been deleted |
| `400 Bad Request` | `defaultValue` is not valid for the flag's type |

---

### Delete Flag

Delete a flag (soft delete).

**Request**

```
DELETE /flags/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The flag's unique identifier |

**Response**

```
HTTP/1.1 204 No Content
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has already been deleted |

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

### Validation Errors

Validation errors include field-specific details:

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "key": "Key must contain only lowercase letters, numbers, and hyphens",
    "name": "Name is required",
    "type": "Type is required",
    "defaultValue": "Default value is required"
  }
}
```

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| `400 Bad Request` | Validation error or business rule violation |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User lacks required ADMIN role |
| `404 Not Found` | Flag not found or has been deleted |
| `405 Method Not Allowed` | HTTP method not supported for endpoint |
| `500 Internal Server Error` | Unexpected server error |

---

## Business Rules

### Soft Delete

Flags are **soft deleted** rather than permanently removed from the database:

- When deleted, `isActive` is set to `false`
- Soft-deleted flags are excluded from all API responses
- This preserves referential integrity with environment overrides and evaluation history
- There is no restore functionality; once deleted, a flag cannot be recovered via the API

### Key Immutability

The `key` field **cannot be changed** after creation:

- This ensures SDK integrations remain stable
- Update requests that include `key` will ignore the field
- To change a key, create a new flag and migrate configurations

### Type Immutability

The `type` field **cannot be changed** after creation:

- Changing type would invalidate existing environment overrides
- Changing type would break SDK consumers expecting a specific type
- Update requests that include `type` will ignore the field
- To change type, create a new flag and deprecate the old one

### Key Uniqueness

The `key` must be unique **among active flags only**:

- Creating a flag with a key that exists on an active flag will fail
- If a flag was soft-deleted, its key can be reused for a new flag
- This allows recovery scenarios where a deleted flag's key needs to be recreated

### Default Value Validation

The `defaultValue` is validated against the flag's `type` at creation and update:

| Type | Validation Rule |
|------|-----------------|
| `STRING` | Any non-empty string is accepted |
| `BOOLEAN` | Must be exactly `"true"` or `"false"` (case-insensitive) |
| `NUMBER` | Must be a valid numeric value (parsed as `Double`) |

**Examples of valid values:**

| Type | Valid Values |
|------|--------------|
| `STRING` | `"hello"`, `"123"`, `"true"`, `"any text"` |
| `BOOLEAN` | `"true"`, `"false"`, `"TRUE"`, `"False"` |
| `NUMBER` | `"0"`, `"42"`, `"-17"`, `"3.14159"`, `"-0.5"`, `"1e10"` |

**Examples of invalid values:**

| Type | Invalid Value | Error Message |
|------|---------------|---------------|
| `BOOLEAN` | `"yes"` | Default value for BOOLEAN type must be 'true' or 'false', got: 'yes' |
| `BOOLEAN` | `"1"` | Default value for BOOLEAN type must be 'true' or 'false', got: '1' |
| `NUMBER` | `"abc"` | Default value for NUMBER type must be a valid number, got: 'abc' |
| `NUMBER` | `"12.34.56"` | Default value for NUMBER type must be a valid number, got: '12.34.56' |

### Audit Trail

All flags automatically track:

- `createdAt`: Timestamp when the flag was created
- `updatedAt`: Timestamp of the last modification
- `createdBy`: User who created the flag (stored internally)
- `updatedBy`: User who last modified the flag (stored internally)

---

## Examples

### cURL Examples

**List all flags:**
```bash
curl -X GET "http://localhost:8080/flags" \
  -H "Authorization: Bearer <token>"
```

**Search flags:**
```bash
curl -X GET "http://localhost:8080/flags?search=checkout" \
  -H "Authorization: Bearer <token>"
```

**Create a boolean flag:**
```bash
curl -X POST "http://localhost:8080/flags" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "enable-new-dashboard",
    "name": "Enable New Dashboard",
    "description": "Show the redesigned dashboard to users",
    "type": "BOOLEAN",
    "defaultValue": "false"
  }'
```

**Create a number flag:**
```bash
curl -X POST "http://localhost:8080/flags" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "rate-limit-per-minute",
    "name": "API Rate Limit",
    "description": "Maximum API requests allowed per minute",
    "type": "NUMBER",
    "defaultValue": "100"
  }'
```

**Create a string flag:**
```bash
curl -X POST "http://localhost:8080/flags" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "banner-message",
    "name": "Banner Message",
    "description": "Announcement banner text shown at the top of the page",
    "type": "STRING",
    "defaultValue": "Welcome to our platform!"
  }'
```

**Update a flag:**
```bash
curl -X PATCH "http://localhost:8080/flags/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enable New Dashboard (Beta)",
    "description": "Show the redesigned dashboard to beta users",
    "defaultValue": "true"
  }'
```

**Delete a flag:**
```bash
curl -X DELETE "http://localhost:8080/flags/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>"
```
