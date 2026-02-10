package com.github._manan.featureflags.controller;

import com.github._manan.featureflags.dto.EnvironmentDto;
import com.github._manan.featureflags.service.EnvironmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/environments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @GetMapping
    public ResponseEntity<List<EnvironmentDto>> getAll(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(environmentService.getAll(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(environmentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EnvironmentDto> create(@Valid @RequestBody EnvironmentDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(environmentService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EnvironmentDto> update(
            @PathVariable UUID id,
            @RequestBody EnvironmentDto request) {
        return ResponseEntity.ok(environmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        environmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/api-key")
    public ResponseEntity<EnvironmentDto> regenerateApiKey(@PathVariable UUID id) {
        return ResponseEntity.ok(environmentService.regenerateApiKey(id));
    }
}
