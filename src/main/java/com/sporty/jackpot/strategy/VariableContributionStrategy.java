package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Contributes a percentage that starts at {@code baseContributionPct} and
 * decays by {@code contributionDecayRate} for every {@code contributionStepAmount}
 * the pool has grown past its initial value, floored at {@code minContributionPct}.
 * <p>
 * Example: base 10%, decay 0.5% per $1,000 grown, floor 2%. At pool = initial + $4,000
 * (4 steps), effective rate = 10% - 4*0.5% = 8%. At pool = initial + $16,000+,
 * the rate has decayed to the 2% floor.
 */
@Component
public class VariableContributionStrategy implements ContributionStrategy {

    @Override
    public BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot) {
        BigDecimal poolGrowth = jackpot.getCurrentPoolAmount().subtract(jackpot.getInitialPoolAmount());
        int steps = poolGrowth.signum() <= 0
                ? 0
                : poolGrowth.divideToIntegralValue(jackpot.getContributionStepAmount()).intValue();

        BigDecimal decayed = jackpot.getBaseContributionPct()
                .subtract(jackpot.getContributionDecayRate().multiply(BigDecimal.valueOf(steps)));

        BigDecimal effectivePct = decayed.max(jackpot.getMinContributionPct());

        return betAmount.multiply(effectivePct).setScale(2, RoundingMode.HALF_UP);
    }
}
