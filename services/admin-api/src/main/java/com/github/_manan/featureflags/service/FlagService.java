package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.FlagDto;
import com.github._manan.featureflags.entity.Flag;
import com.github._manan.featureflags.entity.FlagType;
import com.github._manan.featureflags.exception.ResourceNotFoundException;
import com.github._manan.featureflags.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing feature flag definitions.
 */
@Service
@RequiredArgsConstructor
public class FlagService {

    private final FlagRepository flagRepository;

    /**
     * Get all active flags, optionally filtered by search query.
     *
     * @param search optional search term for key, name, or description
     * @return list of matching flags
     */
    public List<FlagDto> getAllFlags(String search) {
        List<Flag> flags;
        if (search != null && !search.isBlank()) {
            flags = flagRepository.findAllByIsActiveTrueAndKeyContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCase(
                    search, search, search);
        } else {
            flags = flagRepository.findAllByIsActiveTrue();
        }
        return flags.stream()
                .map(FlagDto::from)
                .toList();
    }

    /**
     * Get a flag by ID.
     *
     * @param id the flag ID
     * @return the flag DTO
     * @throws ResourceNotFoundException if flag not found
     */
    public FlagDto getFlagById(UUID id) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));
        return FlagDto.from(flag);
    }

    /**
     * Create a new flag.
     *
     * @param request the flag creation request
     * @return the created flag DTO
     * @throws IllegalArgumentException if key already exists or default value is invalid
     */
    @Transactional
    public FlagDto createFlag(FlagDto request) {
        // Validate key uniqueness
        if (flagRepository.existsByKeyAndIsActiveTrue(request.getKey())) {
            throw new IllegalArgumentException("Flag with key '" + request.getKey() + "' already exists");
        }

        // Validate default value against type
        validateDefaultValue(request.getType(), request.getDefaultValue());

        Flag flag = Flag.builder()
                .key(request.getKey())
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .defaultValue(request.getDefaultValue())
                .isActive(true)
                .build();

        Flag savedFlag = flagRepository.save(flag);
        return FlagDto.from(savedFlag);
    }

    /**
     * Update an existing flag.
     * Note: The key and type fields are immutable and will be ignored if provided.
     *
     * @param id the flag ID
     * @param request the update request
     * @return the updated flag DTO
     * @throws ResourceNotFoundException if flag not found
     * @throws IllegalArgumentException if default value is invalid for the flag's type
     */
    @Transactional
    public FlagDto updateFlag(UUID id, FlagDto request) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));

        // Key is immutable - do not update
        // Type is immutable - do not update

        if (request.getName() != null) {
            flag.setName(request.getName());
        }

        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }

        if (request.getDefaultValue() != null) {
            // Validate default value against the flag's existing type
            validateDefaultValue(flag.getType(), request.getDefaultValue());
            flag.setDefaultValue(request.getDefaultValue());
        }

        Flag savedFlag = flagRepository.save(flag);
        return FlagDto.from(savedFlag);
    }

    /**
     * Delete a flag (soft delete).
     *
     * @param id the flag ID
     * @throws ResourceNotFoundException if flag not found
     */
    @Transactional
    public void deleteFlag(UUID id) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));

        flag.setIsActive(false);
        flagRepository.save(flag);
    }

    /**
     * Validates that the default value is compatible with the specified flag type.
     *
     * @param type the flag type
     * @param defaultValue the value to validate
     * @throws IllegalArgumentException if the value is not valid for the type
     */
    private void validateDefaultValue(FlagType type, String defaultValue) {
        if (defaultValue == null || defaultValue.isBlank()) {
            throw new IllegalArgumentException("Default value is required");
        }

        switch (type) {
            case BOOLEAN:
                if (!defaultValue.equalsIgnoreCase("true") && !defaultValue.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException(
                            "Default value for BOOLEAN type must be 'true' or 'false', got: '" + defaultValue + "'");
                }
                break;
            case NUMBER:
                try {
                    Double.parseDouble(defaultValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Default value for NUMBER type must be a valid number, got: '" + defaultValue + "'");
                }
                break;
            case STRING:
                // Any string is valid for STRING type
                break;
            default:
                throw new IllegalArgumentException("Unknown flag type: " + type);
        }
    }
}
