package com.github._manan.featureflags.repository;

import com.github._manan.featureflags.entity.FlagValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlagValueRepository extends JpaRepository<FlagValue, UUID> {

    @EntityGraph(attributePaths = {"flag", "environment", "variants"})
    Optional<FlagValue> findByIdAndIsActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"flag", "environment", "variants"})
    List<FlagValue> findAllByFlagIdAndIsActiveTrue(UUID flagId);

    @EntityGraph(attributePaths = {"flag", "environment", "variants"})
    Optional<FlagValue> findByFlagIdAndEnvironmentIdAndIsActiveTrue(UUID flagId, UUID environmentId);

    @EntityGraph(attributePaths = {"flag", "environment", "variants"})
    List<FlagValue> findAllByEnvironmentIdAndIsActiveTrue(UUID environmentId);

    boolean existsByFlagIdAndEnvironmentIdAndIsActiveTrue(UUID flagId, UUID environmentId);

    @Modifying
    @Query("UPDATE FlagValue fv SET fv.isActive = false WHERE fv.flag.id = :flagId AND fv.isActive = true")
    int deactivateByFlagId(@Param("flagId") UUID flagId);

    @Modifying
    @Query("UPDATE FlagValue fv SET fv.isActive = false WHERE fv.environment.id = :environmentId AND fv.isActive = true")
    int deactivateByEnvironmentId(@Param("environmentId") UUID environmentId);

    @EntityGraph(attributePaths = {"flag", "environment", "variants"})
    @Query("""
        SELECT fv FROM FlagValue fv
        WHERE fv.isActive = true
        AND (:flagId IS NULL OR fv.flag.id = :flagId)
        AND (:environmentId IS NULL OR fv.environment.id = :environmentId)
        AND (:search IS NULL OR :search = ''
            OR LOWER(fv.flag.key) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(fv.flag.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(fv.environment.key) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(fv.environment.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    List<FlagValue> findAllWithFilters(
            @Param("flagId") UUID flagId,
            @Param("environmentId") UUID environmentId,
            @Param("search") String search);
}
