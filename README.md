# sporty-jackpot

Backend service that accepts bets, contributes them to jackpot pools, and evaluates
bets for jackpot rewards. Built for the Jackpot BE home assignment.

## Stack

- Java 21, Spring Boot 3.3.4 (Web + Validation)
- **No real Kafka** - the producer is mocked (logs the payload, then synchronously
  hands off to the consumer), per the assignment's own fallback condition.
- **No real database** - all persistence is in-memory (`ConcurrentHashMap`-backed
  repositories), per the assignment's condition.
- Lombok for boilerplate.

## Running it

Prerequisites: JDK 21, Maven 3.8+.

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on `http://localhost:8080` and seeds two demo jackpots on boot
(see `JackpotSeedConfig`):

| Jackpot ID | Contribution | Reward |
|---|---|---|
| `JP-FIXED` | Fixed 5% of bet amount | Fixed 10% win chance |
| `JP-VARIABLE` | Starts at 10%, decays 1% per $100 pool growth, floors at 2% | Starts at 5%, grows 2% per $100 pool growth, guaranteed win at pool >= $1000 |

## API

| Method | Path | Purpose | Codes |
|---|---|---|---|
| `POST` | `/bets` | Publish a bet to the mocked `jackpot-bets` topic | `202`, `400`, `409` |
| `POST` | `/bets/{betId}/evaluate` | Evaluate a bet for a jackpot reward | `200`, `404` |
| `GET` | `/bets/{betId}` | Inspect a published bet (debug, not in spec) | `200`, `404` |
| `GET` | `/jackpots` | Inspect jackpot pool state (debug, not in spec) | `200` |

See **[API.md](API.md)** for a step-wise walkthrough with curls, expected
responses, and the edge cases worth checking (duplicate betId, repeated
evaluation, guaranteed win and pool reset, unmatched jackpotId).

## Design notes and assumptions

The spec deliberately leaves several rules open-ended; here's what was assumed and why.

- **Contribution and reward rules are a Strategy pattern.** `ContributionStrategy`
  and `RewardStrategy` are interfaces with `Fixed*` and `Variable*` implementations,
  selected per-jackpot via a factory keyed on an enum. Adding a third model later
  means adding one class and one enum value - no existing code changes.

- **Variable contribution decay / variable reward growth formula.** The spec says
  "over time it becomes lower/bigger at a fixed rate as the pool increases" without
  giving the exact formula. Implemented as: for every `stepAmount` the pool has
  grown past its initial value, the percentage moves by `rate` (decaying for
  contribution, growing for reward), floored/capped accordingly. This is
  configurable per-jackpot rather than hardcoded.

- **Reward evaluation uses a pool *snapshot*, not the live pool.** Each
  `JackpotContribution` stores `currentJackpotAmount` - the pool value right after
  that specific contribution was applied. `/evaluate` judges a bet's reward chance
  against that snapshot rather than the jackpot's live current pool. This matters
  because the pool can be reset by someone else's win between when a bet
  contributes and when it's evaluated; without a snapshot, a bet's odds would
  depend on evaluation *order*, not on what it actually contributed to. The spec's
  own schema (asking `JackpotContribution` to store "Current Jackpot Amount") is
  what suggested this design.

- **A bet with no contribution cannot be evaluated.** If `jackpotId` never matched
  a known jackpot, or the bet doesn't exist, `/evaluate` returns `404` rather than
  a false "no win" - those are different facts and conflating them is misleading.

- **Evaluation is idempotent per bet - for losses as well as wins.** Calling
  `/evaluate` again returns the stored outcome instead of rolling again. Recording
  only wins would leave a loss with no trace, letting a caller retry a losing bet
  until it won, so every outcome is recorded as a `JackpotEvaluation`. The slot is
  claimed with an atomic `putIfAbsent`, so two concurrent evaluations of the same
  bet settle on one outcome rather than both paying out and both resetting the pool.

- **A betId can only be published once.** `POST /bets` returns `409 Conflict` for a
  betId that was already published. Otherwise a replayed bet contributes to the pool
  again while overwriting its own contribution record - leaving the pool and the
  contribution ledger disagreeing, with the surviving snapshot inflated toward the
  variable reward's guaranteed-win threshold. `BetRepository` is the idempotency
  store for this; the check is an atomic `putIfAbsent` inside `save()` rather than a
  check-then-save in the controller, so concurrent duplicates can't both get through.
  It has to sit at the API edge because with a real Kafka producer the consumer
  hasn't processed the bet by the time publish returns.

- **Non-matching jackpot IDs are accepted at publish time, rejected at consume
  time.** `POST /bets` doesn't validate the jackpot exists - that check happens in
  the consumer, preserving the producer/consumer decoupling a real Kafka setup
  would have. The betId is still registered (`BetRepository`); it simply produces no
  `JackpotContribution`.

- **Pool mutations are synchronized per-jackpot.** Since there's no database
  transaction to lean on, `Jackpot.addToPool()` / `resetPool()` are synchronized
  methods so concurrent bets against the same jackpot don't race on a
  read-modify-write of the pool amount.

- **One contribution per bet.** `JackpotContributionRepository` is keyed by
  `betId`, i.e. a bet is assumed to contribute at most once.

## Tests

```bash
mvn test
```

Covers:

- the contribution and reward strategy math (`ContributionStrategyTest`,
  `RewardStrategyTest`), including the decay floor and the
  guaranteed-win-at-pool-limit case;
- evaluation idempotency (`JackpotEvaluationServiceTest`) - that a losing bet is
  replayed rather than re-rolled, and a winning bet pays out once;
- duplicate betId rejection (`BetRepositoryTest`) - and that the original bet
  survives, since a silent overwrite is what let a replay double-contribute.

## What's intentionally out of scope

- Real Kafka integration (mocked per assignment conditions).
- Real persistence / durability across restarts (in-memory per assignment conditions).
- AuthN/AuthZ on the endpoints.
- Pagination on the debug `/jackpots` listing.
