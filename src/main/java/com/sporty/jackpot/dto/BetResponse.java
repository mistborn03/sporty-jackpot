package com.sporty.jackpot.dto;

import com.sporty.jackpot.domain.Bet;

import java.math.BigDecimal;
import java.time.Instant;

public record BetResponse(
        String betId,
        String userId,
        String jackpotId,
        BigDecimal betAmount,
        Instant receivedAt
) {

    public static BetResponse from(Bet bet) {
        return new BetResponse(
                bet.betId(),
                bet.userId(),
                bet.jackpotId(),
                bet.betAmount(),
                bet.receivedAt()
        );
    }
}
