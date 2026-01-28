package com.github._manan.featureflags.repository;

import com.github._manan.featureflags.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    boolean existsByKeyAndIsActiveTrue(String key);

    Optional<Environment> findByIdAndIsActiveTrue(UUID id);

    List<Environment> findAllByIsActiveTrue();

    List<Environment> findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCase(
            String name, String description);
}
