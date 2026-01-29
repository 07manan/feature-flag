package com.github._manan.featureflags.dto;

import com.github._manan.featureflags.entity.Flag;
import com.github._manan.featureflags.entity.FlagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for Flag entity.
 * Used for both request and response payloads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagDto {

    /**
     * Unique identifier (read-only, auto-generated).
     */
    private UUID id;

    /**
     * Programmatic key for SDK usage.
     * Immutable after creation.
     */
    @NotBlank(message = "Key is required")
    @Size(min = 1, max = 100, message = "Key must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Key must contain only lowercase letters, numbers, and hyphens")
    private String key;

    /**
     * Human-readable display name.
     */
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
    private String name;

    /**
     * Optional description of the flag's purpose.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * The data type of this flag's value.
     */
    @NotNull(message = "Type is required")
    private FlagType type;

    /**
     * The default value for this flag.
     * Must be valid for the specified type.
     */
    @NotBlank(message = "Default value is required")
    @Size(max = 500, message = "Default value must not exceed 500 characters")
    private String defaultValue;

    /**
     * Whether the flag is active (read-only).
     */
    private Boolean isActive;

    /**
     * Timestamp of creation (read-only).
     */
    private Instant createdAt;

    /**
     * Timestamp of last update (read-only).
     */
    private Instant updatedAt;

    /**
     * Factory method to create a DTO from a Flag entity.
     *
     * @param flag the Flag entity
     * @return the corresponding DTO
     */
    public static FlagDto from(Flag flag) {
        return FlagDto.builder()
                .id(flag.getId())
                .key(flag.getKey())
                .name(flag.getName())
                .description(flag.getDescription())
                .type(flag.getType())
                .defaultValue(flag.getDefaultValue())
                .isActive(flag.getIsActive())
                .createdAt(flag.getCreatedAt())
                .updatedAt(flag.getUpdatedAt())
                .build();
    }
}
