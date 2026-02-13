package io.github._07manan.featureflags.sdk.exception;

public class FlagNotFoundException extends FeatureFlagException {
    
    private final String flagKey;

    public FlagNotFoundException(String flagKey) {
        super("Flag not found: " + flagKey);
        this.flagKey = flagKey;
    }

    public FlagNotFoundException(String flagKey, String message) {
        super(message);
        this.flagKey = flagKey;
    }

    public String getFlagKey() {
        return flagKey;
    }
}
