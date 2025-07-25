package com.wallet;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

public class WalletService {
    private final ConcurrentHashMap<String, WalletAccount> wallets = new ConcurrentHashMap<>();

    public static class WalletAccount {
        private final String id;
        private final AtomicReference<BigDecimal> balance;
        private final StampedLock lock;

        public WalletAccount(String id, BigDecimal initialBalance) {
            this.id = id;
            this.balance = new AtomicReference<>(initialBalance);
            this.lock = new StampedLock();
        }

        public BigDecimal getBalance() {
            long stamp = lock.tryOptimisticRead();
            BigDecimal balance = this.balance.get();
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    balance = this.balance.get();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return balance;
        }
    }

    public WalletAccount createWallet(String userId, BigDecimal initialBalance) {
        WalletAccount account = new WalletAccount(userId, initialBalance);
        wallets.put(userId, account);
        return account;
    }

    public BigDecimal credit(String userId, BigDecimal amount) {
        WalletAccount account = wallets.get(userId);
        if (account == null) {
            throw new IllegalArgumentException("Wallet not found");
        }

        long stamp = account.lock.writeLock();
        try {
            BigDecimal currentBalance = account.balance.get();
            BigDecimal newBalance = currentBalance.add(amount);
            account.balance.set(newBalance);
            return newBalance;
        } finally {
            account.lock.unlockWrite(stamp);
        }
    }

    protected ConcurrentHashMap<String, WalletAccount> getWallets() {
        return wallets;
    }


    public BigDecimal debit(String userId, BigDecimal amount) {
        WalletAccount account = wallets.get(userId);
        if (account == null) {
            throw new IllegalArgumentException("Wallet not found");
        }

        long stamp = account.lock.writeLock();
        try {
            BigDecimal currentBalance = account.balance.get();
            if (currentBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds");
            }
            BigDecimal newBalance = currentBalance.subtract(amount);
            account.balance.set(newBalance);
            return newBalance;
        } finally {
            account.lock.unlockWrite(stamp);
        }
    }
}