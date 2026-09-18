package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.ContributionType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the {@link ContributionStrategy} for a given {@link ContributionType}.
 * Adding a new contribution model = add an enum value + a new
 * {@code @Component} strategy + one line here. No other code changes.
 */
@Component
public class ContributionStrategyFactory {

    private final Map<ContributionType, ContributionStrategy> strategies = new EnumMap<>(ContributionType.class);

    public ContributionStrategyFactory(FixedContributionStrategy fixed, VariableContributionStrategy variable) {
        strategies.put(ContributionType.FIXED, fixed);
        strategies.put(ContributionType.VARIABLE, variable);
    }

    public ContributionStrategy get(ContributionType type) {
        ContributionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No contribution strategy registered for type: " + type);
        }
        return strategy;
    }
}
