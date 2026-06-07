package com.scorm.generator.controller;

import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for controller tests providing common setup and utilities.
 * Uses Spring Security Test for JWT authentication mocking.
 */
public abstract class BaseControllerTest {
    protected MockMvc mockMvc;

    protected static final String VALID_USER_ID = "1";
    protected static final String VALID_USER_EMAIL = "test@example.com";
    protected static final String ADMIN_ROLE = "ADMIN";
    protected static final String USER_ROLE = "USER";
}
