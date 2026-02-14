package com.github._manan.featureflags.config;

import com.github._manan.featureflags.dto.OAuthUserInfo;
import com.github._manan.featureflags.entity.AuthProvider;
import com.github._manan.featureflags.oauth.OAuthTokenVerifier;
import com.github._manan.featureflags.oauth.OAuthVerificationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Provides a controllable mock {@link OAuthTokenVerifier} for Google,
 * allowing integration tests to simulate OAuth flows without hitting
 * external APIs.
 *
 * <p>Usage in tests:
 * <pre>
 *   &#64;Autowired TestOAuthConfig.MockGoogleVerifier mockGoogleVerifier;
 *
 *   mockGoogleVerifier.setHandler(token -> new OAuthUserInfo(...));
 *   // or
 *   mockGoogleVerifier.setHandler(token -> { throw new OAuthVerificationException("..."); });
 * </pre>
 */
@TestConfiguration
public class TestOAuthConfig {

    @Bean("googleTokenVerifier")
    @Primary
    public MockGoogleVerifier mockGoogleTokenVerifier() {
        return new MockGoogleVerifier();
    }

    public static class MockGoogleVerifier implements OAuthTokenVerifier {

        private final AtomicReference<Function<String, OAuthUserInfo>> handler =
                new AtomicReference<>(token -> {
                    throw new OAuthVerificationException("No mock handler configured");
                });

        public void setHandler(Function<String, OAuthUserInfo> verifyHandler) {
            this.handler.set(verifyHandler);
        }

        public void reset() {
            this.handler.set(token -> {
                throw new OAuthVerificationException("No mock handler configured");
            });
        }

        @Override
        public AuthProvider getProvider() {
            return AuthProvider.GOOGLE;
        }

        @Override
        public OAuthUserInfo verify(String token) throws OAuthVerificationException {
            try {
                return handler.get().apply(token);
            } catch (OAuthVerificationException e) {
                throw e;
            } catch (Exception e) {
                throw new OAuthVerificationException(e.getMessage());
            }
        }
    }
}
