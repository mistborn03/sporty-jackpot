package com.sporty.jackpot.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A jackpot must carry the fields its own types read. Without this, a missing
 * field only surfaces as an NPE inside a strategy on the first bet.
 */
class JackpotConfigValidationTest {

    private Jackpot.JackpotBuilder variableContribution() {
        return Jackpot.builder()
                .jackpotId("JP-X")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.VARIABLE)
                .baseContributionPct(new BigDecimal("0.10"))
                .contributionDecayRate(new BigDecimal("0.01"))
                .minContributionPct(new BigDecimal("0.02"))
                .contributionStepAmount(new BigDecimal("100.00"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(new BigDecimal("0.10"));
    }

    @Test
    void rejectsVariableContributionMissingStepAmount() {
        assertThatThrownBy(() -> variableContribution().contributionStepAmount(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contributionStepAmount");
    }

    @Test
    void rejectsFixedContributionMissingPct() {
        assertThatThrownBy(() -> Jackpot.builder()
                .jackpotId("JP-Y")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(new BigDecimal("0.10"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixedContributionPct");
    }

    @Test
    void rejectsVariableRewardMissingPoolLimit() {
        assertThatThrownBy(() -> Jackpot.builder()
                .jackpotId("JP-Z")
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.VARIABLE)
                .baseRewardChance(new BigDecimal("0.05"))
                .rewardGrowthRate(new BigDecimal("0.02"))
                .rewardStepAmount(new BigDecimal("100.00"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("poolLimit");
    }

    @Test
    void acceptsFullyConfiguredJackpot() {
        assertThatCode(() -> variableContribution().build()).doesNotThrowAnyException();
    }
}
