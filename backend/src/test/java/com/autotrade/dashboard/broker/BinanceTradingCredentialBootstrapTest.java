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
 * Proves {@link BinanceTradingCredentialBootstrap}'s three cases: env vars
 * unset (no-op), a credential already stored (no-op, never overwrites), and
 * a genuinely first-time seed (stores exactly once) — mirrors {@link
 * AlpacaTradingCredentialBootstrapTest}.
 */
@ExtendWith(MockitoExtension.class)
class BinanceTradingCredentialBootstrapTest {

    @Mock
    private BrokerCredentialService credentialService;

    @Test
    void run_credentialsNotConfigured_doesNothing() {
        BinanceTradingCredentialBootstrap bootstrap =
                new BinanceTradingCredentialBootstrap(credentialService, "", "");

        bootstrap.run(null);

        verify(credentialService, never()).store(any(), any(), any(), any());
    }

    @Test
    void run_credentialAlreadyStored_doesNotOverwrite() {
        when(credentialService.find(Broker.BINANCE, TradingMode.PAPER))
                .thenReturn(Optional.of(new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "existing", "existing")));
        BinanceTradingCredentialBootstrap bootstrap =
                new BinanceTradingCredentialBootstrap(credentialService, "env-key", "env-secret");

        bootstrap.run(null);

        verify(credentialService, never()).store(any(), any(), any(), any());
    }

    @Test
    void run_noCredentialStoredYet_seedsFromEnvVarsExactlyOnce() {
        when(credentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.empty());
        BinanceTradingCredentialBootstrap bootstrap =
                new BinanceTradingCredentialBootstrap(credentialService, "env-key", "env-secret");

        bootstrap.run(null);

        verify(credentialService).store(Broker.BINANCE, TradingMode.PAPER, "env-key", "env-secret");
    }
}
