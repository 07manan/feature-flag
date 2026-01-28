package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.EnvironmentDto;
import com.github._manan.featureflags.entity.Environment;
import com.github._manan.featureflags.exception.ResourceNotFoundException;
import com.github._manan.featureflags.repository.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    public List<EnvironmentDto> getAll(String search) {
        List<Environment> environments;

        if (search != null && !search.isBlank()) {
            environments = environmentRepository
                    .findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCase(
                            search, search);
        } else {
            environments = environmentRepository.findAllByIsActiveTrue();
        }

        return environments.stream()
                .map(EnvironmentDto::from)
                .toList();
    }

    public EnvironmentDto getById(UUID id) {
        Environment environment = environmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));
        return EnvironmentDto.from(environment);
    }

    @Transactional
    public EnvironmentDto create(EnvironmentDto request) {
        if (environmentRepository.existsByKeyAndIsActiveTrue(request.getKey())) {
            throw new IllegalArgumentException("Environment with key '" + request.getKey() + "' already exists");
        }

        Environment environment = Environment.builder()
                .key(request.getKey())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        environment = environmentRepository.save(environment);
        return EnvironmentDto.from(environment);
    }

    @Transactional
    public EnvironmentDto update(UUID id, EnvironmentDto request) {
        Environment environment = environmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));

        // Key is not updatable - only update name and description
        if (request.getName() != null) {
            environment.setName(request.getName());
        }
        if (request.getDescription() != null) {
            environment.setDescription(request.getDescription());
        }

        environment = environmentRepository.save(environment);
        return EnvironmentDto.from(environment);
    }

    @Transactional
    public void delete(UUID id) {
        Environment environment = environmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));

        // Soft delete - set isActive to false
        environment.setIsActive(false);
        environmentRepository.save(environment);
    }
}
