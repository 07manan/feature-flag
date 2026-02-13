export { FeatureFlagClient } from "./client.js";

export { FlagType } from "./types.js";
export type {
  EvaluationResult,
  BulkEvaluationResult,
  FeatureFlagClientOptions,
} from "./types.js";

export {
  FeatureFlagError,
  AuthenticationError,
  FlagNotFoundError,
} from "./errors.js";

export { LocalCache } from "./cache.js";
