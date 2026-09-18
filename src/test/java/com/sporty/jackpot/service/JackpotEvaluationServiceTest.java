package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.dto.EvaluationResponse;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotEvaluationRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.FixedChanceRewardStrategy;
import com.sporty.jackpot.strategy.RewardStrategyFactory;
import com.sporty.jackpot.strategy.VariableChanceRewardStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JackpotEvaluationServiceTest {

    private JackpotRepository jackpotRepository;
    private JackpotContributionRepository contributionRepository;
    private JackpotEvaluationRepository evaluationRepository;
    private JackpotRewardRepository rewardRepository;
    private JackpotEvaluationService service;

    @BeforeEach
    void setUp() {
        jackpotRepository = new JackpotRepository();
        contributionRepository = new JackpotContributionRepository();
        evaluationRepository = new JackpotEvaluationRepository();
        rewardRepository = new JackpotRewardRepository();

        RewardStrategyFactory factory = new RewardStrategyFactory(
                new FixedChanceRewardStrategy(), new VariableChanceRewardStrategy());

        service = new JackpotEvaluationService(
                contributionRepository, evaluationRepository,
                rewardRepository, jackpotRepository, factory);
    }

    private void seed(String jackpotId, String betId, BigDecimal rewardChance) {
        jackpotRepository.save(Jackpot.builder()
                .jackpotId(jackpotId)
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(rewardChance)
                .build());

        contributionRepository.save(new JackpotContribution(
                betId, "user-1", jackpotId,
                new BigDecimal("100.00"), new BigDecimal("5.00"),
                new BigDecimal("150.00"), Instant.now()));
    }

    @Test
    void losingBet_isNotRolledAgain_onRepeatedEvaluation() {
        seed("JP-LOSE", "bet-1", BigDecimal.ZERO);   // 0% chance -> always loses

        EvaluationResponse first = service.evaluate("bet-1");
        assertThat(first.won()).isFalse();
        assertThat(first.alreadyEvaluated()).isFalse();

        // The loss must be replayed, not re-rolled - otherwise a caller could
        // retry a losing bet until it wins.
        EvaluationResponse second = service.evaluate("bet-1");
        assertThat(second.won()).isFalse();
        assertThat(second.alreadyEvaluated()).isTrue();
    }

    @Test
    void winningBet_replaysSameResult_andPaysOutOnce() {
        seed("JP-WIN", "bet-2", BigDecimal.ONE);     // 100% chance -> always wins

        EvaluationResponse first = service.evaluate("bet-2");
        assertThat(first.won()).isTrue();
        assertThat(first.alreadyEvaluated()).isFalse();
        assertThat(first.rewardAmount()).isEqualByComparingTo("150.00");

        EvaluationResponse second = service.evaluate("bet-2");
        assertThat(second.won()).isTrue();
        assertThat(second.alreadyEvaluated()).isTrue();
        assertThat(second.rewardAmount()).isEqualByComparingTo("150.00");

        assertThat(rewardRepository.findByBetId("bet-2")).isPresent();
    }
}
