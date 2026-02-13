export class FeatureFlagError extends Error {
  public readonly code: string;

  constructor(message: string, code: string = "unknown_error") {
    super(message);
    this.name = "FeatureFlagError";
    this.code = code;
  }
}

export class AuthenticationError extends FeatureFlagError {
  constructor(message: string = "Invalid or missing API key") {
    super(message, "unauthorized");
    this.name = "AuthenticationError";
  }
}

export class FlagNotFoundError extends FeatureFlagError {
  constructor(flagKey: string) {
    super(`Flag not found: ${flagKey}`, "not_found");
    this.name = "FlagNotFoundError";
  }
}
