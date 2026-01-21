package com.scorm.generator.controller;

import com.scorm.generator.dto.ScormPackageCreateRequest;
import com.scorm.generator.dto.ScormPackageResponse;
import com.scorm.generator.service.ScormPackageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScormPackageController {

    private final ScormPackageService scormPackageService;

    public ScormPackageController(ScormPackageService scormPackageService) {
        this.scormPackageService = scormPackageService;
    }

    @PostMapping("/courses/{courseId}/scorm-packages")
    public ResponseEntity<ScormPackageResponse> create(
            @PathVariable Long courseId,
            @RequestBody ScormPackageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(scormPackageService.createPackage(courseId, request, authentication));
    }

    @GetMapping("/scorm-packages")
    public ResponseEntity<List<ScormPackageResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(scormPackageService.listPackages(authentication));
    }

    @GetMapping("/scorm-packages/{packageId}")
    public ResponseEntity<ScormPackageResponse> getById(
            @PathVariable Long packageId,
            Authentication authentication) {
        return ResponseEntity.ok(scormPackageService.getPackageById(packageId, authentication));
    }

    @GetMapping("/scorm-packages/{packageId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long packageId,
            Authentication authentication) {
        return scormPackageService.downloadPackage(packageId, authentication);
    }

    @DeleteMapping("/scorm-packages/{packageId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long packageId,
            Authentication authentication) {
        scormPackageService.deletePackage(packageId, authentication);
        return ResponseEntity.ok().build();
    }
}

