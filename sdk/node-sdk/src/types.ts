export enum FlagType {
  BOOLEAN = "BOOLEAN",
  STRING = "STRING",
  NUMBER = "NUMBER",
}

export interface EvaluationResult {
  flagKey: string;
  value: boolean | string | number;
  type: FlagType;
  isDefault: boolean;
  variantId: string | null;
}

export interface BulkEvaluationResult {
  flags: Record<string, EvaluationResult>;
}

export interface FeatureFlagClientOptions {
  /** Must start with "ff_". */
  apiKey: string;

  /** Resolution order: this option > FEATUREFLAGS_BASE_URL env var > default deployed URL. */
  baseUrl?: string;

  /** In milliseconds. @default 30000 */
  cacheTTL?: number;

  /** In milliseconds. @default 10000 */
  requestTimeout?: number;
}
