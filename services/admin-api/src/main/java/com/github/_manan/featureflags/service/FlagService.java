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

@Service
@RequiredArgsConstructor
public class FlagService {

    private final FlagRepository flagRepository;

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

    public FlagDto getFlagById(UUID id) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));
        return FlagDto.from(flag);
    }

    @Transactional
    public FlagDto createFlag(FlagDto request) {
        if (flagRepository.existsByKeyAndIsActiveTrue(request.getKey())) {
            throw new IllegalArgumentException("Flag with key '" + request.getKey() + "' already exists");
        }

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

    @Transactional
    public FlagDto updateFlag(UUID id, FlagDto request) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));

        if (request.getName() != null) {
            flag.setName(request.getName());
        }

        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }

        if (request.getDefaultValue() != null) {
            validateDefaultValue(flag.getType(), request.getDefaultValue());
            flag.setDefaultValue(request.getDefaultValue());
        }

        Flag savedFlag = flagRepository.save(flag);
        return FlagDto.from(savedFlag);
    }

    @Transactional
    public void deleteFlag(UUID id) {
        Flag flag = flagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", id));

        flag.setIsActive(false);
        flagRepository.save(flag);
    }

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
