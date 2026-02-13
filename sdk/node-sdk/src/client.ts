import type { EvaluationResult, FeatureFlagClientOptions } from "./types.js";
import { FlagType } from "./types.js";
import { AuthenticationError, FeatureFlagError, FlagNotFoundError } from "./errors.js";
import { LocalCache } from "./cache.js";
import { HttpClient } from "./http-client.js";

const DEFAULT_BASE_URL = "https://strong-lorena-07manan-b3c1d402.koyeb.app";
const DEFAULT_CACHE_TTL_MS = 30_000;
const DEFAULT_REQUEST_TIMEOUT_MS = 10_000;

/**
 * @example
 * ```ts
 * const client = new FeatureFlagClient({ apiKey: "ff_production_abc123" });
 *
 * const enabled = await client.getBooleanFlag("dark-mode", "user-42", false);
 * console.log("Dark mode:", enabled);
 *
 * client.close();
 * ```
 */
export class FeatureFlagClient {
  private readonly httpClient: HttpClient;
  private readonly cache: LocalCache<EvaluationResult>;

  constructor(options: FeatureFlagClientOptions) {
    this.validateApiKey(options.apiKey);

    const baseUrl = this.resolveBaseUrl(options.baseUrl);
    const cacheTTL = options.cacheTTL ?? DEFAULT_CACHE_TTL_MS;
    const requestTimeout = options.requestTimeout ?? DEFAULT_REQUEST_TIMEOUT_MS;

    this.httpClient = new HttpClient(baseUrl, options.apiKey, requestTimeout);
    this.cache = new LocalCache<EvaluationResult>(cacheTTL);
  }

  /**
   * Returns `defaultValue` if the flag is not found or type doesn't match.
   * Throws {@link AuthenticationError} if the API key is invalid.
   */
  async getBooleanFlag(
    flagKey: string,
    userId?: string,
    defaultValue: boolean = false
  ): Promise<boolean> {
    const result = await this.getFlag(flagKey, userId);
    if (!result) return defaultValue;

    if (result.type !== FlagType.BOOLEAN) {
      return defaultValue;
    }

    return typeof result.value === "boolean" ? result.value : defaultValue;
  }

  /** @see getBooleanFlag */
  async getStringFlag(
    flagKey: string,
    userId?: string,
    defaultValue: string = ""
  ): Promise<string> {
    const result = await this.getFlag(flagKey, userId);
    if (!result) return defaultValue;

    if (result.type !== FlagType.STRING) {
      return defaultValue;
    }

    return typeof result.value === "string" ? result.value : defaultValue;
  }

  /** @see getBooleanFlag */
  async getNumberFlag(
    flagKey: string,
    userId?: string,
    defaultValue: number = 0
  ): Promise<number> {
    const result = await this.getFlag(flagKey, userId);
    if (!result) return defaultValue;

    if (result.type !== FlagType.NUMBER) {
      return defaultValue;
    }

    return typeof result.value === "number" ? result.value : defaultValue;
  }

  /** Evaluates all flags and populates individual cache entries. */
  async getAllFlags(
    userId?: string
  ): Promise<Record<string, EvaluationResult>> {
    try {
      const bulk = await this.httpClient.evaluateAllFlags(userId);

      for (const [flagKey, result] of Object.entries(bulk.flags)) {
        const cacheKey = LocalCache.buildKey(flagKey, userId);
        this.cache.set(cacheKey, result);
      }

      return bulk.flags;
    } catch (error) {
      if (error instanceof AuthenticationError) {
        throw error;
      }
      return {};
    }
  }

  invalidateCache(flagKey: string, userId?: string): void {
    const cacheKey = LocalCache.buildKey(flagKey, userId);
    this.cache.delete(cacheKey);
  }

  clearCache(): void {
    this.cache.clear();
  }

  /** Shuts down the client. Do not use the client after calling this. */
  close(): void {
    this.cache.shutdown();
  }

  /** Cache-first evaluation. Returns null on non-auth errors (caller returns default). */
  private async getFlag(
    flagKey: string,
    userId?: string
  ): Promise<EvaluationResult | null> {
    const cacheKey = LocalCache.buildKey(flagKey, userId);

    const cached = this.cache.get(cacheKey);
    if (cached) return cached;

    try {
      const result = await this.httpClient.evaluateFlag(flagKey, userId);
      this.cache.set(cacheKey, result);
      return result;
    } catch (error) {
      if (error instanceof AuthenticationError) {
        throw error;
      }
      return null;
    }
  }

  private validateApiKey(apiKey: string): void {
    if (!apiKey || !apiKey.startsWith("ff_")) {
      throw new FeatureFlagError(
        'Invalid API key: must start with "ff_"',
        "invalid_api_key"
      );
    }
  }

  /** Priority: explicit option > FEATUREFLAGS_BASE_URL env var > default URL. */
  private resolveBaseUrl(explicitUrl?: string): string {
    if (explicitUrl) return explicitUrl;

    const envUrl = process.env.FEATUREFLAGS_BASE_URL;
    if (envUrl) return envUrl;

    return DEFAULT_BASE_URL;
  }
}
