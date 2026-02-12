# Feature Flags Java SDK

A lightweight Java SDK for evaluating feature flags with local caching support.

## Features

- **Type-safe flag evaluation**: Separate methods for boolean, string, and numeric flags
- **Local caching**: In-memory cache with configurable TTL (default 30 seconds)
- **Robust error handling**: Graceful degradation with default values
- **Thread-safe**: All operations are thread-safe for concurrent usage
- **Minimal dependencies**: Only requires SLF4J, Jackson, and Apache HttpClient
- **Configurable endpoints**: Easy testing against local or production APIs

## Requirements

- Java 21 or higher
- Maven 3.6+ (for building)

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github._manan.featureflags</groupId>
    <artifactId>featureflags-java-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

```java
import com.github._manan.featureflags.sdk.FeatureFlagClient;

// Initialize the client with your API key
FeatureFlagClient client = FeatureFlagClient.builder()
    .apiKey("ff_production_xxxxx")
    .build();

// Evaluate a boolean flag
boolean newFeatureEnabled = client.getBooleanFlag(
    "new-checkout-flow", 
    "user-123", 
    false  // default value
);

if (newFeatureEnabled) {
    // Use new checkout flow
} else {
    // Use old checkout flow
}

// Evaluate a string flag
String theme = client.getStringFlag("theme-color", "user-123", "blue");

// Evaluate numeric flags
int rateLimit = client.getIntFlag("rate-limit", "user-123", 100);
double discountRate = client.getDoubleFlag("discount-rate", "user-123", 0.1);

// Get all active flags at once
Map<String, Object> allFlags = client.getAllFlags("user-123");

// Clean up when done
client.close();
```

## Configuration

### Builder Options

The SDK can be configured using the builder pattern:

```java
FeatureFlagClient client = FeatureFlagClient.builder()
    .apiKey("ff_production_xxxxx")              // Required: Your environment's API key
    .baseUrl("http://localhost:8081")            // Optional: API endpoint
    .cacheTTL(60, TimeUnit.SECONDS)              // Optional: Cache TTL (default 30s)
    .httpTimeout(10, 20, TimeUnit.SECONDS)       // Optional: HTTP timeouts (default 5s, 10s)
    .build();
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiKey` | String | **Required** | API key from your environment |
| `baseUrl` | String | `https://strong-lorena-07manan-b3c1d402.koyeb.app` | Evaluation API endpoint |
| `cacheTTL` | long, TimeUnit | 30 seconds | Time-to-live for cached flag values |
| `httpTimeout` | long, long, TimeUnit | 5s, 10s | Connection and socket timeouts |

### System Property Override

For testing purposes, you can override the base URL using a system property:

```bash
# Via command line
java -Dfeatureflags.baseUrl=http://localhost:8081 -jar your-app.jar

# Via Maven
mvn test -Dfeatureflags.baseUrl=http://localhost:8081
```

This is useful for:
- **SDK Development**: Testing against local evaluation API
- **Integration Tests**: Running tests in different environments
- **CI/CD**: Switching endpoints without code changes

## API Reference

### Boolean Flags

```java
boolean getBooleanFlag(String flagKey, String userId, boolean defaultValue)
```

Evaluates a boolean feature flag.

- **Parameters:**
  - `flagKey`: The flag's unique identifier
  - `userId`: User ID for percentage rollouts (can be `null`)
  - `defaultValue`: Value to return if flag not found or on error
- **Returns:** Evaluated boolean value
- **Throws:** `AuthenticationException` if API key is invalid

### String Flags

```java
String getStringFlag(String flagKey, String userId, String defaultValue)
```

Evaluates a string feature flag.

- **Parameters:** Same as boolean flags
- **Returns:** Evaluated string value

### Integer Flags

```java
int getIntFlag(String flagKey, String userId, int defaultValue)
```

Evaluates an integer flag.

- **Parameters:** Same as boolean flags
- **Returns:** Evaluated integer value

### Double Flags

```java
double getDoubleFlag(String flagKey, String userId, double defaultValue)
```

Evaluates a double/decimal flag.

- **Parameters:** Same as boolean flags
- **Returns:** Evaluated double value

### Bulk Evaluation

```java
Map<String, Object> getAllFlags(String userId)
```

Evaluates all active flags at once. More efficient than individual calls when you need multiple flags.

- **Parameters:**
  - `userId`: User ID for percentage rollouts (can be `null`)
- **Returns:** Map of flag keys to their evaluated values
- **Throws:** `AuthenticationException` or `FeatureFlagException`

### Cache Management

```java
void invalidateCache(String flagKey, String userId)
void clearCache()
```

Manually manage the local cache:
- `invalidateCache`: Removes a specific flag from cache
- `clearCache`: Removes all cached flags

## Caching Behavior

The SDK implements a local in-memory cache with the following characteristics:

