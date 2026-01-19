package com.scorm.generator.controller;

import com.scorm.generator.dto.LibraryDetailResponse;
import com.scorm.generator.entity.MyLibrary;
import com.scorm.generator.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/libraries")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    public ResponseEntity<MyLibrary> create(@RequestBody CreateLibraryRequest request, Authentication authentication) {
        return ResponseEntity.ok(libraryService.create(
                request.libraryName,
                request.description,
                request.scopeType,
                authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibraryDetailResponse> getLibraryDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(libraryService.getLibraryDetail(id));
    }

    public static class CreateLibraryRequest {
        public String libraryName;
        public String description;
        public String scopeType;
    }
}
