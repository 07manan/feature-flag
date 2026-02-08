package com.github._manan.featureflags.controller;

import com.github._manan.featureflags.dto.FlagValueDto;
import com.github._manan.featureflags.service.FlagValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flags/{flagId}/values")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FlagValueController {

    private final FlagValueService flagValueService;

    @GetMapping
    public ResponseEntity<List<FlagValueDto>> getAllByFlagId(@PathVariable UUID flagId) {
        return ResponseEntity.ok(flagValueService.getAllByFlagId(flagId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlagValueDto> getById(
            @PathVariable UUID flagId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(flagValueService.getById(flagId, id));
    }

    @PostMapping
    public ResponseEntity<FlagValueDto> create(
            @PathVariable UUID flagId,
            @Valid @RequestBody FlagValueDto request) {
        FlagValueDto created = flagValueService.create(flagId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlagValueDto> update(
            @PathVariable UUID flagId,
            @PathVariable UUID id,
            @Valid @RequestBody FlagValueDto request) {
        return ResponseEntity.ok(flagValueService.update(flagId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID flagId,
            @PathVariable UUID id) {
        flagValueService.delete(flagId, id);
        return ResponseEntity.noContent().build();
    }
}
