package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RewardStrategyTest {

    @Test
    void variableStrategy_guaranteesWin_whenPoolAtOrAboveLimit() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("JP-3")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.VARIABLE)
                .baseRewardChance(new BigDecimal("0.05"))
                .rewardGrowthRate(new BigDecimal("0.02"))
                .rewardStepAmount(new BigDecimal("100.00"))
                .poolLimit(new BigDecimal("1000.00"))
                .build();

        VariableChanceRewardStrategy strategy = new VariableChanceRewardStrategy();

        // Pool snapshot exactly at the limit -> guaranteed win regardless of RNG.
        boolean won = strategy.evaluateWin(new BigDecimal("1000.00"), jackpot);
        assertThat(won).isTrue();

        boolean wonAbove = strategy.evaluateWin(new BigDecimal("1500.00"), jackpot);
        assertThat(wonAbove).isTrue();
    }

    @Test
    void fixedStrategy_ignoresPoolAmount() {
        Jackpot jackpot = Jackpot.builder()
                .jackpotId("JP-4")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(BigDecimal.ONE)  // 100% chance for a deterministic test
                .build();

        FixedChanceRewardStrategy strategy = new FixedChanceRewardStrategy();

        assertThat(strategy.evaluateWin(new BigDecimal("50.00"), jackpot)).isTrue();
        assertThat(strategy.evaluateWin(new BigDecimal("5000.00"), jackpot)).isTrue();
    }
}
