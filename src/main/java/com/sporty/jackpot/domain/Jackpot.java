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

    /** Resets the pool to its initial value, e.g. after a jackpot win. */
    public synchronized void resetPool() {
        this.currentPoolAmount = this.initialPoolAmount;
    }
}
