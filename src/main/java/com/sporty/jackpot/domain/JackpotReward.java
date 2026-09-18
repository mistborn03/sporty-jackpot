package com.sporty.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Records that a bet won a jackpot reward. */
public record JackpotReward(
        String betId,
        String userId,
        String jackpotId,
        BigDecimal jackpotRewardAmount,
        Instant createdAt
) {
}
