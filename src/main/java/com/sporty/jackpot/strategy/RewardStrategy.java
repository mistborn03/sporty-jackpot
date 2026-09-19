package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;

import java.math.BigDecimal;

/**
 * Decides whether a bet wins the jackpot reward.
 * <p>
 * {@code currentPoolAmount} is the jackpot's live pool at evaluation time,
 * which is also what a win pays out.
 */
public interface RewardStrategy {
    boolean evaluateWin(BigDecimal currentPoolAmount, Jackpot jackpot);
}
