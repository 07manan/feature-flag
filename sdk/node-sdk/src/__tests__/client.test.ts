import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { FeatureFlagClient } from "../client.js";
import { AuthenticationError, FeatureFlagError } from "../errors.js";
import { FlagType } from "../types.js";
import type { EvaluationResult } from "../types.js";

describe("FeatureFlagClient", () => {
  let client: FeatureFlagClient;
  const mockFetch = vi.fn();

  const mockResponse = (status: number, body: unknown): Response =>
    ({
      ok: status >= 200 && status < 300,
      status,
      statusText: status === 200 ? "OK" : "Error",
      json: () => Promise.resolve(body),
    }) as unknown as Response;

  const booleanResult: EvaluationResult = {
    flagKey: "dark-mode",
    value: true,
    type: FlagType.BOOLEAN,
    isDefault: false,
    variantId: "variant-1",
  };

  const stringResult: EvaluationResult = {
    flagKey: "theme",
    value: "midnight",
    type: FlagType.STRING,
    isDefault: false,
    variantId: "variant-2",
  };

  const numberResult: EvaluationResult = {
    flagKey: "max-items",
    value: 50,
    type: FlagType.NUMBER,
    isDefault: false,
    variantId: "variant-3",
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", mockFetch);
    mockFetch.mockReset();

    client = new FeatureFlagClient({
      apiKey: "ff_test_key123",
      baseUrl: "http://localhost:8081",
      cacheTTL: 5000,
    });
  });

  afterEach(() => {
    client.close();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  // ---------------------------------------------------------------------------
  // Construction & validation
  // ---------------------------------------------------------------------------

  describe("constructor", () => {
    it("should throw if API key does not start with ff_", () => {
      expect(
        () => new FeatureFlagClient({ apiKey: "invalid_key" })
      ).toThrow(FeatureFlagError);
    });

    it("should throw if API key is empty", () => {
      expect(
        () => new FeatureFlagClient({ apiKey: "" })
      ).toThrow(FeatureFlagError);
    });

    it("should accept a valid API key", () => {
      const c = new FeatureFlagClient({
        apiKey: "ff_prod_abc",
        baseUrl: "http://localhost:8081",
      });
      c.close();
    });

    it("should use FEATUREFLAGS_BASE_URL env var when no baseUrl provided", () => {
      vi.stubEnv("FEATUREFLAGS_BASE_URL", "http://custom:9090");

      mockFetch.mockResolvedValueOnce(mockResponse(200, booleanResult));

      const c = new FeatureFlagClient({ apiKey: "ff_test_key" });
      c.getBooleanFlag("dark-mode", "user-1");

      const [url] = mockFetch.mock.calls[0]!;
      expect(url).toContain("http://custom:9090/");

      c.close();
      vi.unstubAllGlobals();
    });
  });

  // ---------------------------------------------------------------------------
  // Boolean flags
  // ---------------------------------------------------------------------------

  describe("getBooleanFlag", () => {
    it("should return the flag value from API", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, booleanResult));

      const value = await client.getBooleanFlag("dark-mode", "user-1", false);
      expect(value).toBe(true);
    });

    it("should return cached value on second call", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, booleanResult));

      await client.getBooleanFlag("dark-mode", "user-1");
      const value = await client.getBooleanFlag("dark-mode", "user-1");

      expect(mockFetch).toHaveBeenCalledOnce();
      expect(value).toBe(true);
    });

    it("should fetch again after cache expires", async () => {
      mockFetch
        .mockResolvedValueOnce(mockResponse(200, booleanResult))
        .mockResolvedValueOnce(mockResponse(200, { ...booleanResult, value: false }));

      await client.getBooleanFlag("dark-mode", "user-1");
      vi.advanceTimersByTime(6000); // past 5s TTL
      const value = await client.getBooleanFlag("dark-mode", "user-1");

      expect(mockFetch).toHaveBeenCalledTimes(2);
      expect(value).toBe(false);
    });

    it("should return default value when flag is not found", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(404, { error: "not_found", message: "Flag not found" })
      );

      const value = await client.getBooleanFlag("missing", "user-1", true);
      expect(value).toBe(true);
    });

    it("should throw AuthenticationError on 401", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(401, { error: "unauthorized", message: "Invalid API key" })
      );

      await expect(
        client.getBooleanFlag("dark-mode", "user-1")
      ).rejects.toThrow(AuthenticationError);
    });

    it("should return default when type does not match", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, stringResult));

      const value = await client.getBooleanFlag("theme", "user-1", false);
      expect(value).toBe(false);
    });
  });

  // ---------------------------------------------------------------------------
  // String flags
  // ---------------------------------------------------------------------------

  describe("getStringFlag", () => {
    it("should return the string value from API", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, stringResult));

      const value = await client.getStringFlag("theme", "user-1", "default");
      expect(value).toBe("midnight");
    });

    it("should return default when flag not found", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(404, { error: "not_found", message: "not found" })
      );

      const value = await client.getStringFlag("missing", "user-1", "fallback");
      expect(value).toBe("fallback");
    });
  });

  // ---------------------------------------------------------------------------
  // Number flags
  // ---------------------------------------------------------------------------

  describe("getNumberFlag", () => {
    it("should return the number value from API", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, numberResult));

      const value = await client.getNumberFlag("max-items", "user-1", 10);
      expect(value).toBe(50);
    });

    it("should return default when flag not found", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(404, { error: "not_found", message: "not found" })
      );

      const value = await client.getNumberFlag("missing", "user-1", 25);
      expect(value).toBe(25);
    });
  });

  // ---------------------------------------------------------------------------
  // Bulk evaluation
  // ---------------------------------------------------------------------------

  describe("getAllFlags", () => {
    it("should return all flags and populate cache", async () => {
      const bulkResult = {
        flags: {
          "dark-mode": booleanResult,
          "theme": stringResult,
        },
      };
      mockFetch.mockResolvedValueOnce(mockResponse(200, bulkResult));

      const flags = await client.getAllFlags("user-1");

      expect(flags["dark-mode"]).toEqual(booleanResult);
      expect(flags["theme"]).toEqual(stringResult);

      // Subsequent individual calls should be cached
      const darkMode = await client.getBooleanFlag("dark-mode", "user-1");
      expect(darkMode).toBe(true);

      // Only the bulk call should have been made
      expect(mockFetch).toHaveBeenCalledOnce();
    });

    it("should return empty object on error (non-auth)", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(500, { error: "internal_error", message: "error" })
      );

      const flags = await client.getAllFlags("user-1");
      expect(flags).toEqual({});
    });

    it("should throw AuthenticationError on 401", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(401, { error: "unauthorized", message: "bad key" })
      );

      await expect(client.getAllFlags("user-1")).rejects.toThrow(AuthenticationError);
    });
  });

  // ---------------------------------------------------------------------------
  // Cache management
  // ---------------------------------------------------------------------------

  describe("cache management", () => {
    it("should invalidate a specific cached flag", async () => {
      mockFetch
        .mockResolvedValueOnce(mockResponse(200, booleanResult))
        .mockResolvedValueOnce(mockResponse(200, { ...booleanResult, value: false }));

      await client.getBooleanFlag("dark-mode", "user-1");
      client.invalidateCache("dark-mode", "user-1");
      const value = await client.getBooleanFlag("dark-mode", "user-1");

      expect(mockFetch).toHaveBeenCalledTimes(2);
      expect(value).toBe(false);
    });

    it("should clear all cache entries", async () => {
      mockFetch
        .mockResolvedValueOnce(mockResponse(200, booleanResult))
        .mockResolvedValueOnce(mockResponse(200, stringResult))
        .mockResolvedValueOnce(mockResponse(200, booleanResult))
        .mockResolvedValueOnce(mockResponse(200, stringResult));

      await client.getBooleanFlag("dark-mode", "user-1");
      await client.getStringFlag("theme", "user-1");

      client.clearCache();

      await client.getBooleanFlag("dark-mode", "user-1");
      await client.getStringFlag("theme", "user-1");

      expect(mockFetch).toHaveBeenCalledTimes(4);
    });
  });
});
