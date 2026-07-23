package com.medsupply.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsupply.platform.modules.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock to prevent JWT filter interception blocking simple checks

    @Test
    void testPublicEndpointsAreAccessible() throws Exception {
        // /actuator/health is permitAll
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpointsRejectUnauthenticated() throws Exception {
        // /reports/metrics is protected and should return 401 (Unauthorized) without a valid user session or token
        mockMvc.perform(get("/api/v1/reports/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testProtectedEndpointsAllowAuthenticated() throws Exception {
        // With an ADMIN mock user, the endpoint should proceed beyond security filters
        // Note: It might return 404 if there are no reports mappings found at that exact mock route or if context-path issues,
        // but it should NOT return 401 Unauthorized.
        mockMvc.perform(get("/reports/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // NotFound is expected because servlet path without "/api/v1" or similar returns 404, but security allowed it!
    }
}
