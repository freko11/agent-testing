package com.autotrade.dashboard.order;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E6-F3-S3: real, full-context wiring check for {@code GET /api/orders/audit-entries} that {@link
 * OrderQueryControllerTest} (service mocked) and {@link OrderAuditEntryRepositoryTest} (repository
 * only, no HTTP/auth) can't individually provide — proves {@code OrderService.listAuditEntries},
 * the real {@code OrderAuditEntryRepository} JOIN-FETCH/count query, and normal session auth all
 * wire together end to end through real HTTP/JSON. Same real login flow as {@code
 * SignalDriftControllerIntegrationTest} (E8-F5-S1), and the same reason for existing: the sandboxed
 * environment this story was implemented in has no Docker daemon available to run the real Oracle
 * XE + {@code spring-boot:run} stack the {@code run} skill normally drives. See
 * docs/CHANGELOG.md's E6-F3-S3 entry.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderAuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void auditEntries_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders/audit-entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditEntries_wiresRealServiceAndRepository_returnsEmptyPageWithNoOrders() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/orders/audit-entries").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void auditEntries_negativePage_returns400() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/orders/audit-entries").session(session).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    /** Same real login flow {@code SecurityConfigTest}/{@code SignalDriftControllerIntegrationTest} already prove. */
    private MockHttpSession login() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .param("username", "testuser")
                        .param("password", "testpass1234")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        return csrfCookie;
    }
}
