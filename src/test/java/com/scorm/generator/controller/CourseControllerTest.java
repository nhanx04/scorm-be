package com.scorm.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.service.CourseService;
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

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    private CourseCreateRequest createRequest;
    private CourseUpdateRequest updateRequest;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        createRequest = new CourseCreateRequest();
        createRequest.setTitle("Test Course");
        createRequest.setDescription("Test Description");

        updateRequest = new CourseUpdateRequest();
        updateRequest.setTitle("Updated Course");

        courseResponse = new CourseResponse();
        courseResponse.setTitle("Test Course");
    }

    @Test
    @WithMockUser
    void shouldGetCoursesSuccessfully() throws Exception {
        List<CourseResponse> courses = Arrays.asList(courseResponse);
        when(courseService.listMine(any())).thenReturn(courses);

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Course"));

        verify(courseService).listMine(any());
    }

    @Test
    void shouldReturnUnauthorizedWhenGetCoursesWithoutJwt() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldCreateCourseSuccessfully() throws Exception {
        when(courseService.create(any(CourseCreateRequest.class), any())).thenReturn(courseResponse);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Course"));

        verify(courseService).create(any(CourseCreateRequest.class), any());
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenCreateCourseWithInvalidData() throws Exception {
        CourseCreateRequest invalidRequest = new CourseCreateRequest();
        // Missing title

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenCreateCourseWithoutJwt() throws Exception {
        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldGetCourseByIdSuccessfully() throws Exception {
        when(courseService.getById(anyLong(), any())).thenReturn(null);

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk());

        verify(courseService).getById(1L, any());
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        when(courseService.getById(anyLong(), any())).thenThrow(new IllegalArgumentException("Course not found"));

        mockMvc.perform(get("/courses/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenGetCourseByIdWithoutJwt() throws Exception {
        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldUpdateCourseSuccessfully() throws Exception {
        when(courseService.update(anyLong(), any(CourseUpdateRequest.class), any())).thenReturn(courseResponse);

        mockMvc.perform(patch("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(courseService).update(eq(1L), any(CourseUpdateRequest.class), any());
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenUpdateNonExistentCourse() throws Exception {
        when(courseService.update(anyLong(), any(), any())).thenThrow(new IllegalArgumentException("Course not found"));

        mockMvc.perform(patch("/courses/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdateCourseWithoutJwt() throws Exception {
        mockMvc.perform(patch("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldDeleteCourseSuccessfully() throws Exception {
        doNothing().when(courseService).delete(anyLong(), any());

        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isOk());

        verify(courseService).delete(1L, any());
    }

    @Test
    void shouldReturnUnauthorizedWhenDeleteCourseWithoutJwt() throws Exception {
        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isUnauthorized());
    }
}

