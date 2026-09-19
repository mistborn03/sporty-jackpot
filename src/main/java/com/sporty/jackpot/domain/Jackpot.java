package com.sporty.jackpot.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * A jackpot pool plus its contribution and reward configuration.
 * <p>
 * Held in-memory (see {@link com.sporty.jackpot.repository.JackpotRepository}),
 * so pool mutations must be thread-safe: multiple bets on the same jackpot
 * can be processed concurrently by the (mocked) consumer.
 */
@Getter
public class Jackpot {

    private final String jackpotId;
    private final BigDecimal initialPoolAmount;
    private volatile BigDecimal currentPoolAmount;

    // Contribution config
    private final ContributionType contributionType;
    private final BigDecimal fixedContributionPct;      // used when type = FIXED
    private final BigDecimal baseContributionPct;        // used when type = VARIABLE
    private final BigDecimal contributionDecayRate;      // % decay per growth step
    private final BigDecimal minContributionPct;         // floor for VARIABLE
    private final BigDecimal contributionStepAmount;      // pool growth per decay step

    // Reward config
    private final RewardType rewardType;
    private final BigDecimal fixedRewardChance;           // used when type = FIXED
    private final BigDecimal baseRewardChance;            // used when type = VARIABLE
    private final BigDecimal rewardGrowthRate;             // % growth per growth step
    private final BigDecimal rewardStepAmount;              // pool growth per growth step
    private final BigDecimal poolLimit;                      // VARIABLE reward becomes 100% at/above this

    @Builder
    private Jackpot(String jackpotId, BigDecimal initialPoolAmount,
                    ContributionType contributionType, BigDecimal fixedContributionPct,
                    BigDecimal baseContributionPct, BigDecimal contributionDecayRate,
                    BigDecimal minContributionPct, BigDecimal contributionStepAmount,
                    RewardType rewardType, BigDecimal fixedRewardChance,
                    BigDecimal baseRewardChance, BigDecimal rewardGrowthRate,
                    BigDecimal rewardStepAmount, BigDecimal poolLimit) {
        this.jackpotId = jackpotId;
        this.initialPoolAmount = initialPoolAmount;
        this.currentPoolAmount = initialPoolAmount;
        this.contributionType = contributionType;
        this.fixedContributionPct = fixedContributionPct;
        this.baseContributionPct = baseContributionPct;
        this.contributionDecayRate = contributionDecayRate;
        this.minContributionPct = minContributionPct;
        this.contributionStepAmount = contributionStepAmount;
        this.rewardType = rewardType;
        this.fixedRewardChance = fixedRewardChance;
        this.baseRewardChance = baseRewardChance;
        this.rewardGrowthRate = rewardGrowthRate;
        this.rewardStepAmount = rewardStepAmount;
        this.poolLimit = poolLimit;
        validateConfig();
    }

    /**
     * A jackpot must carry the fields its own contribution/reward type reads.
     * Enforced here rather than only at the API boundary so the invariant holds
     * however a jackpot is built - otherwise a missing field surfaces as an NPE
     * inside a strategy on the first bet, long after the mistake was made.
     */
    private void validateConfig() {
        switch (contributionType) {
            case FIXED -> require(fixedContributionPct, "fixedContributionPct", "FIXED contribution");
            case VARIABLE -> {
                require(baseContributionPct, "baseContributionPct", "VARIABLE contribution");
                require(contributionDecayRate, "contributionDecayRate", "VARIABLE contribution");
                require(minContributionPct, "minContributionPct", "VARIABLE contribution");
                require(contributionStepAmount, "contributionStepAmount", "VARIABLE contribution");
            }
        }
        switch (rewardType) {
            case FIXED -> require(fixedRewardChance, "fixedRewardChance", "FIXED reward");
            case VARIABLE -> {
                require(baseRewardChance, "baseRewardChance", "VARIABLE reward");
                require(rewardGrowthRate, "rewardGrowthRate", "VARIABLE reward");
                require(rewardStepAmount, "rewardStepAmount", "VARIABLE reward");
                require(poolLimit, "poolLimit", "VARIABLE reward");
            }
        }
    }

    private void require(BigDecimal value, String field, String because) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Jackpot " + jackpotId + " uses " + because + " but is missing " + field);
        }
    }

    /**
     * Adds a contribution to the pool and returns the new pool amount.
     * Synchronized per-jackpot-instance so concurrent bets on the same
     * jackpot don't race on a read-modify-write of currentPoolAmount.
     */
    public synchronized BigDecimal addToPool(BigDecimal amount) {
        this.currentPoolAmount = this.currentPoolAmount.add(amount);
        return this.currentPoolAmount;
    }

    /**
     * Pays out the pool: returns everything currently in it and resets it to
     * the initial amount, as one atomic step.
     * <p>
     * Read and reset must not be separate calls - a contribution landing
     * between them would be handed to nobody and then wiped by the reset.
     */
    public synchronized BigDecimal claimPool() {
        BigDecimal claimed = this.currentPoolAmount;
        this.currentPoolAmount = this.initialPoolAmount;
        return claimed;
    }
}
