package com.sporty.jackpot.dto;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A jackpot to create. Which of the optional fields are required depends on
 * the chosen contribution/reward type - see
 * {@link com.sporty.jackpot.strategy.VariableContributionStrategy} and
 * {@link com.sporty.jackpot.strategy.VariableChanceRewardStrategy} for what
 * each one does. Cross-field rules are enforced by {@link Jackpot}'s builder,
 * which reports the specific missing field.
 */
@Schema(description = "Definition of a new jackpot")
public record JackpotRequest(

        @NotBlank(message = "jackpotId is required")
        @Schema(example = "JP-NEW")
        String jackpotId,

        @NotNull(message = "initialPoolAmount is required")
        @DecimalMin(value = "0.00", message = "initialPoolAmount cannot be negative")
        @Schema(example = "50.00")
        BigDecimal initialPoolAmount,

        @NotNull(message = "contributionType is required")
        ContributionType contributionType,

        @Schema(description = "FIXED contribution: flat share of each bet, e.g. 0.05 for 5%", example = "0.05")
        BigDecimal fixedContributionPct,

        @Schema(description = "VARIABLE contribution: starting share before any decay", example = "0.10")
        BigDecimal baseContributionPct,

        @Schema(description = "VARIABLE contribution: share removed per step of pool growth", example = "0.01")
        BigDecimal contributionDecayRate,

        @Schema(description = "VARIABLE contribution: floor the share never decays below", example = "0.02")
        BigDecimal minContributionPct,

        @Schema(description = "VARIABLE contribution: pool growth that makes up one decay step", example = "100.00")
        BigDecimal contributionStepAmount,

        @NotNull(message = "rewardType is required")
        RewardType rewardType,

        @Schema(description = "FIXED reward: flat win chance, e.g. 0.10 for 10%", example = "0.10")
        BigDecimal fixedRewardChance,

        @Schema(description = "VARIABLE reward: starting win chance before any growth", example = "0.05")
        BigDecimal baseRewardChance,

        @Schema(description = "VARIABLE reward: chance added per step of pool growth", example = "0.02")
        BigDecimal rewardGrowthRate,

        @Schema(description = "VARIABLE reward: pool growth that makes up one growth step", example = "100.00")
        BigDecimal rewardStepAmount,

        @Schema(description = "VARIABLE reward: pool value at or above which a win is guaranteed", example = "1000.00")
        BigDecimal poolLimit
) {

    public Jackpot toJackpot() {
        return Jackpot.builder()
                .jackpotId(jackpotId)
                .initialPoolAmount(initialPoolAmount)
                .contributionType(contributionType)
                .fixedContributionPct(fixedContributionPct)
                .baseContributionPct(baseContributionPct)
                .contributionDecayRate(contributionDecayRate)
                .minContributionPct(minContributionPct)
                .contributionStepAmount(contributionStepAmount)
                .rewardType(rewardType)
                .fixedRewardChance(fixedRewardChance)
                .baseRewardChance(baseRewardChance)
                .rewardGrowthRate(rewardGrowthRate)
                .rewardStepAmount(rewardStepAmount)
                .poolLimit(poolLimit)
                .build();
    }
}
