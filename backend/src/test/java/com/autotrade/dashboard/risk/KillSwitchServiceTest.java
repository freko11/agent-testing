package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.alert.SystemAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** currentState()/engage()/clear()/assertNotEngaged() behavior for the kill switch (E6-F2-S2). */
@ExtendWith(MockitoExtension.class)
class KillSwitchServiceTest {

    @Mock
    private KillSwitchEventRepository repository;
    @Mock
    private SystemAlertService systemAlertService;

    private KillSwitchService service;
    private final AtomicReference<KillSwitchEvent> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new KillSwitchService(repository, systemAlertService);
    }

    private void stubSave() {
        when(repository.save(any(KillSwitchEvent.class))).thenAnswer(invocation -> {
            KillSwitchEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", 1L);
            ReflectionTestUtils.setField(event, "changedAt", Instant.parse("2026-08-02T00:00:00Z"));
            saved.set(event);
            return event;
        });
        when(repository.findTopByOrderByIdDesc()).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
    }

    @Test
    void currentState_noHistory_defaultsToClearedWithNullChangedAt() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        KillSwitchResponse state = service.currentState();

        assertEquals(KillSwitchState.CLEARED, state.state());
        assertNull(state.changedAt());
        assertNull(state.changedBy());
    }

    @Test
    void isEngaged_noHistory_false() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        assertFalse(service.isEngaged());
    }

    @Test
    void assertNotEngaged_noHistory_doesNotThrow() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.assertNotEngaged());
    }

    @Test
    void engage_insertsEngagedRow() {
        stubSave();

        KillSwitchResponse response = service.engage("alice");

        assertEquals(KillSwitchState.ENGAGED, response.state());
        assertEquals("alice", response.changedBy());
        verify(repository).save(any(KillSwitchEvent.class));
        verify(systemAlertService, times(1)).recordKillSwitchEngaged(any(KillSwitchEvent.class));
    }

    @Test
    void assertNotEngaged_afterEngage_throws() {
        stubSave();
        service.engage("alice");

        assertThrows(KillSwitchEngagedException.class, () -> service.assertNotEngaged());
    }

    @Test
    void engage_alreadyEngaged_idempotentNoOp_noSecondRowInserted() {
        stubSave();
        service.engage("alice");

        KillSwitchResponse response = service.engage("bob");

        verify(repository, times(1)).save(any(KillSwitchEvent.class));
        assertEquals("alice", response.changedBy());
        assertEquals("alice", saved.get().getChangedBy());
        // Still only the one alert from the first engage -- the idempotent no-op never re-alerts.
        verify(systemAlertService, times(1)).recordKillSwitchEngaged(any(KillSwitchEvent.class));
    }

    @Test
    void clear_afterEngage_flipsBackToCleared() {
        stubSave();
        service.engage("alice");

        KillSwitchResponse response = service.clear("bob");

        assertEquals(KillSwitchState.CLEARED, response.state());
        assertEquals("bob", response.changedBy());
        assertFalse(service.isEngaged());
        // Exactly the one alert from the preceding engage() -- clear() itself never records one.
        verify(systemAlertService, times(1)).recordKillSwitchEngaged(any());
    }

    @Test
    void clear_alreadyCleared_idempotentNoOp() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        KillSwitchResponse response = service.clear("bob");

        assertEquals(KillSwitchState.CLEARED, response.state());
        verify(repository, never()).save(any());
        verify(systemAlertService, never()).recordKillSwitchEngaged(any());
    }
}
