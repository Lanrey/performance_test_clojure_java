package com.wallet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;

public class MeteredWalletService extends WalletService {
    private final MeterRegistry registry;
    private final Timer creditTimer;
    private final Timer debitTimer;

    public MeteredWalletService(MeterRegistry registry) {
        this.registry = registry;
        this.creditTimer = registry.timer("wallet.credit");
        this.debitTimer = registry.timer("wallet.debit");

        // Add gauges for monitoring
        registry.gauge("wallet.accounts.total", this, MeteredWalletService::getTotalAccounts);
    }

    private int getTotalAccounts() {
        return super.getWallets().size();
    }

    @Override
    public BigDecimal credit(String userId, BigDecimal amount) {
        return creditTimer.record(() -> {
            try {
                return super.credit(userId, amount);
            } catch (Exception e) {
                registry.counter("wallet.credit.errors").increment();
                throw e;
            }
        });
    }

    @Override
    public BigDecimal debit(String userId, BigDecimal amount) {
        return debitTimer.record(() -> {
            try {
                return super.debit(userId, amount);
            } catch (Exception e) {
                registry.counter("wallet.debit.errors").increment();
                throw e;
            }
        });
    }
}