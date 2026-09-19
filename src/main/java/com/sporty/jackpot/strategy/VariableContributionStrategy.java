package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Contribution percentage that decays a step at a time as the pool grows,
 * floored at {@code minContributionPct}. See the README for the rule and
 * worked examples.
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
