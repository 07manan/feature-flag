package com.github._manan.featureflags.controller;

import com.github._manan.featureflags.dto.FlagDto;
import com.github._manan.featureflags.service.FlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing feature flag definitions.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FlagController {

    private final FlagService flagService;

    /**
     * List all active flags, optionally filtered by search query.
     *
     * @param search optional search term for key, name, or description
     * @return list of flags
     */
    @GetMapping
    public ResponseEntity<List<FlagDto>> getAllFlags(
            @RequestParam(required = false) String search) {
        List<FlagDto> flags = flagService.getAllFlags(search);
        return ResponseEntity.ok(flags);
    }

    /**
     * Get a flag by ID.
     *
     * @param id the flag ID
     * @return the flag
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlagDto> getFlagById(@PathVariable UUID id) {
        FlagDto flag = flagService.getFlagById(id);
        return ResponseEntity.ok(flag);
    }

    /**
     * Create a new flag.
     *
     * @param request the flag creation request
     * @return the created flag
     */
    @PostMapping
    public ResponseEntity<FlagDto> createFlag(@Valid @RequestBody FlagDto request) {
        FlagDto createdFlag = flagService.createFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFlag);
    }

    /**
     * Update an existing flag.
     * Note: The key and type fields are immutable and will be ignored.
     *
     * @param id the flag ID
     * @param request the update request
     * @return the updated flag
     */
    @PatchMapping("/{id}")
    public ResponseEntity<FlagDto> updateFlag(
            @PathVariable UUID id,
            @RequestBody FlagDto request) {
        FlagDto updatedFlag = flagService.updateFlag(id, request);
        return ResponseEntity.ok(updatedFlag);
    }

    /**
     * Delete a flag (soft delete).
     *
     * @param id the flag ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID id) {
        flagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }
}
