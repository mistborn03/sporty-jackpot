package com.sporty.jackpot.controller;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.dto.BetPublishResponse;
import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.dto.BetResponse;
import com.sporty.jackpot.dto.EvaluationResponse;
import com.sporty.jackpot.exception.BetNotFoundException;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.BetRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.service.BetPublisherService;
import com.sporty.jackpot.service.JackpotEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bets", description = "Publish bets and evaluate them for jackpot rewards")
public class BetController {

    private final BetRepository betRepository;
    private final JackpotRepository jackpotRepository;
    private final BetPublisherService betPublisherService;
    private final JackpotEvaluationService evaluationService;

    @PostMapping
    @Operation(
            summary = "Publish a bet",
            description = """
                    Validates the jackpot exists, then publishes the bet to the mocked
                    jackpot-bets topic, where the consumer contributes to that jackpot's
                    pool. Returns 202 Accepted, 404 for an unknown jackpotId, or 409 if
                    that betId was already published.""")
    public ResponseEntity<BetPublishResponse> publishBet(@Valid @RequestBody BetRequest request) {
        if (jackpotRepository.findById(request.jackpotId()).isEmpty()) {
            throw new JackpotNotFoundException(request.jackpotId());
        }

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
    @Operation(
            summary = "Evaluate a bet for a jackpot reward",
            description = """
                    Rolls once against the jackpot's live pool. A win pays out the whole
                    live pool - including everything other bets contributed after this
                    one - and resets it. Idempotent per bet: calling again replays the
                    stored outcome for losses as well as wins, so a losing bet cannot be
                    retried until it wins.""")
    public ResponseEntity<EvaluationResponse> evaluate(@PathVariable String betId) {
        return ResponseEntity.ok(evaluationService.evaluate(betId));
    }

    @GetMapping("/{betId}")
    @Operation(summary = "Fetch a published bet")
    public ResponseEntity<BetResponse> getBet(@PathVariable String betId) {
        return betRepository.findById(betId)
                .map(BetResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BetNotFoundException(betId));
    }
}
