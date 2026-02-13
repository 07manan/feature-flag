import type { EvaluationResult, BulkEvaluationResult } from "./types.js";
import {
  FeatureFlagError,
  AuthenticationError,
  FlagNotFoundError,
} from "./errors.js";

export class HttpClient {
  private readonly baseUrl: string;
  private readonly apiKey: string;
  private readonly requestTimeout: number;

  constructor(baseUrl: string, apiKey: string, requestTimeout: number) {
    this.baseUrl = baseUrl.replace(/\/+$/, "");
    this.apiKey = apiKey;
    this.requestTimeout = requestTimeout;
  }

  async evaluateFlag(
    flagKey: string,
    userId?: string
  ): Promise<EvaluationResult> {
    const url = this.buildUrl(`/evaluate/${encodeURIComponent(flagKey)}`, userId);
    const response = await this.request(url);
    return response as EvaluationResult;
  }

  async evaluateAllFlags(userId?: string): Promise<BulkEvaluationResult> {
    const url = this.buildUrl("/evaluate", userId);
    const response = await this.request(url);
    return response as BulkEvaluationResult;
  }

  private async request(url: string): Promise<unknown> {
    let response: Response;

    try {
      response = await fetch(url, {
        method: "GET",
        headers: {
          "X-API-Key": this.apiKey,
          Accept: "application/json",
        },
        signal: AbortSignal.timeout(this.requestTimeout),
      });
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === "TimeoutError") {
        throw new FeatureFlagError(
          `Request timed out after ${this.requestTimeout}ms`,
          "timeout"
        );
      }
      throw new FeatureFlagError(
        `Network error: ${error instanceof Error ? error.message : String(error)}`,
        "network_error"
      );
    }

    if (response.ok) {
      return response.json();
    }

    let errorMessage: string;
    try {
      const body = (await response.json()) as { error?: string; message?: string };
      errorMessage = body.message ?? body.error ?? response.statusText;
    } catch {
      errorMessage = response.statusText;
    }

    switch (response.status) {
      case 401:
        throw new AuthenticationError(errorMessage);
      case 404:
        throw new FlagNotFoundError(errorMessage);
      default:
        throw new FeatureFlagError(
          `API error (${response.status}): ${errorMessage}`,
          "internal_error"
        );
    }
  }

  private buildUrl(path: string, userId?: string): string {
    const url = new URL(path, this.baseUrl);
    if (userId) {
      url.searchParams.set("user", userId);
    }
    return url.toString();
  }
}
