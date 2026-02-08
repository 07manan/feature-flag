package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.AuthResponse;
import com.github._manan.featureflags.dto.OAuthUserInfo;
import com.github._manan.featureflags.entity.AuthProvider;
import com.github._manan.featureflags.entity.Role;
import com.github._manan.featureflags.entity.User;
import com.github._manan.featureflags.entity.UserProvider;
import com.github._manan.featureflags.oauth.OAuthTokenVerifier;
import com.github._manan.featureflags.oauth.OAuthVerificationException;
import com.github._manan.featureflags.repository.UserProviderRepository;
import com.github._manan.featureflags.repository.UserRepository;
import com.github._manan.featureflags.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final JwtUtil jwtUtil;
    private final List<OAuthTokenVerifier> tokenVerifiers;

    private Map<AuthProvider, OAuthTokenVerifier> verifierMap;

    private Map<AuthProvider, OAuthTokenVerifier> getVerifierMap() {
        if (verifierMap == null) {
            verifierMap = tokenVerifiers.stream()
                    .collect(Collectors.toMap(OAuthTokenVerifier::getProvider, Function.identity()));
        }
        return verifierMap;
    }

    @Transactional
    public AuthResponse authenticate(AuthProvider provider, String token) {
        OAuthTokenVerifier verifier = getVerifierMap().get(provider);
        
        if (verifier == null) {
            throw new OAuthVerificationException("Unsupported OAuth provider: " + provider);
        }

        OAuthUserInfo userInfo = verifier.verify(token);
        User user = findOrCreateUser(userInfo, provider);
        String jwtToken = jwtUtil.generateToken(user);

        return AuthResponse.of(jwtToken, user);
    }

    private User findOrCreateUser(OAuthUserInfo userInfo, AuthProvider provider) {
        Optional<UserProvider> existingProvider = userProviderRepository
                .findByProviderAndProviderId(provider, userInfo.providerId());

        if (existingProvider.isPresent()) {
            log.debug("Found existing user linked to {} provider", provider);
            return existingProvider.get().getUser();
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.debug("Merging {} provider with existing user: {}", provider, user.getEmail());
        } else {
            user = createNewUser(userInfo);
            log.debug("Created new user from {} provider: {}", provider, user.getEmail());
        }

        linkProvider(user, provider, userInfo.providerId());

        return user;
    }

    private User createNewUser(OAuthUserInfo userInfo) {
        boolean isFirstUser = userRepository.count() == 0;
        Role assignedRole = isFirstUser ? Role.ADMIN : Role.GUEST;

        User user = User.builder()
                .email(userInfo.email())
                .password(null)
                .firstName(userInfo.firstName())
                .lastName(userInfo.lastName().isEmpty() ? userInfo.firstName() : userInfo.lastName())
                .role(assignedRole)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    private void linkProvider(User user, AuthProvider provider, String providerId) {
        UserProvider userProvider = UserProvider.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .build();

        userProviderRepository.save(userProvider);
        log.debug("Linked {} provider to user: {}", provider, user.getEmail());
    }
}
