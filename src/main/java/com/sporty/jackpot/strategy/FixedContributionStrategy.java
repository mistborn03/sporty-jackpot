package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Contributes a flat percentage of the bet amount, regardless of pool size. */
@Component
public class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot) {
        return betAmount.multiply(jackpot.getFixedContributionPct())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
