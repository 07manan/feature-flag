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

@RestController
@RequestMapping("/flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FlagController {

    private final FlagService flagService;

    @GetMapping
    public ResponseEntity<List<FlagDto>> getAllFlags(
            @RequestParam(required = false) String search) {
        List<FlagDto> flags = flagService.getAllFlags(search);
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlagDto> getFlagById(@PathVariable UUID id) {
        FlagDto flag = flagService.getFlagById(id);
        return ResponseEntity.ok(flag);
    }

    @PostMapping
    public ResponseEntity<FlagDto> createFlag(@Valid @RequestBody FlagDto request) {
        FlagDto createdFlag = flagService.createFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFlag);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FlagDto> updateFlag(
            @PathVariable UUID id,
            @RequestBody FlagDto request) {
        FlagDto updatedFlag = flagService.updateFlag(id, request);
        return ResponseEntity.ok(updatedFlag);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID id) {
        flagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }
}
