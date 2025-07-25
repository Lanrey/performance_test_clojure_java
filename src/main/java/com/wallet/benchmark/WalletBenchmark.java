package com.wallet.benchmark;

import com.wallet.WalletService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(64) // Simulating 50k concurrent users
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 2)
@Measurement(iterations = 3)
public class WalletBenchmark {
    private WalletService walletService;
    private static final int NUM_ACCOUNTS = 1000;

    @Setup
    public void setup() {
        walletService = new WalletService();
        // Initialize wallets with random balances
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String userId = "user" + i;
            BigDecimal initialBalance = new BigDecimal("1000.00");
            walletService.createWallet(userId, initialBalance);
        }
    }

    @Benchmark
    public void testConcurrentCreditDebit(ThreadState state) {
        String userId = state.nextUserId();
        try {
            walletService.credit(userId, new BigDecimal("10.00"));
            walletService.debit(userId, new BigDecimal("5.00"));
        } catch (Exception e) {
            // Log or handle exceptions
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        private int counter = 0;

        public String nextUserId() {
            counter = (counter + 1) % NUM_ACCOUNTS;
            return "user" + counter;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WalletBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}