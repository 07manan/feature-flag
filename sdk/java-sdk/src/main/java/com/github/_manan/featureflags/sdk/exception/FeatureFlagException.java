package com.github._manan.featureflags.sdk.exception;

public class FeatureFlagException extends RuntimeException {
    
    public FeatureFlagException(String message) {
        super(message);
    }

    public FeatureFlagException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeatureFlagException(Throwable cause) {
        super(cause);
    }
}
