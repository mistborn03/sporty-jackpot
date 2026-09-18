package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/** Flat win probability, independent of pool size. */
@Component
public class FixedChanceRewardStrategy implements RewardStrategy {

    @Override
    public boolean evaluateWin(BigDecimal poolAmountAtContribution, Jackpot jackpot) {
        double roll = ThreadLocalRandom.current().nextDouble();
        return roll < jackpot.getFixedRewardChance().doubleValue();
    }
}
