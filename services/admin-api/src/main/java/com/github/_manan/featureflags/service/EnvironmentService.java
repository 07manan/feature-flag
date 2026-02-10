package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.EnvironmentDto;
import com.github._manan.featureflags.entity.Environment;
import com.github._manan.featureflags.event.CacheInvalidationPublisher;
import com.github._manan.featureflags.exception.ResourceNotFoundException;
import com.github._manan.featureflags.repository.EnvironmentRepository;
import com.github._manan.featureflags.repository.FlagValueRepository;
import com.github._manan.featureflags.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final FlagValueRepository flagValueRepository;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

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
                .apiKey(generateUniqueApiKey(request.getKey()))
                .build();

        environment = environmentRepository.save(environment);
        return EnvironmentDto.from(environment);
    }

    @Transactional
    public EnvironmentDto update(UUID id, EnvironmentDto request) {
        Environment environment = environmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));

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

        flagValueRepository.deactivateByEnvironmentId(id);
        
        environment.setIsActive(false);
        environmentRepository.save(environment);
        cacheInvalidationPublisher.publishEnvironmentDeleted(environment.getKey(), environment.getId());
    }

    @Transactional
    public EnvironmentDto regenerateApiKey(UUID id) {
        Environment environment = environmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));

        environment.setApiKey(generateUniqueApiKey(environment.getKey()));
        environment = environmentRepository.save(environment);
        cacheInvalidationPublisher.publishEnvironmentApiKeyRegenerated(environment.getKey(), environment.getId());
        return EnvironmentDto.from(environment);
    }

    private String generateUniqueApiKey(String environmentKey) {
        String apiKey;
        do {
            apiKey = ApiKeyGenerator.generateApiKey(environmentKey);
        } while (environmentRepository.existsByApiKey(apiKey));
        return apiKey;
    }
}
