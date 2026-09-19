package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.domain.JackpotEvaluation;
import com.sporty.jackpot.domain.JackpotReward;
import com.sporty.jackpot.dto.EvaluationResponse;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.exception.NoContributionFoundException;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotEvaluationRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.RewardStrategy;
import com.sporty.jackpot.strategy.RewardStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JackpotEvaluationService {

    private final JackpotContributionRepository contributionRepository;
    private final JackpotEvaluationRepository evaluationRepository;
    private final JackpotRewardRepository rewardRepository;
    private final JackpotRepository jackpotRepository;
    private final RewardStrategyFactory rewardStrategyFactory;

    /**
     * Evaluates whether a bet wins its jackpot's reward.
     * <p>
     * - A bet with no recorded contribution (never matched a jackpot, or
     *   doesn't exist) cannot be evaluated: throws NoContributionFoundException,
     *   mapped to 404 by the global exception handler.
     * - Evaluation is idempotent per bet, for losses as well as wins: the
     *   dice are rolled at most once and the outcome is then replayed. A
     *   caller cannot retry a losing bet until it wins.
     * - Both the reward chance and the payout use the jackpot's LIVE pool at
     *   evaluation time, not the pool as it stood when this bet contributed.
     *   Winning a jackpot means winning what is in the pot now, including
     *   everything other bets have added since.
     */
    public EvaluationResponse evaluate(String betId) {
        JackpotContribution contribution = contributionRepository.findByBetId(betId)
                .orElseThrow(() -> new NoContributionFoundException(betId));

        Optional<JackpotEvaluation> existing = evaluationRepository.findByBetId(betId);
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }

        Jackpot jackpot = jackpotRepository.findById(contribution.jackpotId())
                .orElseThrow(() -> new JackpotNotFoundException(contribution.jackpotId()));

        RewardStrategy strategy = rewardStrategyFactory.get(jackpot.getRewardType());
        boolean won = strategy.evaluateWin(jackpot.getCurrentPoolAmount(), jackpot);

        JackpotEvaluation evaluation = new JackpotEvaluation(
                betId, contribution.userId(), contribution.jackpotId(),
                won, null, Instant.now()
        );

        // Claim the evaluation slot before touching the pool: if another caller
        // got here first, this one must not pay out on top of theirs.
        Optional<JackpotEvaluation> claimedByAnother = evaluationRepository.saveIfAbsent(evaluation);
        if (claimedByAnother.isPresent()) {
            return toResponse(claimedByAnother.get(), true);
        }

        if (!won) {
            log.info("Bet {} evaluated - no win (jackpot={})", betId, jackpot.getJackpotId());
            return toResponse(evaluation, false);
        }

        BigDecimal rewardAmount = jackpot.claimPool();
        JackpotEvaluation paid = new JackpotEvaluation(
                betId, contribution.userId(), contribution.jackpotId(),
                true, rewardAmount, evaluation.createdAt()
        );
        evaluationRepository.recordPayout(paid);
        rewardRepository.save(new JackpotReward(
                betId, contribution.userId(), contribution.jackpotId(),
                rewardAmount, paid.createdAt()
        ));

        log.info("Bet {} WON jackpot {} - amount={} - pool reset to {}",
                betId, jackpot.getJackpotId(), rewardAmount, jackpot.getInitialPoolAmount());

        return toResponse(paid, false);
    }

    private EvaluationResponse toResponse(JackpotEvaluation evaluation, boolean alreadyEvaluated) {
        return new EvaluationResponse(
                evaluation.betId(),
                evaluation.userId(),
                evaluation.jackpotId(),
                evaluation.won(),
                evaluation.rewardAmount(),
                alreadyEvaluated,
                message(evaluation.won(), alreadyEvaluated)
        );
    }

    private String message(boolean won, boolean alreadyEvaluated) {
        if (alreadyEvaluated) {
            return won
                    ? "This bet was already evaluated and had already won."
                    : "This bet was already evaluated and did not win.";
        }
        return won ? "Congratulations, this bet won the jackpot!" : "No win this time.";
    }
}
