package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.enums.TransactionType;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CardServiceIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    @AfterEach
    void cleanDb() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
    }

    @Test
    void concurrencyTestStrongSpendContention() throws Exception {
        Card card = cardService.createCard("Bob", new BigDecimal("100.00"), "create-strong-1");
        UUID cardId = card.getId();

        int threads = 20;
        BigDecimal spendAmount = new BigDecimal("15.00");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < threads; i++) {
            String key = "spend-strong-" + i;
            futures.add(executor.submit(() -> runSpend(cardId, spendAmount, key, ready, start)));
        }

        assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int successCalls = 0;
        int failedCalls = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(10, TimeUnit.SECONDS)) {
                successCalls++;
            } else {
                failedCalls++;
            }
        }
        executor.shutdownNow();

        Card current = cardService.getCard(cardId);
        List<Transaction> txs = transactionRepository.findByCard_Id(cardId);
        long successTx = txs.stream()
                .filter(tx -> tx.getType() == TransactionType.SPEND && tx.getStatus() == TransactionStatus.SUCCESS)
                .count();
        long declinedTx = txs.stream()
                .filter(tx -> tx.getType() == TransactionType.SPEND && tx.getStatus() == TransactionStatus.DECLINED)
                .count();

        assertThat(successCalls).isEqualTo(6);
        assertThat(failedCalls).isEqualTo(14);
        assertThat(successTx).isEqualTo(6);
        assertThat(declinedTx).isEqualTo(14);
        assertThat(current.getBalance()).isEqualByComparingTo("10.00");
        assertThat(current.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private boolean runSpend(UUID cardId, BigDecimal amount, String key, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            cardService.spend(cardId, amount, key);
            return true;
        } catch (InsufficientBalanceException ex) {
            return false;
        }
    }
}
