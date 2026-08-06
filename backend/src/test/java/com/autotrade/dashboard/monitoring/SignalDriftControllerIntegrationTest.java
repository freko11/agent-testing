package com.autotrade.dashboard.monitoring;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E8-F5-S1: a real, full-context wiring check that the mocked-collaborator unit test ({@link
 * LiveSignalDriftServiceTest}) can't provide — {@code LiveSignalDriftService}'s {@code @Value}
 * constructor parameters actually bind from config, {@code @ConditionalOnProperty} actually
 * creates the service/controller beans when the flag is on (the rest of this codebase's test
 * suite only ever exercises the flag-off/beans-absent path, since {@code
 * src/test/resources/application.properties} forces {@code monitoring.live-drift.enabled=false}
 * for the same "no live network calls in CI" reason every other scheduled job in this app is
 * disabled there), and the new {@code OrderAuditEntryRepository} {@code JOIN FETCH} JPQL query
 * actually executes against a real (H2-in-Oracle-mode) database, end to end through real HTTP/
 * JSON — not just a unit-tested Java method call. Authenticates via the real session-cookie/CSRF
 * login flow ({@code SecurityConfigTest}'s own pattern), not {@code @WithMockUser} — this
 * endpoint has no test precedent of its own yet, and the real flow doubles as one more genuine
 * exercise of this app's actual auth path rather than a shortcut around it.
 *
 * <p>Runs against an empty {@code order_audit_entries} table (no fixture rows inserted), so
 * {@code computeDrift} finds zero audit entries and therefore never calls the real {@code
 * MarketDataService} — this test makes no outbound network call, consistent with this codebase's
 * "no live network calls in CI" rule despite overriding the flag that's normally used to prevent
 * exactly that.
 *
 * <p>Stands in for this story's live end-to-end verification: the sandboxed environment this
 * story was implemented in has no Docker daemon available to run the real Oracle XE + {@code
 * spring-boot:run} stack the {@code run} skill normally drives, and H2 (test-scoped only) isn't
 * on the {@code spring-boot:run} runtime classpath either — both attempted and confirmed blocked
 * before falling back to this. See docs/CHANGELOG.md's E8-F5-S1 entry.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "monitoring.live-drift.enabled=true",
        "monitoring.live-drift.lookback-days=90",
        "monitoring.live-drift.min-sample-size=20",
        "monitoring.live-drift.decay-threshold-pct=0.5"
})
class SignalDriftControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signalDrift_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/monitoring/signal-drift"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signalDrift_wiresRealServiceAndRepository_returnsEmptyReportWithNoAuditEntries() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/monitoring/signal-drift").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lookbackDays").value(90))
                .andExpect(jsonPath("$.totalAuditEntriesConsidered").value(0))
                .andExpect(jsonPath("$.scoredAuditEntries").value(0))
                .andExpect(jsonPath("$.skippedAuditEntries").value(0))
                .andExpect(jsonPath("$.versions").isArray())
                .andExpect(jsonPath("$.versions").isEmpty());
    }

    @Test
    void signalDrift_lookbackDaysOverride_isHonored() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/monitoring/signal-drift").session(session).param("lookbackDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lookbackDays").value(7));
    }

    /** Same real login flow {@code SecurityConfigTest} already proves: fetch the CSRF cookie,
     * POST credentials with it echoed back as a header, return the resulting session. */
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
