package io.github._07manan.featureflags.sdk.exception;

public class AuthenticationException extends FeatureFlagException {
    
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
