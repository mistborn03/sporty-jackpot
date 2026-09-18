package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;

import java.math.BigDecimal;

/**
 * Decides how much of a bet's stake is contributed to a jackpot pool.
 * Implementations must not mutate the jackpot - they only calculate.
 */
public interface ContributionStrategy {
    BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot);
}
