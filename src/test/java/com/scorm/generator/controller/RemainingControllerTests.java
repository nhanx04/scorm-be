package com.scorm.generator.controller;

import com.scorm.generator.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentPageController.class)
class ContentPageControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContentPageService svc;

    @Test @WithMockUser void shouldCreate() throws Exception {
        when(svc.create(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(post("/content-pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test void shouldRequireAuth() throws Exception {
        mvc.perform(post("/content-pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }
}

@WebMvcTest(QuizPageController.class)
class QuizPageControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private QuizPageService svc;

    @Test @WithMockUser void shouldCreate() throws Exception {
        when(svc.create(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(post("/quiz-pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test void shouldRequireAuth() throws Exception {
        mvc.perform(post("/quiz-pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }
}

@WebMvcTest(HealthCheckController.class)
class HealthCheckControllerTest {
    @Autowired private MockMvc mvc;

    @Test void shouldReturnHealth() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }
}

