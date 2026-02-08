# Flag Values API

The Flag Values API allows you to manage environment-specific value configurations for feature flags. Flag values enable percentage-based rollouts where different users receive different values based on configurable distribution percentages.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Data Model](#data-model)
  - [Flag Value Object](#flag-value-object)
  - [Variant Object](#variant-object)
  - [Field Constraints](#field-constraints)
- [Endpoints](#endpoints)
  - [List Flag Values](#list-flag-values)
  - [Get Flag Value](#get-flag-value)
  - [Create Flag Value](#create-flag-value)
  - [Update Flag Value](#update-flag-value)
  - [Delete Flag Value](#delete-flag-value)
- [Error Responses](#error-responses)
- [Business Rules](#business-rules)
- [Examples](#examples)

---

## Overview

Flag Values represent environment-specific configurations for feature flags. Each flag value:

- Associates a **Flag** with an **Environment**
- Contains one or more **Variants** with percentage-based distribution
- Enables A/B testing and gradual rollouts
- Percentages must always sum to exactly 100%

When a flag is evaluated in a specific environment, the evaluation service uses the variants and their percentages to determine which value a user receives.

**Base URL:** `/flags/{flagId}/values`

---

## Authentication

All endpoints require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

Requests without a valid token will receive a `401 Unauthorized` response.

---

## Authorization

All Flag Values API endpoints require the **ADMIN** role. Users without this role will receive a `403 Forbidden` response.

---

## Data Model

### Flag Value Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier (read-only, auto-generated) |
| `flagId` | `UUID` | Reference to the parent flag (read-only) |
| `flagKey` | `string` | The parent flag's programmatic key (read-only) |
| `flagType` | `FlagType` | The parent flag's type: `STRING`, `BOOLEAN`, or `NUMBER` (read-only) |
| `environmentId` | `UUID` | Reference to the target environment |
| `environmentKey` | `string` | The environment's programmatic key (read-only) |
| `variants` | `Variant[]` | List of value variants with percentages |
| `isActive` | `boolean` | Whether the flag value is active (read-only) |
| `createdAt` | `ISO 8601` | Timestamp of creation (read-only) |
| `updatedAt` | `ISO 8601` | Timestamp of last update (read-only) |

### Variant Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier for the variant (read-only, auto-generated) |
| `value` | `string` | The value to return (must be valid for the flag's type) |
| `percentage` | `integer` | Distribution percentage (0-100) |

### Field Constraints

| Field | Constraints |
|-------|-------------|
| `environmentId` | Required, must reference an active environment |
| `variants` | Required, at least one variant |
| `variants[].value` | Required, non-blank, max 500 characters, must match flag's type |
| `variants[].percentage` | Required, integer between 0 and 100 |
| Percentage sum | Must equal exactly 100 |

### Value Type Validation

Variant values are validated against the parent flag's type:

| Flag Type | Valid Values | Invalid Examples |
|-----------|--------------|------------------|
| `STRING` | Any non-blank string | (none) |
| `BOOLEAN` | `"true"` or `"false"` (case-insensitive) | `"yes"`, `"1"`, `"on"` |
| `NUMBER` | Any valid number (integer or decimal) | `"abc"`, `"12.3.4"` |

---

## Endpoints

### List Flag Values

Retrieve all active flag values for a specific flag.

**Request**

```
GET /flags/{flagId}/values
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flagId` | `UUID` | Yes | The parent flag's unique identifier |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "flagId": "550e8400-e29b-41d4-a716-446655440000",
    "flagKey": "new-checkout-flow",
    "flagType": "BOOLEAN",
    "environmentId": "770e8400-e29b-41d4-a716-446655440000",
    "environmentKey": "production",
    "variants": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440000",
        "value": "true",
        "percentage": 20
      },
      {
        "id": "880e8400-e29b-41d4-a716-446655440001",
        "value": "false",
        "percentage": 80
      }
    ],
    "isActive": true,
    "createdAt": "2026-02-01T10:00:00Z",
    "updatedAt": "2026-02-01T10:00:00Z"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "flagId": "550e8400-e29b-41d4-a716-446655440000",
    "flagKey": "new-checkout-flow",
    "flagType": "BOOLEAN",
    "environmentId": "770e8400-e29b-41d4-a716-446655440001",
    "environmentKey": "staging",
    "variants": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440002",
        "value": "true",
        "percentage": 100
      }
    ],
    "isActive": true,
    "createdAt": "2026-02-01T09:00:00Z",
    "updatedAt": "2026-02-01T09:00:00Z"
  }
]
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has been deleted |

---

### Get Flag Value

Retrieve a single flag value by ID.

**Request**

```
GET /flags/{flagId}/values/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flagId` | `UUID` | Yes | The parent flag's unique identifier |
| `id` | `UUID` | Yes | The flag value's unique identifier |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "flagId": "550e8400-e29b-41d4-a716-446655440000",
  "flagKey": "new-checkout-flow",
  "flagType": "BOOLEAN",
  "environmentId": "770e8400-e29b-41d4-a716-446655440000",
  "environmentKey": "production",
  "variants": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440000",
      "value": "true",
      "percentage": 20
    },
    {
      "id": "880e8400-e29b-41d4-a716-446655440001",
      "value": "false",
      "percentage": 80
    }
  ],
  "isActive": true,
  "createdAt": "2026-02-01T10:00:00Z",
  "updatedAt": "2026-02-01T10:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has been deleted |
| `404 Not Found` | Flag value not found, deleted, or belongs to a different flag |

---

### Create Flag Value

Create a new flag value for a specific environment.

**Request**

```
POST /flags/{flagId}/values
Content-Type: application/json
```

```json
{
  "environmentId": "770e8400-e29b-41d4-a716-446655440000",
  "variants": [
    {
      "value": "true",
      "percentage": 30
    },
    {
      "value": "false",
      "percentage": 70
    }
  ]
}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flagId` | `UUID` | Yes | The parent flag's unique identifier |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `environmentId` | `UUID` | Yes | Target environment's unique identifier |
| `variants` | `Variant[]` | Yes | List of value variants |
| `variants[].value` | `string` | Yes | The value (must match flag's type) |
| `variants[].percentage` | `integer` | Yes | Distribution percentage (0-100) |

**Response**

```
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440002",
  "flagId": "550e8400-e29b-41d4-a716-446655440000",
  "flagKey": "new-checkout-flow",
  "flagType": "BOOLEAN",
  "environmentId": "770e8400-e29b-41d4-a716-446655440000",
  "environmentKey": "production",
  "variants": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "value": "true",
      "percentage": 30
    },
    {
      "id": "880e8400-e29b-41d4-a716-446655440004",
      "value": "false",
      "percentage": 70
    }
  ],
  "isActive": true,
  "createdAt": "2026-02-08T14:00:00Z",
  "updatedAt": "2026-02-08T14:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed (see [Validation Errors](#validation-errors)) |
| `400 Bad Request` | Flag value already exists for this flag+environment combination |
| `400 Bad Request` | Variant value is not valid for the flag's type |
| `400 Bad Request` | Percentages do not sum to 100 |
| `404 Not Found` | Flag not found or has been deleted |
| `404 Not Found` | Environment not found or has been deleted |

**Example: Duplicate Flag+Environment Error**

```json
{
  "timestamp": "2026-02-08T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Flag value already exists for flag 'new-checkout-flow' in environment 'production'"
}
```

**Example: Invalid Percentage Sum**

```json
{
  "timestamp": "2026-02-08T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "variants": "Percentages must sum to 100, got: 80"
  }
}
```

**Example: Invalid Boolean Value**

```json
{
  "timestamp": "2026-02-08T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Variant at index 0 has invalid BOOLEAN value: 'yes'. Must be 'true' or 'false'"
}
```

---

### Update Flag Value

Update the variants for an existing flag value. This is a full replacement operation—all variants are replaced atomically.

**Request**

```
PUT /flags/{flagId}/values/{id}
Content-Type: application/json
```

```json
{
  "environmentId": "770e8400-e29b-41d4-a716-446655440000",
  "variants": [
    {
      "value": "true",
      "percentage": 50
    },
    {
      "value": "false",
      "percentage": 50
    }
  ]
}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flagId` | `UUID` | Yes | The parent flag's unique identifier |
| `id` | `UUID` | Yes | The flag value's unique identifier |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `environmentId` | `UUID` | Yes | Environment ID (must match existing, cannot change) |
| `variants` | `Variant[]` | Yes | New list of value variants (replaces all existing) |
| `variants[].value` | `string` | Yes | The value (must match flag's type) |
| `variants[].percentage` | `integer` | Yes | Distribution percentage (0-100) |

> **Note:** The `environmentId` cannot be changed. To move a flag value to a different environment, delete it and create a new one.

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440002",
  "flagId": "550e8400-e29b-41d4-a716-446655440000",
  "flagKey": "new-checkout-flow",
  "flagType": "BOOLEAN",
  "environmentId": "770e8400-e29b-41d4-a716-446655440000",
  "environmentKey": "production",
  "variants": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440005",
      "value": "true",
      "percentage": 50
    },
    {
      "id": "880e8400-e29b-41d4-a716-446655440006",
      "value": "false",
      "percentage": 50
    }
  ],
  "isActive": true,
  "createdAt": "2026-02-08T14:00:00Z",
  "updatedAt": "2026-02-08T15:00:00Z"
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed |
| `400 Bad Request` | Percentages do not sum to 100 |
| `400 Bad Request` | Variant value is not valid for the flag's type |
| `404 Not Found` | Flag not found or has been deleted |
| `404 Not Found` | Flag value not found, deleted, or belongs to a different flag |

---

### Delete Flag Value

Soft delete a flag value. The flag value becomes inactive and will not be returned in queries.

**Request**

```
DELETE /flags/{flagId}/values/{id}
```

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flagId` | `UUID` | Yes | The parent flag's unique identifier |
| `id` | `UUID` | Yes | The flag value's unique identifier |

**Response**

```
HTTP/1.1 204 No Content
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `404 Not Found` | Flag not found or has been deleted |
| `404 Not Found` | Flag value not found, already deleted, or belongs to a different flag |

---

## Error Responses

### Validation Errors

When request body validation fails, the API returns a structured error response:

```json
{
  "timestamp": "2026-02-08T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "environmentId": "Environment ID is required",
    "variants": "At least one variant is required"
  }
}
```

### Common Validation Messages

| Field | Message |
|-------|---------|
| `environmentId` | "Environment ID is required" |
| `variants` | "At least one variant is required" |
| `variants` | "Percentages must sum to 100, got: {sum}" |
| `variants[n].value` | "Variant value is required" |
| `variants[n].value` | "Variant at index {n} has blank value" |
| `variants[n].percentage` | "Percentage is required" |
| `variants[n].percentage` | "Percentage must be at least 0" |
| `variants[n].percentage` | "Percentage must be at most 100" |

---

## Business Rules

### Uniqueness

- Each flag can have **at most one active flag value per environment**
- Attempting to create a duplicate will return a `400 Bad Request` error

### Percentage Distribution

- All variant percentages must sum to exactly **100**
- Individual percentages can be **0** (variant never selected)
- Minimum one variant is required

### Value Type Consistency

- Variant values must match the parent flag's type
- `BOOLEAN` flags only accept `"true"` or `"false"` (case-insensitive)
- `NUMBER` flags only accept valid numeric strings
- `STRING` flags accept any non-blank string

### Cascade Deletion

- When a **Flag** is deleted, all its flag values are automatically deactivated
- When an **Environment** is deleted, all flag values for that environment are automatically deactivated
- This ensures referential integrity without orphaned records

---

## Examples

### Gradual Rollout (10% → 50% → 100%)

**Step 1: Initial 10% rollout**

```bash
curl -X POST "http://localhost:8080/flags/{flagId}/values" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "true", "percentage": 10},
      {"value": "false", "percentage": 90}
    ]
  }'
```

**Step 2: Expand to 50%**

```bash
curl -X PUT "http://localhost:8080/flags/{flagId}/values/{flagValueId}" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "true", "percentage": 50},
      {"value": "false", "percentage": 50}
    ]
  }'
```

**Step 3: Full rollout**

```bash
curl -X PUT "http://localhost:8080/flags/{flagId}/values/{flagValueId}" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "true", "percentage": 100}
    ]
  }'
```

### A/B/C Testing with STRING Flag

```bash
curl -X POST "http://localhost:8080/flags/{flagId}/values" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "variant-a", "percentage": 33},
      {"value": "variant-b", "percentage": 33},
      {"value": "variant-c", "percentage": 34}
    ]
  }'
```

### Numeric Value Experiment

```bash
curl -X POST "http://localhost:8080/flags/{flagId}/values" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "5", "percentage": 25},
      {"value": "10", "percentage": 25},
      {"value": "15", "percentage": 25},
      {"value": "20", "percentage": 25}
    ]
  }'
```

### Kill Switch (0% of new feature)

```bash
curl -X PUT "http://localhost:8080/flags/{flagId}/values/{flagValueId}" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "{productionEnvId}",
    "variants": [
      {"value": "false", "percentage": 100}
    ]
  }'
```
