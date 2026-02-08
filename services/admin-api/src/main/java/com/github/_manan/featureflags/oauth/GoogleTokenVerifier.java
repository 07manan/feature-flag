package com.github._manan.featureflags.oauth;

import com.github._manan.featureflags.dto.OAuthUserInfo;
import com.github._manan.featureflags.entity.AuthProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleTokenVerifier implements OAuthTokenVerifier {

    @Value("${oauth.google.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String token) throws OAuthVerificationException {
        try {
            GoogleIdToken idToken = verifier.verify(token);
            
            if (idToken == null) {
                throw new OAuthVerificationException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new OAuthVerificationException("Email not found in Google token");
            }

            if (!payload.getEmailVerified()) {
                throw new OAuthVerificationException("Google email is not verified");
            }

            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String providerId = payload.getSubject();

            if (firstName == null || firstName.isBlank()) {
                String fullName = (String) payload.get("name");
                if (fullName != null && !fullName.isBlank()) {
                    return OAuthUserInfo.fromFullName(email, fullName, providerId);
                }
                firstName = email.split("@")[0];
                lastName = "";
            }

            if (lastName == null) {
                lastName = "";
            }

            return new OAuthUserInfo(email, firstName, lastName, providerId);

        } catch (OAuthVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthVerificationException("Failed to verify Google token: " + e.getMessage(), e);
        }
    }
}
