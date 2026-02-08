package com.github._manan.featureflags.oauth;

import com.github._manan.featureflags.dto.OAuthUserInfo;
import com.github._manan.featureflags.entity.AuthProvider;

public interface OAuthTokenVerifier {

    AuthProvider getProvider();

    OAuthUserInfo verify(String token) throws OAuthVerificationException;
}
