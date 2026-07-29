package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves {@link AlpacaTradingCredentialBootstrap}'s three cases: env vars
 * unset (no-op), a credential already stored (no-op, never overwrites), and
 * a genuinely first-time seed (stores exactly once).
 */
@ExtendWith(MockitoExtension.class)
class AlpacaTradingCredentialBootstrapTest {

    @Mock
    private BrokerCredentialService credentialService;

    @Test
    void run_credentialsNotConfigured_doesNothing() {
        AlpacaTradingCredentialBootstrap bootstrap =
                new AlpacaTradingCredentialBootstrap(credentialService, "", "");

        bootstrap.run(null);

        verify(credentialService, never()).store(any(), any(), any(), any());
    }

    @Test
    void run_credentialAlreadyStored_doesNotOverwrite() {
        when(credentialService.find(Broker.ALPACA, TradingMode.PAPER))
                .thenReturn(Optional.of(new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "existing", "existing")));
        AlpacaTradingCredentialBootstrap bootstrap =
                new AlpacaTradingCredentialBootstrap(credentialService, "env-key", "env-secret");

        bootstrap.run(null);

        verify(credentialService, never()).store(any(), any(), any(), any());
    }

    @Test
    void run_noCredentialStoredYet_seedsFromEnvVarsExactlyOnce() {
        when(credentialService.find(Broker.ALPACA, TradingMode.PAPER)).thenReturn(Optional.empty());
        AlpacaTradingCredentialBootstrap bootstrap =
                new AlpacaTradingCredentialBootstrap(credentialService, "env-key", "env-secret");

        bootstrap.run(null);

        verify(credentialService).store(Broker.ALPACA, TradingMode.PAPER, "env-key", "env-secret");
    }
}
