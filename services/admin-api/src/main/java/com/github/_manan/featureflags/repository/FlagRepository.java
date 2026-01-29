package com.github._manan.featureflags.repository;

import com.github._manan.featureflags.entity.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Flag entity operations.
 * All queries filter by isActive=true to support soft delete.
 */
@Repository
public interface FlagRepository extends JpaRepository<Flag, UUID> {

    /**
     * Find an active flag by its ID.
     */
    Optional<Flag> findByIdAndIsActiveTrue(UUID id);

    /**
     * Find all active flags.
     */
    List<Flag> findAllByIsActiveTrue();

    /**
     * Check if an active flag exists with the given key.
     */
    boolean existsByKeyAndIsActiveTrue(String key);

    /**
     * Search active flags by key, name, or description (case-insensitive).
     */
    List<Flag> findAllByIsActiveTrueAndKeyContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCase(
            String key, String name, String description);
}
