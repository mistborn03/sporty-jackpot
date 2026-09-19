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
- springdoc-openapi for Swagger UI at `/swagger-ui.html`.

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

## Contribution and reward rules

Both variable models move their percentage in whole **steps** of pool growth.
Growth is measured from the jackpot's *initial* pool, not from zero, and a
partially completed step changes nothing until it is crossed.

### Variable contribution - decays as the pool grows

```
growth       = currentPool - initialPool
steps        = floor(growth / contributionStepAmount)
rate         = baseContributionPct - (contributionDecayRate * steps)
effectivePct = max(rate, minContributionPct)        # floor, never negative
contribution = betAmount * effectivePct             # HALF_UP to 2 dp
```

With base 10%, decay 1%, floor 2%, step 100.00, initial pool 50.00, on a 100.00 bet:

| Pool | Growth | Steps | Rate | Contributes |
|---|---|---|---|---|
| 50.00 | 0.00 | 0 | 10% | 10.00 |
| 149.00 | 99.00 | 0 | 10% | 10.00 (step not yet crossed) |
| 150.00 | 100.00 | 1 | 9% | 9.00 |
| 550.00 | 500.00 | 5 | 5% | 5.00 |
| 950.00 | 900.00 | 9 | 2% | 2.00 (floor reached) |
| 5050.00 | 5000.00 | 50 | 2% | 2.00 (floor holds) |

The floor is what stops a large pool from taking a zero or negative cut, which
would otherwise leave it unable to grow at all.

### Variable reward - grows as the pool grows

```
growth = currentPool - initialPool
steps  = floor(growth / rewardStepAmount)
chance = baseRewardChance + (rewardGrowthRate * steps)

if currentPool >= poolLimit  ->  chance = 100%, regardless of the above
```

With base 5%, growth 2%, step 100.00, initial pool 50.00, limit 1000.00:

| Pool | Growth | Steps | Win chance |
|---|---|---|---|
| 50.00 | 0.00 | 0 | 5% |
| 149.00 | 99.00 | 0 | 5% (step not yet crossed) |
| 150.00 | 100.00 | 1 | 7% |
| 550.00 | 500.00 | 5 | 15% |
| 1000.00 | — | — | 100% (guaranteed by the limit) |

Below `poolLimit` the chance is uncapped and can exceed 100% on its own; that
is harmless, since the limit guarantees a win first.

## API

| Method | Path | Purpose | Codes |
|---|---|---|---|
| `POST` | `/bets` | Publish a bet to the mocked `jackpot-bets` topic | `202`, `400`, `404`, `409` |
| `POST` | `/bets/{betId}/evaluate` | Evaluate a bet for a jackpot reward | `200`, `404` |
| `GET` | `/bets/{betId}` | Inspect a published bet (debug, not in spec) | `200`, `404` |
| `POST` | `/jackpots` | Create a jackpot | `201`, `400`, `409` |
| `GET` | `/jackpots` | List jackpots with current pool state | `200` |
| `GET` | `/jackpots/{jackpotId}` | Fetch a single jackpot | `200`, `404` |

Interactive docs (Swagger UI): **http://localhost:8080/swagger-ui.html**
(OpenAPI JSON at `/v3/api-docs`).

See **[API.md](API.md)** for a step-wise walkthrough with curls, expected
responses, and the edge cases worth checking (duplicate betId, repeated
evaluation, live-pool payout, jackpot creation validation).

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

- **A win takes the live pool, not the pool as it stood at contribution time.**
  Winning a jackpot means winning what is in the pot *now*, including everything
  other bets have added since. If user A bets (pool 50 -> 60) and user B bets
  after them (pool 60 -> 70), A winning pays out 70. Both the reward chance and
  the payout read the live pool. `JackpotContribution.currentJackpotAmount` is
  kept as a historical record of what each bet grew the pool to, but is no longer
  what evaluation is judged against. The trade-off is that a bet's odds and payout
  now depend on *when* it is evaluated relative to other activity; that is
  inherent to a shared pool, and is what "winning the jackpot" normally means.

- **The payout reads and resets the pool atomically.** `Jackpot.claimPool()` does
  both in one synchronized step - a contribution landing between a separate read
  and reset would be paid to nobody and then wiped.

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

- **`POST /bets` rejects an unknown jackpotId with 404.** The bet is validated
  against the jackpot store before anything is published, so a typo fails
  immediately rather than being accepted and silently producing no contribution.
  This trades away some producer/consumer decoupling - a real Kafka producer would
  not consult the jackpot store - in exchange for the caller learning about the
  mistake at the point they made it. The consumer still re-checks, since with real
  Kafka a jackpot could be removed between publish and consume.

- **Jackpots can be created at runtime via `POST /jackpots`.** Note this endpoint
  is unauthenticated, as is the rest of the service: anything that can reach it can
  create a jackpot with a 100% reward chance. Real deployments would put jackpot
  administration behind auth or in a separate admin service - see
  "intentionally out of scope" below.

- **Pool mutations are synchronized per-jackpot.** Since there's no database
  transaction to lean on, `Jackpot.addToPool()` / `claimPool()` are synchronized
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
