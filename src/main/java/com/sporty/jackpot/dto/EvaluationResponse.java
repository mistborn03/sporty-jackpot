package com.sporty.jackpot.dto;

import java.math.BigDecimal;

/**
 * @param rewardAmount     null if not won
 * @param alreadyEvaluated true if this bet had already been evaluated previously
 */
public record EvaluationResponse(
        String betId,
        String userId,
        String jackpotId,
        boolean won,
        BigDecimal rewardAmount,
        boolean alreadyEvaluated,
        String message
) {
}
