package com.autotrade.dashboard.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAuditEntryRepository extends JpaRepository<OrderAuditEntry, Long> {
}
