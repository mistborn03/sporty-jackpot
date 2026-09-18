package com.sporty.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records that a bet contributed to a jackpot pool.
 * <p>
 * {@code currentJackpotAmount} is a SNAPSHOT of the pool right after this
 * contribution was applied. It is intentionally preserved (never
 * recomputed) so that {@code /evaluate} can later judge this bet's reward
 * chance against the pool state the bettor actually contributed to -
 * rather than whatever the pool happens to be at evaluation time, which
 * may have already been reset by someone else's win in the meantime.
 */
public record JackpotContribution(
        String betId,
        String userId,
        String jackpotId,
        BigDecimal stakeAmount,
        BigDecimal contributionAmount,
        BigDecimal currentJackpotAmount,
        Instant createdAt
) {
}
