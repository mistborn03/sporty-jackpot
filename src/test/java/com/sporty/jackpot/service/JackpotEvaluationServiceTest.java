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

    /** Seeds a jackpot with a flat win chance and returns it for pool manipulation. */
    private Jackpot seedJackpot(String jackpotId, BigDecimal rewardChance) {
        return jackpotRepository.save(Jackpot.builder()
                .jackpotId(jackpotId)
                .initialPoolAmount(new BigDecimal("50.00"))
                .contributionType(ContributionType.FIXED)
                .fixedContributionPct(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED)
                .fixedRewardChance(rewardChance)
                .build());
    }

    private void recordContribution(String betId, String userId, String jackpotId, BigDecimal poolAfter) {
        contributionRepository.save(new JackpotContribution(
                betId, userId, jackpotId,
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                poolAfter, Instant.now()));
    }

    @Test
    void winnerTakesLivePool_includingContributionsMadeAfterTheirOwnBet() {
        Jackpot jackpot = seedJackpot("JP-LIVE", BigDecimal.ONE);   // always wins

        // User A bets: pool 50.00 -> 60.00. A's contribution snapshot is 60.00.
        jackpot.addToPool(new BigDecimal("10.00"));
        recordContribution("bet-A", "user-A", "JP-LIVE", new BigDecimal("60.00"));

        // User B bets afterwards: pool 60.00 -> 70.00.
        jackpot.addToPool(new BigDecimal("10.00"));
        recordContribution("bet-B", "user-B", "JP-LIVE", new BigDecimal("70.00"));

        // A now wins. The payout is the live pool of 70.00, not A's 60.00 snapshot.
        EvaluationResponse result = service.evaluate("bet-A");

        assertThat(result.won()).isTrue();
        assertThat(result.rewardAmount()).isEqualByComparingTo("70.00");
        assertThat(rewardRepository.findByBetId("bet-A").orElseThrow().jackpotRewardAmount())
                .isEqualByComparingTo("70.00");
    }

    @Test
    void winResetsPoolToInitial() {
        Jackpot jackpot = seedJackpot("JP-RESET", BigDecimal.ONE);
        jackpot.addToPool(new BigDecimal("500.00"));
        recordContribution("bet-1", "user-1", "JP-RESET", new BigDecimal("550.00"));

        service.evaluate("bet-1");

        assertThat(jackpot.getCurrentPoolAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void losingBet_isNotRolledAgain_onRepeatedEvaluation() {
        seedJackpot("JP-LOSE", BigDecimal.ZERO);     // never wins
        recordContribution("bet-2", "user-1", "JP-LOSE", new BigDecimal("60.00"));

        EvaluationResponse first = service.evaluate("bet-2");
        assertThat(first.won()).isFalse();
        assertThat(first.alreadyEvaluated()).isFalse();

        // The loss must be replayed, not re-rolled - otherwise a caller could
        // retry a losing bet until it wins.
        EvaluationResponse second = service.evaluate("bet-2");
        assertThat(second.won()).isFalse();
        assertThat(second.alreadyEvaluated()).isTrue();
    }

    @Test
    void winningBet_replaysSameResult_andPaysOutOnce() {
        Jackpot jackpot = seedJackpot("JP-WIN", BigDecimal.ONE);
        jackpot.addToPool(new BigDecimal("100.00"));
        recordContribution("bet-3", "user-1", "JP-WIN", new BigDecimal("150.00"));

        EvaluationResponse first = service.evaluate("bet-3");
        assertThat(first.won()).isTrue();
        assertThat(first.rewardAmount()).isEqualByComparingTo("150.00");

        // The pool has already been claimed and reset; a replay must return the
        // original payout rather than re-reading (and re-claiming) the pool.
        EvaluationResponse second = service.evaluate("bet-3");
        assertThat(second.won()).isTrue();
        assertThat(second.alreadyEvaluated()).isTrue();
        assertThat(second.rewardAmount()).isEqualByComparingTo("150.00");
        assertThat(jackpot.getCurrentPoolAmount()).isEqualByComparingTo("50.00");
    }
}
