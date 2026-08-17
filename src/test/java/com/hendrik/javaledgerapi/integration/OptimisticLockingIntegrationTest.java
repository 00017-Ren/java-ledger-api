package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class OptimisticLockingIntegrationTest extends PostgresIntegrationTest {
    private static final long TIMEOUT_SECONDS = 5;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void optimisticLock_rejectsTransaction_whenAccountVersionMismatched() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        Account account = testDataFactory.persistAccount(user, BigDecimal.TEN);
        UUID accountId = account.getId();

        BigDecimal startingBalance = account.getBalance();
        int startingVersion = account.getVersion();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch bothReadsCompleted = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);

        Callable<WorkerOutcome> loadAndWait = () -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Account loadedAccount = accountRepository.findById(accountId).orElseThrow();

                    bothReadsCompleted.countDown();
                    awaitLatch(allowWrites, "permission to write");

                    loadedAccount.setBalance(loadedAccount.getBalance().add(BigDecimal.ONE));
                    accountRepository.saveAndFlush(loadedAccount);
                });

                return WorkerOutcome.success();
            } catch (ObjectOptimisticLockingFailureException exception) {
                return WorkerOutcome.failure(exception);
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<WorkerOutcome> firstWorker = executor.submit(loadAndWait);
            Future<WorkerOutcome> secondWorker = executor.submit(loadAndWait);

            try {
                assertThat(bothReadsCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

                allowWrites.countDown();

                List<WorkerOutcome> outcomes = List.of(
                        firstWorker.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        secondWorker.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                );

                assertThat(outcomes).filteredOn(WorkerOutcome::succeeded).hasSize(1);
                assertThat(outcomes)
                        .filteredOn(outcome -> !outcome.succeeded())
                        .singleElement()
                        .extracting(WorkerOutcome::failure)
                        .isInstanceOf(ObjectOptimisticLockingFailureException.class);

                Account reloadedAccount = accountRepository.findById(accountId).orElseThrow();

                assertThat(reloadedAccount.getBalance())
                        .isEqualByComparingTo(startingBalance.add(BigDecimal.ONE));
                assertThat(reloadedAccount.getVersion()).isEqualTo(startingVersion + 1);

            } finally {
                allowWrites.countDown();
            }
        }
    }

    private static void awaitLatch(CountDownLatch countDownLatch, String description) {
        try {
            if (!countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for " + description);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + description, exception);
        }
    }

    private record WorkerOutcome(Throwable failure) {
        static WorkerOutcome success() {
            return new WorkerOutcome(null);
        }

        static WorkerOutcome failure(Throwable failure) {
            return new WorkerOutcome(failure);
        }

        boolean succeeded() {
            return failure == null;
        }
    }
}
