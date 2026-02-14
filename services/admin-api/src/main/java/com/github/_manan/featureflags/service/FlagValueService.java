package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.FlagValueDto;
import com.github._manan.featureflags.dto.FlagValueVariantDto;
import com.github._manan.featureflags.entity.*;
import com.github._manan.featureflags.event.CacheInvalidationPublisher;
import com.github._manan.featureflags.exception.ResourceNotFoundException;
import com.github._manan.featureflags.repository.EnvironmentRepository;
import com.github._manan.featureflags.repository.FlagRepository;
import com.github._manan.featureflags.repository.FlagValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlagValueService {

    private final FlagValueRepository flagValueRepository;
    private final FlagRepository flagRepository;
    private final EnvironmentRepository environmentRepository;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public List<FlagValueDto> getAll(UUID flagId, UUID environmentId, String search) {
        return flagValueRepository.findAllWithFilters(flagId, environmentId, search).stream()
                .map(FlagValueDto::from)
                .toList();
    }

    public FlagValueDto getById(UUID id) {
        FlagValue flagValue = flagValueRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlagValue", "id", id));

        return FlagValueDto.from(flagValue);
    }

    @Transactional
    public FlagValueDto create(FlagValueDto request) {
        UUID flagId = request.getFlagId();
        Flag flag = flagRepository.findByIdAndIsActiveTrue(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", flagId));

        Environment environment = environmentRepository.findByIdAndIsActiveTrue(request.getEnvironmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", request.getEnvironmentId()));

        if (flagValueRepository.existsByFlagIdAndEnvironmentIdAndIsActiveTrue(flagId, request.getEnvironmentId())) {
            throw new IllegalArgumentException(
                    "Flag value already exists for flag '" + flag.getKey() 
                    + "' in environment '" + environment.getKey() + "'");
        }

        validateVariantValues(flag.getType(), request.getVariants());

        List<FlagValueVariant> variants = new ArrayList<>(request.getVariants().stream()
                .map(FlagValueVariantDto::toEntity)
                .toList());

        FlagValue flagValue = FlagValue.builder()
                .flag(flag)
                .environment(environment)
                .variants(variants)
                .isActive(true)
                .build();

        FlagValue saved = flagValueRepository.save(flagValue);
        cacheInvalidationPublisher.publishFlagValueCreated(
                environment.getKey(), environment.getId(), flag.getKey());
        return FlagValueDto.from(saved);
    }

    @Transactional
    public FlagValueDto update(UUID id, FlagValueDto request) {
        FlagValue flagValue = flagValueRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlagValue", "id", id));

        if (!flagValue.getFlag().getId().equals(request.getFlagId())) {
            throw new IllegalArgumentException("Flag ID cannot be changed");
        }

        validateVariantValues(flagValue.getFlag().getType(), request.getVariants());

        List<FlagValueVariant> variants = request.getVariants().stream()
                .map(FlagValueVariantDto::toEntity)
                .toList();

        flagValue.getVariants().clear();
        flagValue.getVariants().addAll(variants);

        FlagValue saved = flagValueRepository.save(flagValue);
        cacheInvalidationPublisher.publishFlagValueUpdated(
                flagValue.getEnvironment().getKey(),
                flagValue.getEnvironment().getId(),
                flagValue.getFlag().getKey());
        return FlagValueDto.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        FlagValue flagValue = flagValueRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlagValue", "id", id));

        flagValue.setIsActive(false);
        flagValueRepository.save(flagValue);
        cacheInvalidationPublisher.publishFlagValueDeleted(
                flagValue.getEnvironment().getKey(),
                flagValue.getEnvironment().getId(),
                flagValue.getFlag().getKey());
    }

    private void validateVariantValues(FlagType type, List<FlagValueVariantDto> variants) {
        for (int i = 0; i < variants.size(); i++) {
            FlagValueVariantDto variant = variants.get(i);
            String value = variant.getValue();

            switch (type) {
                case BOOLEAN:
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException(
                                "Variant at index " + i + " has invalid BOOLEAN value: '" + value 
                                + "'. Must be 'true' or 'false'");
                    }
                    break;
                case NUMBER:
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Variant at index " + i + " has invalid NUMBER value: '" + value 
                                + "'. Must be a valid number");
                    }
                    break;
                case STRING:
                    break;
            }
        }
    }
}
