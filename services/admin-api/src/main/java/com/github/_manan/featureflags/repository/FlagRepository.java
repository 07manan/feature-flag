package com.github._manan.featureflags.repository;

import com.github._manan.featureflags.entity.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlagRepository extends JpaRepository<Flag, UUID> {

    Optional<Flag> findByIdAndIsActiveTrue(UUID id);

    List<Flag> findAllByIsActiveTrue();

    boolean existsByKeyAndIsActiveTrue(String key);

    List<Flag> findAllByIsActiveTrueAndKeyContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCase(
            String key, String name, String description);
}
