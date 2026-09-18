package com.sporty.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A bet as received on the publish endpoint. */
public record Bet(
        String betId,
        String userId,
        String jackpotId,
        BigDecimal betAmount,
        Instant receivedAt
) {
}
