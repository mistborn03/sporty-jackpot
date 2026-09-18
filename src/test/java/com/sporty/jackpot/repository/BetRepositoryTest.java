package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.exception.DuplicateBetException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetRepositoryTest {

    private final BetRepository repository = new BetRepository();

    private Bet bet(String betId, BigDecimal amount) {
        return new Bet(betId, "user-1", "JP-FIXED", amount, Instant.now());
    }

    @Test
    void rejectsSecondBetWithSameId_andKeepsTheOriginal() {
        repository.save(bet("bet-1", new BigDecimal("100.00")));

        assertThatThrownBy(() -> repository.save(bet("bet-1", new BigDecimal("999.00"))))
                .isInstanceOf(DuplicateBetException.class)
                .hasMessageContaining("bet-1");

        // The original must survive - a silent overwrite is what let a replayed
        // bet contribute to the pool again while leaving one contribution record.
        assertThat(repository.findById("bet-1")).isPresent();
        assertThat(repository.findById("bet-1").orElseThrow().betAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void acceptsDistinctBetIds() {
        repository.save(bet("bet-1", new BigDecimal("100.00")));
        repository.save(bet("bet-2", new BigDecimal("100.00")));

        assertThat(repository.findById("bet-1")).isPresent();
        assertThat(repository.findById("bet-2")).isPresent();
    }
}
