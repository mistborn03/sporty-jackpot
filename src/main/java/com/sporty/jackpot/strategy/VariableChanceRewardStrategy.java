package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Win chance that grows a step at a time as the pool grows, and becomes a
 * guaranteed win once the pool reaches {@code poolLimit}. See the README for
 * the rule and worked examples.
 */
@Component
public class VariableChanceRewardStrategy implements RewardStrategy {

    @Override
    public boolean evaluateWin(BigDecimal currentPoolAmount, Jackpot jackpot) {
        if (currentPoolAmount.compareTo(jackpot.getPoolLimit()) >= 0) {
            return true;
        }

        BigDecimal poolGrowth = currentPoolAmount.subtract(jackpot.getInitialPoolAmount());
        int steps = poolGrowth.signum() <= 0
                ? 0
                : poolGrowth.divideToIntegralValue(jackpot.getRewardStepAmount()).intValue();

        BigDecimal chance = jackpot.getBaseRewardChance()
                .add(jackpot.getRewardGrowthRate().multiply(BigDecimal.valueOf(steps)));

        double roll = ThreadLocalRandom.current().nextDouble();
        return roll < chance.doubleValue();
    }
}
