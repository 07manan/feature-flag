# Environments API

The Environments API allows you to manage deployment environments for the feature flag system. Environments represent deployment targets (e.g., `development`, `staging`, `production`) where feature flags can have different configurations.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Data Model](#data-model)
- [Endpoints](#endpoints)
  - [List Environments](#list-environments)
  - [Get Environment](#get-environment)
  - [Create Environment](#create-environment)
  - [Update Environment](#update-environment)
  - [Delete Environment](#delete-environment)
- [Error Responses](#error-responses)
- [Business Rules](#business-rules)

---

## Overview

Environments are core entities in the feature flag system that define where feature flags are evaluated. Each environment has a unique programmatic `key` (used by SDKs) and a human-readable `name`.

**Base URL:** `/environments`

---

## Authentication

All endpoints require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

Requests without a valid token will receive a `401 Unauthorized` response.

---

## Authorization

All Environment API endpoints require the **ADMIN** role. Users without this role will receive a `403 Forbidden` response.

---

## Data Model

### Environment Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier (read-only, auto-generated) |
| `key` | `string` | Programmatic key for SDK usage (immutable after creation) |
| `name` | `string` | Human-readable display name |
| `description` | `string` | Optional description of the environment |
| `isActive` | `boolean` | Whether the environment is active (read-only) |
| `createdAt` | `ISO 8601` | Timestamp of creation (read-only) |
| `updatedAt` | `ISO 8601` | Timestamp of last update (read-only) |

### Field Constraints

| Field | Constraints |
|-------|-------------|
| `key` | Required, 1-50 characters, lowercase letters, numbers, and hyphens only (`^[a-z0-9-]+$`) |
| `name` | Required, 1-100 characters |
| `description` | Optional, max 500 characters |

---

## Endpoints

### List Environments

Retrieve all active environments, optionally filtered by search query.

**Request**

```
GET /environments
GET /environments?search={query}
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `search` | `string` | No | Filter environments by name or description (case-insensitive partial match) |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "key": "production",
    "name": "Production",
    "description": "Live production environment",
    "isActive": true,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-01-15T10:30:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "key": "staging",
    "name": "Staging",
    "description": "Pre-production testing environment",
    "isActive": true,
    "createdAt": "2026-01-15T10:31:00Z",
    "updatedAt": "2026-01-15T10:31:00Z"
  }
]
```

**Example: Search Filter**

```
GET /environments?search=prod
```

Returns environments where `name` or `description` contains "prod" (case-insensitive).

---

### Get Environment

Retrieve a single environment by ID.

**Request**

```
GET /environments/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The environment's unique identifier |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "production",
  "name": "Production",
  "description": "Live production environment",
  "isActive": true,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Environment not found or has been deleted |

---

### Create Environment

Create a new deployment environment.

**Request**

```
POST /environments
Content-Type: application/json
```

```json
{
  "key": "development",
  "name": "Development",
  "description": "Local development environment"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `key` | `string` | Yes | Unique programmatic key (lowercase, alphanumeric, hyphens) |
| `name` | `string` | Yes | Display name |
| `description` | `string` | No | Optional description |

**Response**

```
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "key": "development",
  "name": "Development",
  "description": "Local development environment",
  "isActive": true,
  "createdAt": "2026-01-28T14:00:00Z",
  "updatedAt": "2026-01-28T14:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed (see [Validation Errors](#validation-errors)) |
| `400 Bad Request` | Environment with the same `key` already exists (among active environments) |

**Example: Duplicate Key Error**

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Environment with key 'production' already exists"
}
```

---

### Update Environment

Update an existing environment. Only `name` and `description` can be modified.

**Request**

```
PATCH /environments/{id}
Content-Type: application/json
```

```json
{
  "name": "Production (US-East)",
  "description": "Primary production environment in US-East region"
}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The environment's unique identifier |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | No | New display name (if provided) |
| `description` | `string` | No | New description (if provided) |

> **Note:** The `key` field is **immutable** and will be ignored if included in the request body.

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "production",
  "name": "Production (US-East)",
  "description": "Primary production environment in US-East region",
  "isActive": true,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-28T15:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Environment not found or has been deleted |

---

### Delete Environment

Delete an environment (soft delete).

**Request**

```
DELETE /environments/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | The environment's unique identifier |

**Response**

```
HTTP/1.1 204 No Content
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Environment not found or has already been deleted |

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
    "name": "Name is required"
  }
}
```

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| `400 Bad Request` | Validation error or business rule violation |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User lacks required ADMIN role |
| `405 Method Not Allowed` | HTTP method not supported for endpoint |
| `500 Internal Server Error` | Unexpected server error |

---

## Business Rules

### Soft Delete

Environments are **soft deleted** rather than permanently removed from the database:

- When deleted, `isActive` is set to `false`
- Soft-deleted environments are excluded from all API responses
- This preserves referential integrity with feature flags that reference the environment
- There is no restore functionality; once deleted, an environment cannot be recovered via the API

### Key Immutability

The `key` field **cannot be changed** after creation:

- This ensures SDK integrations remain stable
- Update requests that include `key` will ignore the field
- To change a key, create a new environment and migrate configurations

### Key Uniqueness

The `key` must be unique **among active environments only**:

- Creating an environment with a key that exists on an active environment will fail
- If an environment was soft-deleted, its key can be reused for a new environment
- This allows recovery scenarios where a deleted environment's key needs to be recreated

### Audit Trail

All environments automatically track:

- `createdAt`: Timestamp when the environment was created
- `updatedAt`: Timestamp of the last modification
- `createdBy`: User who created the environment (stored internally)
- `updatedBy`: User who last modified the environment (stored internally)

---

## Examples

### cURL Examples

**List all environments:**
```bash
curl -X GET "http://localhost:8080/environments" \
  -H "Authorization: Bearer <token>"
```

**Search environments:**
```bash
curl -X GET "http://localhost:8080/environments?search=prod" \
  -H "Authorization: Bearer <token>"
```

**Create environment:**
```bash
curl -X POST "http://localhost:8080/environments" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "staging",
    "name": "Staging",
    "description": "Pre-production testing"
  }'
```

**Update environment:**
```bash
curl -X PATCH "http://localhost:8080/environments/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Staging (EU)",
    "description": "European staging environment"
  }'
```

**Delete environment:**
```bash
curl -X DELETE "http://localhost:8080/environments/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>"
```
