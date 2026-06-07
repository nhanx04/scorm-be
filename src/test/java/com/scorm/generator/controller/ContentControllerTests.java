package com.scorm.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.service.*;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(SectionController.class)
class SectionControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @MockitoBean private SectionService sectionService;

    private Object createReq;

    @BeforeEach void setUp() { createReq = new Object(); }

    @Test @WithMockUser void shouldCreateSectionSuccessfully() throws Exception {
        when(sectionService.create(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(post("/courses/1/sections").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(createReq))).andExpect(status().isOk());
        verify(sectionService).create(1L, any(), any());
    }

    @Test void shouldReturnUnauthorizedWhenCreateWithoutJwt() throws Exception {
        mvc.perform(post("/courses/1/sections").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser void shouldListSectionsSuccessfully() throws Exception {
        when(sectionService.listByCourseId(anyLong(), any())).thenReturn(null);
        mvc.perform(get("/courses/1/sections")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldGetSectionByIdSuccessfully() throws Exception {
        when(sectionService.getById(anyLong(), any())).thenReturn(null);
        mvc.perform(get("/sections/1")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldUpdateSectionSuccessfully() throws Exception {
        when(sectionService.update(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(patch("/sections/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldDeleteSectionSuccessfully() throws Exception {
        doNothing().when(sectionService).delete(anyLong(), any());
        mvc.perform(delete("/sections/1")).andExpect(status().isOk());
    }
}

@WebMvcTest(PageController.class)
class PageControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @MockitoBean private PageService pageService;

    @Test @WithMockUser void shouldCreatePageSuccessfully() throws Exception {
        when(pageService.create(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(post("/sections/1/pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test void shouldReturnUnauthorizedWhenCreatePageWithoutJwt() throws Exception {
        mvc.perform(post("/sections/1/pages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser void shouldListPagesSuccessfully() throws Exception {
        when(pageService.listBySectionId(anyLong(), any())).thenReturn(null);
        mvc.perform(get("/sections/1/pages")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldGetPageByIdSuccessfully() throws Exception {
        when(pageService.getById(anyLong(), any())).thenReturn(null);
        mvc.perform(get("/pages/1")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldUpdatePageSuccessfully() throws Exception {
        when(pageService.update(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(patch("/pages/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldDeletePageSuccessfully() throws Exception {
        doNothing().when(pageService).delete(anyLong(), any());
        mvc.perform(delete("/pages/1")).andExpect(status().isOk());
    }
}

@WebMvcTest(ContentBlockController.class)
class ContentBlockControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContentBlockService contentBlockService;

    @Test @WithMockUser void shouldCreateContentBlockSuccessfully() throws Exception {
        when(contentBlockService.create(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(post("/content-pages/1/blocks").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldGetContentBlocksSuccessfully() throws Exception {
        when(contentBlockService.getByContentPageId(anyLong(), any())).thenReturn(null);
        mvc.perform(get("/content-pages/1/blocks")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldUpdateContentBlockSuccessfully() throws Exception {
        when(contentBlockService.update(anyLong(), any(), any())).thenReturn(null);
        mvc.perform(patch("/content-blocks/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test @WithMockUser void shouldDeleteContentBlockSuccessfully() throws Exception {
        doNothing().when(contentBlockService).delete(anyLong(), any());
        mvc.perform(delete("/content-blocks/1")).andExpect(status().isOk());
    }
}

