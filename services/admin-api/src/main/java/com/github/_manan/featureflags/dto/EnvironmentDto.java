package com.github._manan.featureflags.dto;

import com.github._manan.featureflags.entity.Environment;
import jakarta.validation.constraints.NotBlank;
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
public class EnvironmentDto {

    private UUID id;

    @NotBlank(message = "Key is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Key must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 1, max = 50, message = "Key must be between 1 and 50 characters")
    private String key;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;

    public static EnvironmentDto from(Environment environment) {
        return EnvironmentDto.builder()
                .id(environment.getId())
                .key(environment.getKey())
                .name(environment.getName())
                .description(environment.getDescription())
                .isActive(environment.getIsActive())
                .createdAt(environment.getCreatedAt())
                .updatedAt(environment.getUpdatedAt())
                .build();
    }
}
