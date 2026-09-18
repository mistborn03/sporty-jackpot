package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionStrategyTest {

    @Test
    void fixedStrategy_alwaysContributesSamePercentage() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("JP-1")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(new BigDecimal("0.10"))
                .build();

        FixedContributionStrategy strategy = new FixedContributionStrategy();

        assertThat(strategy.calculateContribution(new BigDecimal("100.00"), jackpot))
                .isEqualByComparingTo("5.00");

        // Grow the pool - fixed strategy should still return the same percentage.
        jackpot.addToPool(new BigDecimal("500.00"));
        assertThat(strategy.calculateContribution(new BigDecimal("100.00"), jackpot))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void variableStrategy_decaysAsPoolGrows_andRespectsFloor() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("JP-2")
                .initialPoolAmount(new BigDecimal("0.00"))
                .contributionType(ContributionType.VARIABLE)
                .baseContributionPct(new BigDecimal("0.10"))
                .contributionDecayRate(new BigDecimal("0.01"))
                .minContributionPct(new BigDecimal("0.02"))
                .contributionStepAmount(new BigDecimal("100.00"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(new BigDecimal("0.10"))
                .build();

        VariableContributionStrategy strategy = new VariableContributionStrategy();

        // Pool still at initial (0 growth) -> base 10%
        assertThat(strategy.calculateContribution(new BigDecimal("100.00"), jackpot))
                .isEqualByComparingTo("10.00");

        // Grow pool by $500 (5 steps) -> 10% - 5*1% = 5%
        jackpot.addToPool(new BigDecimal("500.00"));
        assertThat(strategy.calculateContribution(new BigDecimal("100.00"), jackpot))
                .isEqualByComparingTo("5.00");

        // Grow pool by a lot more -> should floor at 2%, never go negative
        jackpot.addToPool(new BigDecimal("10000.00"));
        assertThat(strategy.calculateContribution(new BigDecimal("100.00"), jackpot))
                .isEqualByComparingTo("2.00");
    }
}
