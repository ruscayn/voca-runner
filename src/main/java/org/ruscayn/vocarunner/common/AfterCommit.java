package org.ruscayn.vocarunner.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 트랜잭션 안에서만 부를 수 있다 (트랜잭션이 없으면 IllegalStateException) */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

}
