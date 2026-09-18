package com.sporty.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The outcome of evaluating a bet, recorded for wins AND losses.
 * <p>
 * Losses must be recorded too: without them, re-calling /evaluate on a
 * losing bet would roll the dice again, letting a caller retry until they
 * win. This record is what makes evaluation idempotent per bet.
 *
 * @param rewardAmount the amount won, or null when {@code won} is false
 */
public record JackpotEvaluation(
        String betId,
        String userId,
        String jackpotId,
        boolean won,
        BigDecimal rewardAmount,
        Instant createdAt
) {
}
