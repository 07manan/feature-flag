# Evaluation API

A high-performance, read-only Go API service for evaluating feature flags. Designed for SDK consumption with deterministic percentage-based rollouts.

## Features

- **Fast Flag Evaluation**: Direct PostgreSQL queries optimized for read performance
- **Percentage Rollout**: Deterministic user bucketing using MurmurHash3
- **API Key Authentication**: Environment-scoped access via `X-API-Key` header
- **Bulk Evaluation**: Evaluate all flags in a single request
- **Health Checks**: Liveness (`/health`) and readiness (`/ready`) endpoints

## API Endpoints

### Evaluate Single Flag

```
GET /evaluate/{flagKey}?user={userId}
```

**Headers:**
- `X-API-Key` (required): Environment API key

**Query Parameters:**
- `user` (optional): User identifier for percentage rollout bucketing

**Response:**
```json
{
  "flagKey": "new-checkout",
  "value": true,
  "type": "BOOLEAN",
  "isDefault": false,
  "variantId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Evaluate All Flags

```
GET /evaluate?user={userId}
```

**Headers:**
- `X-API-Key` (required): Environment API key

**Query Parameters:**
- `user` (optional): User identifier for percentage rollout bucketing

**Response:**
```json
{
  "flags": {
    "new-checkout": {
      "flagKey": "new-checkout",
      "value": true,
      "type": "BOOLEAN",
      "isDefault": false,
      "variantId": "550e8400-e29b-41d4-a716-446655440000"
    },
    "max-items": {
      "flagKey": "max-items",
      "value": 100,
      "type": "NUMBER",
      "isDefault": true
    }
  }
}
```

### Health Check (Liveness)

```
GET /health
```

**Response:**
```json
{
  "status": "ok"
}
```

### Readiness Check

```
GET /ready
```

**Response (healthy):**
```json
{
  "status": "ready"
}
```

**Response (unhealthy):**
```json
{
  "status": "unavailable",
  "reason": "database connection failed"
}
```

## Error Responses

All errors follow this format:

```json
{
  "error": "not_found",
  "message": "Flag not found"
}
```

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `bad_request` | Invalid request parameters |
| 401 | `unauthorized` | Invalid or missing API key |
| 404 | `not_found` | Flag not found |
| 500 | `internal_error` | Server error |

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8081` | HTTP server port |
| `SERVER_READ_TIMEOUT` | `5s` | Request read timeout |
| `SERVER_WRITE_TIMEOUT` | `10s` | Response write timeout |
| `SERVER_SHUTDOWN_TIMEOUT` | `30s` | Graceful shutdown timeout |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_USER` | `postgres` | PostgreSQL user |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `DB_NAME` | `featureflags` | PostgreSQL database name |
| `DB_SSL_MODE` | `disable` | PostgreSQL SSL mode |
| `DB_MAX_CONNS` | `25` | Maximum database connections |
| `DB_MIN_CONNS` | `5` | Minimum database connections |

## Running Locally

```bash
# Install dependencies
go mod tidy

# Run with default config (connects to localhost:5432)
go run cmd/server/main.go

# Run with custom config
DB_HOST=localhost DB_PORT=5432 go run cmd/server/main.go
```

## Docker

```bash
# Build
docker build -t evaluation-api .

# Run
docker run -p 8081:8081 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e DB_NAME=featureflags \
  evaluation-api
```

## Percentage Rollout

The evaluation API uses MurmurHash3 for deterministic user bucketing:

1. A hash is computed from `flagKey + ":" + userId`
2. The hash is mapped to a bucket (0-99)
3. The bucket is compared against variant percentages

This ensures:
- **Consistency**: Same user always gets the same variant for a given flag
- **Even distribution**: Users are evenly distributed across buckets
- **Independence**: Different flags can have different rollout percentages for the same user

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        HTTP Layer                           │
│  • Chi Router                                               │
│  • CORS, Logging, Recovery middleware                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                          │
│  • API key authentication                                   │
│  • Flag evaluation logic                                    │
│  • Percentage rollout (MurmurHash3)                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Repository Layer                        │
│  • PostgreSQL queries via pgx                               │
│  • Connection pooling                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       PostgreSQL                            │
│  • flags, environments, flag_values, flag_value_variants    │
└─────────────────────────────────────────────────────────────┘
```
