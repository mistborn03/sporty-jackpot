package com.sporty.jackpot.config;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.repository.JackpotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Seeds two demo jackpots on startup so /bets can be tested immediately. */
@Slf4j
@Configuration
public class JackpotSeedConfig {

    @Bean
    CommandLineRunner seedJackpots(JackpotRepository jackpotRepository) {
        return args -> {
            Jackpot fixedJackpot = Jackpot.builder()
                    .jackpotId("JP-FIXED")
                    .initialPoolAmount(new BigDecimal("50.00"))
                    .contributionType(ContributionType.FIXED)
                    .fixedContributionPct(new BigDecimal("0.05"))
                    .rewardType(RewardType.FIXED)
                    .fixedRewardChance(new BigDecimal("0.10"))  // kept high for easy demoing
                    .build();

            Jackpot variableJackpot = Jackpot.builder()
                    .jackpotId("JP-VARIABLE")
                    .initialPoolAmount(new BigDecimal("50.00"))
                    .contributionType(ContributionType.VARIABLE)
                    .baseContributionPct(new BigDecimal("0.10"))
                    .contributionDecayRate(new BigDecimal("0.01"))
                    .minContributionPct(new BigDecimal("0.02"))
                    .contributionStepAmount(new BigDecimal("100.00"))
                    .rewardType(RewardType.VARIABLE)
                    .baseRewardChance(new BigDecimal("0.05"))
                    .rewardGrowthRate(new BigDecimal("0.02"))
                    .rewardStepAmount(new BigDecimal("100.00"))
                    .poolLimit(new BigDecimal("1000.00"))
                    .build();

            jackpotRepository.save(fixedJackpot);
            jackpotRepository.save(variableJackpot);

            log.info("Seeded jackpots: JP-FIXED (fixed 5% contribution / 10% reward chance), " +
                    "JP-VARIABLE (variable contribution decaying from 10%, variable reward chance growing from 5%, " +
                    "guaranteed win at pool >= $1000)");
        };
    }
}
