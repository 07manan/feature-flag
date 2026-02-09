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
@RequestMapping("/flag-values")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FlagValueController {

    private final FlagValueService flagValueService;

    @GetMapping
    public ResponseEntity<List<FlagValueDto>> getAll(
            @RequestParam(required = false) UUID flagId,
            @RequestParam(required = false) UUID environmentId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(flagValueService.getAll(flagId, environmentId, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlagValueDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(flagValueService.getById(id));
    }

    @PostMapping
    public ResponseEntity<FlagValueDto> create(@Valid @RequestBody FlagValueDto request) {
        FlagValueDto created = flagValueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlagValueDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody FlagValueDto request) {
        return ResponseEntity.ok(flagValueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        flagValueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
