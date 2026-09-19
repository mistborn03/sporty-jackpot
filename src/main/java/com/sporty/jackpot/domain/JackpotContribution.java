package com.sporty.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records that a bet contributed to a jackpot pool.
 * <p>
 * {@code currentJackpotAmount} is the pool value right after this
 * contribution was applied - a historical record of what this bet grew the
 * pool to. It is not what {@code /evaluate} pays out: a win takes the live
 * pool at evaluation time, which includes everything contributed since.
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
