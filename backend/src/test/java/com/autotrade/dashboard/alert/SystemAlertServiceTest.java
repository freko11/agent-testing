package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.risk.KillSwitchEvent;
import com.autotrade.dashboard.risk.KillSwitchState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kill-switch/signal-drift-decay alert recording and its re-alert cooldown (post-E8 follow-up). */
@ExtendWith(MockitoExtension.class)
class SystemAlertServiceTest {

    private static final long COOLDOWN_MS = 60_000L;

    @Mock
    private SystemAlertRepository repository;

    private Instant now;
    private SystemAlertService service;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-08-25T00:00:00Z");
        service = new SystemAlertService(repository, Clock.fixed(now, ZoneOffset.UTC), COOLDOWN_MS);
    }

    private KillSwitchEvent killSwitchEvent() {
        KillSwitchEvent event = new KillSwitchEvent(KillSwitchState.ENGAGED, "alice");
        ReflectionTestUtils.setField(event, "id", 7L);
        return event;
    }

    @Test
    void recordKillSwitchEngaged_savesAlertWithComposedMessageAndFk() {
        KillSwitchEvent event = killSwitchEvent();

        service.recordKillSwitchEngaged(event);

        ArgumentCaptor<SystemAlert> captor = ArgumentCaptor.forClass(SystemAlert.class);
        verify(repository).save(captor.capture());
        SystemAlert saved = captor.getValue();
        assertEquals(SystemAlertType.KILL_SWITCH_ENGAGED, saved.getAlertType());
        assertEquals("Kill switch engaged by 'alice'", saved.getMessage());
        assertEquals(event, saved.getKillSwitchEvent());
    }

    @Test
    void recordKillSwitchEngaged_repositoryThrows_swallowsException() {
        doThrow(new RuntimeException("db down")).when(repository).save(any());

        assertDoesNotThrow(() -> service.recordKillSwitchEngaged(killSwitchEvent()));
    }

    @Test
    void recordSignalDriftDecay_firstCall_saves() {
        when(repository.existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                eq(SystemAlertType.SIGNAL_DRIFT_DECAY), eq("v6"), eq("BUY"), eq(Checkpoint.MAX), any()))
                .thenReturn(false);

        service.recordSignalDriftDecay("v6", "BUY", Checkpoint.MAX, -1.2);

        ArgumentCaptor<SystemAlert> captor = ArgumentCaptor.forClass(SystemAlert.class);
        verify(repository).save(captor.capture());
        SystemAlert saved = captor.getValue();
        assertEquals(SystemAlertType.SIGNAL_DRIFT_DECAY, saved.getAlertType());
        assertEquals("v6", saved.getRuleTableVersion());
        assertEquals("BUY", saved.getDirection());
        assertEquals(Checkpoint.MAX, saved.getCheckpoint());
        assertEquals(-1.2, saved.getDriftPct());
        assertNotNull(saved.getMessage());
    }

    @Test
    void recordSignalDriftDecay_withinCooldownForSameTriple_skipsInsert() {
        when(repository.existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                eq(SystemAlertType.SIGNAL_DRIFT_DECAY), eq("v6"), eq("BUY"), eq(Checkpoint.MAX), any()))
                .thenReturn(true);

        service.recordSignalDriftDecay("v6", "BUY", Checkpoint.MAX, -1.2);

        verify(repository, never()).save(any());
    }

    @Test
    void recordSignalDriftDecay_afterCooldownElapses_savesAgain() {
        // First call: nothing recorded yet within the cooldown window -> saves.
        Instant firstCallCutoff = now.minusMillis(COOLDOWN_MS);
        when(repository.existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                SystemAlertType.SIGNAL_DRIFT_DECAY, "v6", "BUY", Checkpoint.MAX, firstCallCutoff))
                .thenReturn(false);
        service.recordSignalDriftDecay("v6", "BUY", Checkpoint.MAX, -1.2);
        verify(repository, times(1)).save(any());

        // Advance the clock past the cooldown -- the repository call now uses a later cutoff, and
        // (per this later-in-time fixture) finds no alert since the cutoff, so it saves again.
        Instant later = now.plusMillis(COOLDOWN_MS + 1_000);
        service = new SystemAlertService(repository, Clock.fixed(later, ZoneOffset.UTC), COOLDOWN_MS);
        Instant secondCallCutoff = later.minusMillis(COOLDOWN_MS);
        when(repository.existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                SystemAlertType.SIGNAL_DRIFT_DECAY, "v6", "BUY", Checkpoint.MAX, secondCallCutoff))
                .thenReturn(false);

        service.recordSignalDriftDecay("v6", "BUY", Checkpoint.MAX, -1.2);

        verify(repository, times(2)).save(any());
    }

    @Test
    void recordSignalDriftDecay_repositoryThrows_swallowsException() {
        when(repository.existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                any(), any(), any(), any(), any())).thenReturn(false);
        doThrow(new RuntimeException("db down")).when(repository).save(any());

        assertDoesNotThrow(() -> service.recordSignalDriftDecay("v6", "BUY", Checkpoint.MAX, -1.2));
    }

    @Test
    void list_delegatesToRepository() {
        when(repository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(List.of());

        List<SystemAlert> result = service.list(20);

        assertNotNull(result);
        verify(repository).findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 20));
    }
}