- **Default TTL:** 30 seconds (configurable)
- **Cache Key:** Combination of flag key and user ID
- **Thread-Safe:** Uses `ConcurrentHashMap` internally
- **Auto Cleanup:** Background task removes expired entries every 30 seconds

### Cache Strategy

1. First request → API call → Cache result
2. Subsequent requests (within TTL) → Return cached value
3. After TTL expiration → API call → Update cache
4. On error → Return cached value if available, otherwise default

### When to Clear Cache

- **Manual refresh**: User explicitly requests updated flags
- **Flag changes**: You've changed flags and need immediate update
- **Testing**: Reset state between test cases

**Note:** In production, rely on TTL expiration rather than manual clearing.

## Error Handling

The SDK follows these error handling principles:

| Scenario | Behavior |
|----------|----------|
| Flag not found (404) | Returns default value silently (logs debug message) |
| Invalid API key (401) | Throws `AuthenticationException` (configuration error) |
| Server error (5xx) | Returns default value (logs error) |
| Network timeout | Returns default value (logs error) |
| Type mismatch | Returns default value (logs warning) |

### Exception Types

```java
// Base exception
FeatureFlagException

// Invalid/missing API key (should fix configuration)
AuthenticationException extends FeatureFlagException

// Flag doesn't exist (handle gracefully with default)
FlagNotFoundException extends FeatureFlagException
```

### Best Practices

```java
try (FeatureFlagClient client = FeatureFlagClient.builder()
        .apiKey(apiKey)
        .build()) {
    
    // Safe evaluation - always returns a value
    boolean feature = client.getBooleanFlag("my-flag", userId, false);
    
} catch (AuthenticationException e) {
    // Configuration error - log and alert
    logger.error("Invalid feature flag API key", e);
    // Fall back to all-defaults or fail fast
}
// Client auto-closed via try-with-resources
```

## User Context

The `userId` parameter is optional but important:

- **With userId**: Enables percentage-based rollouts (A/B testing)
  - Same user always gets same value (deterministic)
  - Different users get distributed across variants
- **Without userId** (`null`): Always gets first variant or default value

```java
// For logged-in users
boolean feature = client.getBooleanFlag("new-ui", user.getId(), false);

// For anonymous/non-personalized flags
boolean maintenance = client.getBooleanFlag("maintenance-mode", null, false);
```

## Testing

### Running Unit Tests

Unit tests use mocked HTTP client and don't require external services:

```bash
mvn clean test
```

### Running Integration Tests

Integration tests require:
1. Local evaluation API running on `http://localhost:8081`
2. Valid API key from a test environment
3. Test flags created in that environment

To run integration tests:

```bash
# 1. Update TEST_API_KEY in IntegrationTest.java with your key
# 2. Remove @Disabled annotation
# 3. Start local evaluation API
# 4. Run tests
mvn test -Dtest=IntegrationTest -Dfeatureflags.baseUrl=http://localhost:8081
```

### Testing Your Application

Override the base URL for testing:

```java
// In your test setup
System.setProperty("featureflags.baseUrl", "http://localhost:8081");

FeatureFlagClient client = FeatureFlagClient.builder()
    .apiKey("ff_test_key")
    .build();

// Client will use http://localhost:8081 instead of production URL
```

## Performance Considerations

- **First call**: ~10-50ms (API request + network)
- **Cached calls**: <1ms (in-memory lookup)
- **Cache hit rate**: Typically 95-98% with 30s TTL
- **Memory usage**: ~1KB per cached flag value
- **Thread safety**: All operations are thread-safe

### Optimization Tips

1. **Use bulk evaluation** for multiple flags:
   ```java
   Map<String, Object> flags = client.getAllFlags(userId);
   // Now access multiple flags from cache
   ```

2. **Adjust cache TTL** based on your needs:
   - Shorter TTL (10-15s): Faster flag updates, more API calls
   - Longer TTL (60-120s): Fewer API calls, slower propagation

3. **Reuse client instance** across requests:
   ```java
   // Good: Single instance
   FeatureFlagClient client = FeatureFlagClient.builder()...build();
   
   // Bad: Creating new instances repeatedly
   // Don't do this in a loop or per-request
   ```

## Logging

The SDK uses SLF4J for logging. Configure your logging framework (Logback, Log4j2, etc.) to control output:

- **INFO**: Client initialization and shutdown
- **DEBUG**: Flag evaluations, cache operations
- **TRACE**: Detailed cache hits/misses, API calls
- **WARN**: Type mismatches, configuration issues
- **ERROR**: API errors, network failures

Example Logback configuration:

```xml
<logger name="com.github._manan.featureflags.sdk" level="INFO"/>
```

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd sdk/java-sdk

# Build
mvn clean package

# Install to local Maven repository
mvn clean install

# Run tests
mvn clean test
```
