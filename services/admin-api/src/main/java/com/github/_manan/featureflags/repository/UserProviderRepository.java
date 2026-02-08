package com.github._manan.featureflags.repository;

import com.github._manan.featureflags.entity.AuthProvider;
import com.github._manan.featureflags.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {

    Optional<UserProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<UserProvider> findByUserIdAndProvider(UUID userId, AuthProvider provider);

    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
}
