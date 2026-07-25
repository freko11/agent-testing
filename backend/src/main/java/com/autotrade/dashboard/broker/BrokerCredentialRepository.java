package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrokerCredentialRepository extends JpaRepository<BrokerCredential, Long> {

    Optional<BrokerCredential> findByBrokerAndEnvironmentAndIsActiveTrue(Broker broker, TradingMode environment);
}
