# Feature Flags Node SDK

A lightweight Node.js SDK for evaluating feature flags with built-in local caching. Uses native `fetch` — zero runtime dependencies.

**Requires Node.js >= 18**

## How It Works

```
┌─────────────────────────────────────────────────────┐
│                   Your Application                  │
│                                                     │
│   client.getBooleanFlag("dark-mode", "user-42")     │
│                        │                            │
│                        ▼                            │
│              ┌─────────────────┐                    │
│              │  Local Cache    │                    │
│              │  (in-memory,    │                    │
│              │   TTL-based)    │                    │
│              └────────┬────────┘                    │
│                       │                             │
│              cache hit│  cache miss                 │
│               ┌───────┘───────┐                     │
│               ▼               ▼                     │
│          Return value    ┌───────────┐              │
│          immediately     │HTTP Client│              │
│                          └─────┬─────┘              │
│                                │                    │
└────────────────────────────────│────────────────────┘
                                 │  GET /evaluate/{flagKey}?user={userId}
                                 │  Header: X-API-Key
                                 ▼
                       ┌───────────────────┐
                       │  Evaluation API   │
                       │  (remote server)  │
                       └───────────────────┘
```

1. **Initialize** — Create a `FeatureFlagClient` with your environment's API key (generated in the admin dashboard). The SDK resolves the evaluation API endpoint automatically (configurable via env var for local dev).

2. **Evaluate** — Call a typed getter like `getBooleanFlag(flagKey, userId, defaultValue)`. The SDK checks the local in-memory cache first. On a cache hit, it returns instantly with no network call.

3. **Cache miss → API call** — On a cache miss, the SDK calls the evaluation API (`GET /evaluate/{flagKey}?user={userId}`) with your API key in the `X-API-Key` header. The API evaluates the flag (percentage-based rollout using consistent hashing) and returns the result.

4. **Cache & return** — The result is stored in the local cache (default TTL: 30 seconds) and returned. Subsequent calls for the same flag+user return from cache until the TTL expires.

5. **Error handling** — If the flag isn't found or the API is unreachable, the SDK returns your `defaultValue` silently. Authentication errors (invalid API key) are always thrown so you can fix your configuration.

6. **Cleanup** — A background timer prunes expired cache entries every 30 seconds. Call `client.close()` when you're done to stop the timer. The timer is unreferenced, so it won't prevent your Node.js process from exiting.

## Installation

```bash
npm install featureflags-node-sdk
```

## Quick Start

```ts
import { FeatureFlagClient } from "featureflags-node-sdk";

const client = new FeatureFlagClient({
  apiKey: "ff_production_your-api-key",
});

// Evaluate a boolean flag
const darkMode = await client.getBooleanFlag("dark-mode", "user-42", false);
console.log("Dark mode enabled:", darkMode);

// Evaluate a string flag
const theme = await client.getStringFlag("theme", "user-42", "light");

// Evaluate a number flag
const maxItems = await client.getNumberFlag("max-items", "user-42", 10);

// Bulk evaluate all flags
const allFlags = await client.getAllFlags("user-42");

// Clean up when done
client.close();
```

## Configuration

| Option           | Type     | Default                                                      | Description                          |
| ---------------- | -------- | ------------------------------------------------------------ | ------------------------------------ |
| `apiKey`         | `string` | **(required)**                                               | API key (must start with `ff_`)      |
| `baseUrl`        | `string` | `https://strong-lorena-07manan-b3c1d402.koyeb.app`          | Evaluation API base URL              |
| `cacheTTL`       | `number` | `30000` (30s)                                                | Cache time-to-live in milliseconds   |
| `requestTimeout` | `number` | `10000` (10s)                                                | HTTP request timeout in milliseconds |

### Base URL Resolution

The SDK resolves the evaluation API endpoint in this order:

1. **Explicit `baseUrl` option** — passed in the constructor
2. **`FEATUREFLAGS_BASE_URL` environment variable** — for local development
3. **Default deployed URL** — production endpoint

For local development, set the environment variable instead of changing code:

```bash
export FEATUREFLAGS_BASE_URL=http://localhost:8081
```

## API Reference

### `FeatureFlagClient`

#### Constructor

```ts
new FeatureFlagClient(options: FeatureFlagClientOptions)
```

Throws `FeatureFlagError` if the API key is missing or doesn't start with `ff_`.

#### Methods

| Method | Returns | Description |
| --- | --- | --- |
| `getBooleanFlag(flagKey, userId?, defaultValue?)` | `Promise<boolean>` | Evaluate a boolean flag |
| `getStringFlag(flagKey, userId?, defaultValue?)` | `Promise<string>` | Evaluate a string flag |
| `getNumberFlag(flagKey, userId?, defaultValue?)` | `Promise<number>` | Evaluate a number flag |
| `getAllFlags(userId?)` | `Promise<Record<string, EvaluationResult>>` | Evaluate all flags (populates cache) |
| `invalidateCache(flagKey, userId?)` | `void` | Remove a specific entry from cache |
| `clearCache()` | `void` | Remove all cache entries |
| `close()` | `void` | Shut down the client and cleanup timer |

### Typed Getters Behavior

- **Cache-first**: checks local cache before making an API call
- **Default on miss**: returns `defaultValue` when the flag is not found (404) or on network errors
- **Auth errors propagate**: `AuthenticationError` (401) is always thrown, never swallowed
- **Type safety**: returns `defaultValue` if the flag type doesn't match the getter (e.g., calling `getBooleanFlag` on a `STRING` flag)

## Error Handling

```ts
import {
  FeatureFlagClient,
  AuthenticationError,
  FlagNotFoundError,
  FeatureFlagError,
} from "featureflags-node-sdk";

try {
  const value = await client.getBooleanFlag("my-flag", "user-1");
} catch (error) {
  if (error instanceof AuthenticationError) {
    // Invalid API key — fix your configuration
    console.error("Auth failed:", error.message);
  }
  // FlagNotFoundError and other errors return the default value
  // and are NOT thrown from typed getters
}
```

## Caching

The SDK maintains a local in-memory cache with configurable TTL (default: 30 seconds).

- Cache key format: `flagKey:userId`
- Background cleanup runs every 30 seconds (pruning expired entries)
- `getAllFlags()` populates individual cache entries for each flag
- The cleanup timer is unreferenced, so it won't prevent the Node.js process from exiting

```ts
// Manual cache control
client.invalidateCache("dark-mode", "user-42"); // remove one entry
client.clearCache(); // remove all entries
```

## License

MIT
