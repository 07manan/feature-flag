package com.github._manan.featureflags.oauth;

public class OAuthVerificationException extends RuntimeException {

    public OAuthVerificationException(String message) {
        super(message);
    }

    public OAuthVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
