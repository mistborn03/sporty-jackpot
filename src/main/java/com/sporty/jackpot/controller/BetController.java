package com.sporty.jackpot.controller;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.dto.BetPublishResponse;
import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.dto.BetResponse;
import com.sporty.jackpot.dto.EvaluationResponse;
import com.sporty.jackpot.exception.BetNotFoundException;
import com.sporty.jackpot.repository.BetRepository;
import com.sporty.jackpot.service.BetPublisherService;
import com.sporty.jackpot.service.JackpotEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetRepository betRepository;
    private final BetPublisherService betPublisherService;
    private final JackpotEvaluationService evaluationService;

    /**
     * Accepts a bet and publishes it to the jackpot-bets topic (mocked).
     * The bet is accepted regardless of whether its jackpotId matches a
     * known jackpot - that check happens downstream in the consumer, to
     * preserve the producer/consumer decoupling a real Kafka setup would
     * have.
     */
    @PostMapping
    public ResponseEntity<BetPublishResponse> publishBet(@Valid @RequestBody BetRequest request) {
        Bet bet = new Bet(
                request.betId(),
                request.userId(),
                request.jackpotId(),
                request.betAmount(),
                Instant.now()
        );

        betRepository.save(bet);
        betPublisherService.publish(bet);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new BetPublishResponse(bet.betId(), "PUBLISHED", "Bet published to jackpot-bets")
        );
    }

    @PostMapping("/{betId}/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(@PathVariable String betId) {
        return ResponseEntity.ok(evaluationService.evaluate(betId));
    }

    /**
     * Not required by the spec - lets a published bet be inspected, which is
     * how you tell a bet that never matched a jackpot apart from one that
     * was never published at all.
     */
    @GetMapping("/{betId}")
    public ResponseEntity<BetResponse> getBet(@PathVariable String betId) {
        return betRepository.findById(betId)
                .map(BetResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BetNotFoundException(betId));
    }
}
