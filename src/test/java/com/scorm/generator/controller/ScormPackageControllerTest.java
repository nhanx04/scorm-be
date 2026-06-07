package com.scorm.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.ScormPackageCreateRequest;
import com.scorm.generator.dto.ScormPackageResponse;
import com.scorm.generator.service.ScormPackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScormPackageController.class)
class ScormPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScormPackageService scormPackageService;

    private ScormPackageCreateRequest createRequest;
    private ScormPackageResponse packageResponse;

    @BeforeEach
    void setUp() {
        createRequest = new ScormPackageCreateRequest();
        packageResponse = null;
    }

    @Test
    @WithMockUser
    void shouldCreateScormPackageSuccessfully() throws Exception {
        when(scormPackageService.createPackage(anyLong(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/courses/1/scorm-packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        verify(scormPackageService).createPackage(eq(1L), any(), any());
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenCreatePackageWithEmptyCourse() throws Exception {
        when(scormPackageService.createPackage(anyLong(), any(), any()))
                .thenThrow(new IllegalArgumentException("Course content is empty"));

        mockMvc.perform(post("/courses/1/scorm-packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        when(scormPackageService.createPackage(anyLong(), any(), any()))
                .thenThrow(new IllegalArgumentException("Course not found"));

        mockMvc.perform(post("/courses/999/scorm-packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatePackageWithoutJwt() throws Exception {
        mockMvc.perform(post("/courses/1/scorm-packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldListScormPackagesSuccessfully() throws Exception {
        List<ScormPackageResponse> packages = Arrays.asList(packageResponse);
        when(scormPackageService.listPackages(any())).thenReturn(packages);

        mockMvc.perform(get("/scorm-packages"))
                .andExpect(status().isOk());

        verify(scormPackageService).listPackages(any());
    }

    @Test
    void shouldReturnUnauthorizedWhenListPackagesWithoutJwt() throws Exception {
        mockMvc.perform(get("/scorm-packages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldGetScormPackageByIdSuccessfully() throws Exception {
        when(scormPackageService.getPackageById(anyLong(), any())).thenReturn(packageResponse);

        mockMvc.perform(get("/scorm-packages/1"))
                .andExpect(status().isOk());

        verify(scormPackageService).getPackageById(1L, any());
    }

    @Test
    void shouldReturnUnauthorizedWhenGetPackageWithoutJwt() throws Exception {
        mockMvc.perform(get("/scorm-packages/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldDownloadScormPackageSuccessfully() throws Exception {
        when(scormPackageService.downloadPackage(anyLong(), any())).thenReturn(null);

        mockMvc.perform(get("/scorm-packages/1/download"))
                .andExpect(status().isOk());

        verify(scormPackageService).downloadPackage(1L, any());
    }

    @Test
    void shouldReturnUnauthorizedWhenDownloadPackageWithoutJwt() throws Exception {
        mockMvc.perform(get("/scorm-packages/1/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldDeleteScormPackageSuccessfully() throws Exception {
        doNothing().when(scormPackageService).deletePackage(anyLong(), any());

        mockMvc.perform(delete("/scorm-packages/1"))
                .andExpect(status().isOk());

        verify(scormPackageService).deletePackage(1L, any());
    }

    @Test
    void shouldReturnUnauthorizedWhenDeletePackageWithoutJwt() throws Exception {
        mockMvc.perform(delete("/scorm-packages/1"))
                .andExpect(status().isUnauthorized());
    }
}
