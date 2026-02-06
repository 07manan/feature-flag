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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagDto {

    private UUID id;

    @NotBlank(message = "Key is required")
    @Size(min = 1, max = 100, message = "Key must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Key must contain only lowercase letters, numbers, and hyphens")
    private String key;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Type is required")
    private FlagType type;

    @NotBlank(message = "Default value is required")
    @Size(max = 500, message = "Default value must not exceed 500 characters")
    private String defaultValue;

    private Boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;

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
