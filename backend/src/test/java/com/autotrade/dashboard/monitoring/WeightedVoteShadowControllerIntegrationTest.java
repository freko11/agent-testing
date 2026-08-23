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
 * E8-F5-S3: a real, full-context wiring check that {@link WeightedVoteShadowScoringServiceTest}'s
 * mocked-collaborator unit test can't provide -- {@code WeightedVoteShadowScoringService}'s {@code
 * @Value} constructor parameters actually bind from config, {@code @ConditionalOnProperty}
 * actually creates the service/controller beans when the flag is on (every other test in this
 * suite only exercises the flag-off/beans-absent path, since {@code
 * src/test/resources/application.properties} forces {@code
 * monitoring.weighted-vote-shadow.enabled=false} for the same "no live network calls in CI"
 * reason every other scheduled job in this app is disabled there), and the new {@code
 * SignalCallEntryRepository} {@code JOIN FETCH} JPQL query actually executes against a real
 * (H2-in-Oracle-mode) database, end to end through real HTTP/JSON. Mirrors {@code
 * SignalDriftControllerIntegrationTest}'s own shape exactly, including its login helper and its
 * "empty table means no outbound market-data call" reasoning.
 *
 * <p>Runs against an empty {@code signal_calls} table (no fixture rows inserted), so {@code
 * computeShadowReport} finds zero entries and never calls the real {@code MarketDataService} --
 * this test makes no outbound network call.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "monitoring.weighted-vote-shadow.enabled=true",
        "monitoring.weighted-vote-shadow.lookback-days=90"
})
class WeightedVoteShadowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void weightedVoteShadow_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/monitoring/weighted-vote-shadow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void weightedVoteShadow_wiresRealServiceAndRepository_returnsEmptyReportWithNoSignalCalls() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/monitoring/weighted-vote-shadow").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lookbackDays").value(90))
                .andExpect(jsonPath("$.totalEntriesConsidered").value(0))
                .andExpect(jsonPath("$.skippedEntries").value(0))
                .andExpect(jsonPath("$.agreeCount").value(0))
                .andExpect(jsonPath("$.weightedOnlyBuy.count").value(0))
                .andExpect(jsonPath("$.weightedOnlySell.count").value(0))
                .andExpect(jsonPath("$.downgradedByWeighted.count").value(0))
                .andExpect(jsonPath("$.knownLimitations").isArray())
                .andExpect(jsonPath("$.knownLimitations", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void weightedVoteShadow_lookbackDaysOverride_isHonored() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/monitoring/weighted-vote-shadow").session(session).param("lookbackDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lookbackDays").value(7));
    }

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
