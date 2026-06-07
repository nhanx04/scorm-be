package com.scorm.generator.controller;

import com.scorm.generator.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AuthService svc;

    @Test void shouldRegister() throws Exception {
        when(svc.register(any())).thenReturn(null);
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test void shouldLogin() throws Exception {
        when(svc.login(any())).thenReturn(null);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test void shouldHandleGoogleLogin() throws Exception {
        when(svc.loginGoogle(any())).thenReturn(null);
        mvc.perform(post("/auth/google-login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }
}
