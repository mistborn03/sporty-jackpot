package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RewardStrategyFactory {

    private final Map<RewardType, RewardStrategy> strategies = new EnumMap<>(RewardType.class);

    public RewardStrategyFactory(FixedChanceRewardStrategy fixed, VariableChanceRewardStrategy variable) {
        strategies.put(RewardType.FIXED, fixed);
        strategies.put(RewardType.VARIABLE, variable);
    }

    public RewardStrategy get(RewardType type) {
        RewardStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No reward strategy registered for type: " + type);
        }
        return strategy;
    }
}
