package com.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class WalletServiceTest {
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService();
    }

    @Test
    void testBasicOperations() {
        String userId = "test-user";
        walletService.createWallet(userId, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("150.00"),
                walletService.credit(userId, new BigDecimal("50.00")));

        assertEquals(new BigDecimal("120.00"),
                walletService.debit(userId, new BigDecimal("30.00")));
    }

    @Test
    void testConcurrentOperations() throws InterruptedException {
        String userId = "concurrent-user";
        walletService.createWallet(userId, new BigDecimal("1000.00"));

        int numThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    walletService.credit(userId, new BigDecimal("10.00"));
                    walletService.debit(userId, new BigDecimal("5.00"));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal expectedBalance = new BigDecimal("1500.00"); // 1000 + (10-5)*100
        assertEquals(expectedBalance, walletService.getWallets().get(userId).getBalance());
    }

    @Test
    void testInsufficientFunds() {
        String userId = "poor-user";
        walletService.createWallet(userId, new BigDecimal("10.00"));

        assertThrows(IllegalStateException.class, () ->
                walletService.debit(userId, new BigDecimal("20.00")));
    }
}