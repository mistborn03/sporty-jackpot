package com.sporty.jackpot.domain;

/**
 * Identifies which {@link com.sporty.jackpot.strategy.ContributionStrategy}
 * a jackpot uses. Adding a new contribution model later means adding a new
 * enum value and a new strategy implementation - no existing code changes.
 */
public enum ContributionType {
    FIXED,
    VARIABLE
}
