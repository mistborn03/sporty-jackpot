package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;

import java.math.BigDecimal;

/**
 * Decides whether a bet wins the jackpot reward.
 * <p>
 * {@code poolAmountAtContribution} is the pool snapshot recorded on the
 * bet's {@code JackpotContribution} (see that class's javadoc for why a
 * snapshot is used instead of the jackpot's live current pool).
 */
public interface RewardStrategy {
    boolean evaluateWin(BigDecimal poolAmountAtContribution, Jackpot jackpot);
}
