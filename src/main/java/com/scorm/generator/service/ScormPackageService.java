package com.scorm.generator.service;

import com.scorm.generator.dto.ScormPackageCreateRequest;
import com.scorm.generator.dto.ScormPackageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ScormPackageService {
    ScormPackageResponse createPackage(Long courseId, ScormPackageCreateRequest request, Authentication authentication);

    List<ScormPackageResponse> listPackages(Authentication authentication);

    ScormPackageResponse getPackageById(Long packageId, Authentication authentication);

    ResponseEntity<Resource> downloadPackage(Long packageId, Authentication authentication);

    void deletePackage(Long packageId, Authentication authentication);
}

