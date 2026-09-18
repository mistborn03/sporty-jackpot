package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.strategy.ContributionStrategy;
import com.sporty.jackpot.strategy.ContributionStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Consumes a bet (stands in for a @KafkaListener on jackpot-bets) and, if
 * the bet's jackpotId matches a known jackpot, contributes to that
 * jackpot's pool. If there is no matching jackpot, per the assignment
 * spec ("if there is such a jackpot..."), no contribution is made - this
 * is logged and the bet is simply left without a JackpotContribution
 * record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetConsumerService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final ContributionStrategyFactory contributionStrategyFactory;

    public void consume(Bet bet) {
        Optional<Jackpot> maybeJackpot = jackpotRepository.findById(bet.jackpotId());

        if (maybeJackpot.isEmpty()) {
            log.warn("No jackpot found for jackpotId={} (bet={}) - skipping contribution",
                    bet.jackpotId(), bet.betId());
            return;
        }

        Jackpot jackpot = maybeJackpot.get();
        ContributionStrategy strategy = contributionStrategyFactory.get(jackpot.getContributionType());

        BigDecimal contributionAmount = strategy.calculateContribution(bet.betAmount(), jackpot);
        BigDecimal newPoolAmount = jackpot.addToPool(contributionAmount);

        JackpotContribution contribution = new JackpotContribution(
                bet.betId(),
                bet.userId(),
                bet.jackpotId(),
                bet.betAmount(),
                contributionAmount,
                newPoolAmount,
                Instant.now()
        );
        contributionRepository.save(contribution);

        log.info("Contribution recorded: bet={} jackpot={} contributed={} newPool={}",
                bet.betId(), jackpot.getJackpotId(), contributionAmount, newPoolAmount);
    }
}
