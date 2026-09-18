package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotEvaluation;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keyed by betId so a bet is only ever evaluated once. */
@Repository
public class JackpotEvaluationRepository {

    private final Map<String, JackpotEvaluation> store = new ConcurrentHashMap<>();

    /**
     * Atomically claims the evaluation slot for this bet. Returns empty if
     * this evaluation was stored, or the already-stored evaluation if another
     * caller got there first - so concurrent evaluations of the same bet
     * settle on one outcome instead of both paying out.
     */
    public Optional<JackpotEvaluation> saveIfAbsent(JackpotEvaluation evaluation) {
        return Optional.ofNullable(store.putIfAbsent(evaluation.betId(), evaluation));
    }

    public Optional<JackpotEvaluation> findByBetId(String betId) {
        return Optional.ofNullable(store.get(betId));
    }
}
