import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { HttpClient } from "../http-client.js";
import { AuthenticationError, FlagNotFoundError, FeatureFlagError } from "../errors.js";
import { FlagType } from "../types.js";

describe("HttpClient", () => {
  let httpClient: HttpClient;
  const mockFetch = vi.fn();

  beforeEach(() => {
    httpClient = new HttpClient("http://localhost:8081", "ff_development_vs_X5jKfQ2p2RcfSCq-CAWWMqvQELxC4TCy0MSg9RqY", 5000);
    vi.stubGlobal("fetch", mockFetch);
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const mockResponse = (status: number, body: unknown): Response =>
    ({
      ok: status >= 200 && status < 300,
      status,
      statusText: status === 200 ? "OK" : "Error",
      json: () => Promise.resolve(body),
    }) as unknown as Response;

  describe("evaluateFlag", () => {
    it("should call GET /evaluate/{flagKey} with API key header", async () => {
      const result = {
        flagKey: "dark-mode",
        value: true,
        type: FlagType.BOOLEAN,
        isDefault: false,
        variantId: "v1",
      };
      mockFetch.mockResolvedValueOnce(mockResponse(200, result));

      const response = await httpClient.evaluateFlag("dark-mode", "user-1");

      expect(mockFetch).toHaveBeenCalledOnce();
      const [url, options] = mockFetch.mock.calls[0]!;
      expect(url).toBe("http://localhost:8081/evaluate/dark-mode?user=user-1");
      expect(options.headers["X-API-Key"]).toBe("ff_development_vs_X5jKfQ2p2RcfSCq-CAWWMqvQELxC4TCy0MSg9RqY");
      expect(response).toEqual(result);
    });

    it("should call without user param when userId is undefined", async () => {
      const result = {
        flagKey: "dark-mode",
        value: true,
        type: FlagType.BOOLEAN,
        isDefault: false,
        variantId: "v1",
      };
      mockFetch.mockResolvedValueOnce(mockResponse(200, result));

      await httpClient.evaluateFlag("dark-mode");

      const [url] = mockFetch.mock.calls[0]!;
      expect(url).toBe("http://localhost:8081/evaluate/dark-mode");
    });

    it("should encode special characters in flagKey", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(200, { flagKey: "my flag", value: "test", type: "STRING", isDefault: false, variantId: null })
      );

      await httpClient.evaluateFlag("my flag", "user-1");

      const [url] = mockFetch.mock.calls[0]!;
      expect(url).toContain("/evaluate/my%20flag");
    });

    it("should throw AuthenticationError on 401", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(401, { error: "unauthorized", message: "Invalid API key" })
      );

      await expect(httpClient.evaluateFlag("flag", "user")).rejects.toThrow(AuthenticationError);
    });

    it("should throw FlagNotFoundError on 404", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(404, { error: "not_found", message: "Flag not found" })
      );

      await expect(httpClient.evaluateFlag("missing-flag", "user")).rejects.toThrow(FlagNotFoundError);
    });

    it("should throw FeatureFlagError on 500", async () => {
      mockFetch.mockResolvedValueOnce(
        mockResponse(500, { error: "internal_error", message: "Something went wrong" })
      );

      await expect(httpClient.evaluateFlag("flag", "user")).rejects.toThrow(FeatureFlagError);
    });

    it("should throw FeatureFlagError on network error", async () => {
      mockFetch.mockRejectedValueOnce(new Error("ECONNREFUSED"));

      await expect(httpClient.evaluateFlag("flag", "user")).rejects.toThrow(FeatureFlagError);

      mockFetch.mockRejectedValueOnce(new Error("ECONNREFUSED"));

      await expect(httpClient.evaluateFlag("flag", "user")).rejects.toThrow(/Network error/);
    });
  });

  describe("evaluateAllFlags", () => {
    it("should call GET /evaluate with user param", async () => {
      const result = {
        flags: {
          "dark-mode": { flagKey: "dark-mode", value: true, type: FlagType.BOOLEAN, isDefault: false, variantId: "v1" },
        },
      };
      mockFetch.mockResolvedValueOnce(mockResponse(200, result));

      const response = await httpClient.evaluateAllFlags("user-1");

      const [url] = mockFetch.mock.calls[0]!;
      expect(url).toBe("http://localhost:8081/evaluate?user=user-1");
      expect(response).toEqual(result);
    });

    it("should call GET /evaluate without user param when no userId", async () => {
      mockFetch.mockResolvedValueOnce(mockResponse(200, { flags: {} }));

      await httpClient.evaluateAllFlags();

      const [url] = mockFetch.mock.calls[0]!;
      expect(url).toBe("http://localhost:8081/evaluate");
    });
  });
});
