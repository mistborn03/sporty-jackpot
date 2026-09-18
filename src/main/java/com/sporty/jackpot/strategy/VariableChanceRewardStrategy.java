package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Win probability starts at {@code baseRewardChance} and grows by
 * {@code rewardGrowthRate} for every {@code rewardStepAmount} the pool has
 * grown past its initial value. Once the pool is at or above
 * {@code poolLimit}, the chance becomes a guaranteed 100%.
 */
@Component
public class VariableChanceRewardStrategy implements RewardStrategy {

    @Override
    public boolean evaluateWin(BigDecimal poolAmountAtContribution, Jackpot jackpot) {
        if (poolAmountAtContribution.compareTo(jackpot.getPoolLimit()) >= 0) {
            return true;
        }

        BigDecimal poolGrowth = poolAmountAtContribution.subtract(jackpot.getInitialPoolAmount());
        int steps = poolGrowth.signum() <= 0
                ? 0
                : poolGrowth.divideToIntegralValue(jackpot.getRewardStepAmount()).intValue();

        BigDecimal chance = jackpot.getBaseRewardChance()
                .add(jackpot.getRewardGrowthRate().multiply(BigDecimal.valueOf(steps)));

        double roll = ThreadLocalRandom.current().nextDouble();
        return roll < chance.doubleValue();
    }
}
