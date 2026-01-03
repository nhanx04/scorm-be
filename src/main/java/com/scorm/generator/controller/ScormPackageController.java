package com.scorm.generator.controller;

import com.scorm.generator.dto.CreateScormPackageRequest;
import com.scorm.generator.dto.ScormPackageDTO;
import com.scorm.generator.security.UserPrincipal;
import com.scorm.generator.service.ScormPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/scorm-packages")
@RequiredArgsConstructor
// CORS is configured globally in CorsConfig
// @CrossOrigin removed to avoid origins="*" + allowCredentials(true) conflict
public class ScormPackageController {

    private final ScormPackageService scormPackageService;

    @PostMapping
    public ResponseEntity<ScormPackageDTO> createScormPackage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreateScormPackageRequest request) {
        try {
            ScormPackageDTO response = scormPackageService.createScormPackage(userPrincipal.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ScormPackageDTO>> getUserScormPackages(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<ScormPackageDTO> packages = scormPackageService.getUserScormPackages(userPrincipal.getId());
            return ResponseEntity.ok(packages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<ScormPackageDTO> getScormPackageById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long packageId) {
        try {
            ScormPackageDTO packageDTO = scormPackageService.getScormPackageById(userPrincipal.getId(), packageId);
            return ResponseEntity.ok(packageDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> deleteScormPackage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long packageId) {
        try {
            scormPackageService.deleteScormPackage(userPrincipal.getId(), packageId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
