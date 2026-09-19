# API walkthrough

Step-wise curls covering every endpoint and the behaviour worth checking.
Pool figures assume a freshly started app; status codes and idempotency
behaviour hold regardless.

## Start the app

The JDK pin matters - Lombok's annotation processing silently fails on JDK 23,
which surfaces as `cannot find symbol: log / getBetId()` at compile time.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -s ~/.m2/settings-personal.xml spring-boot:run
```

The app listens on `http://localhost:8080` and seeds two jackpots on boot.

If responses look wrong, check for a stale instance holding the port:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## Swagger UI

Everything below can also be driven from the browser, which is usually easier
than curl for the `POST` bodies:

**http://localhost:8080/swagger-ui.html**

Expand an operation, click **Try it out**, edit the pre-filled example body, then
**Execute**. The raw OpenAPI document is at `/v3/api-docs`.

## Endpoints

| Method | Path | Purpose | Codes |
|---|---|---|---|
| `POST` | `/bets` | Publish a bet to the mocked `jackpot-bets` topic | `202`, `400`, `404`, `409` |
| `POST` | `/bets/{betId}/evaluate` | Evaluate a bet for a jackpot reward | `200`, `404` |
| `GET` | `/bets/{betId}` | Inspect a published bet (debug, not in spec) | `200`, `404` |
| `POST` | `/jackpots` | Create a jackpot | `201`, `400`, `409` |
| `GET` | `/jackpots` | List jackpots with current pool state | `200` |
| `GET` | `/jackpots/{jackpotId}` | Fetch a single jackpot | `200`, `404` |

---

## 0. Create a jackpot

```bash
curl -s -X POST localhost:8080/jackpots \
  -H 'Content-Type: application/json' \
  -d '{"jackpotId":"JP-DEMO","initialPoolAmount":50.00,
       "contributionType":"FIXED","fixedContributionPct":0.10,
       "rewardType":"FIXED","fixedRewardChance":1.0}'
```

`201`. A `fixedRewardChance` of `1.0` means every evaluation wins, which makes
the payout behaviour below deterministic.

Reusing an existing `jackpotId` returns `409`. Omitting a field the chosen type
requires returns `400` naming it — e.g. a `VARIABLE` contribution without
`contributionStepAmount`:

```json
{"status":400,"message":"Jackpot JP-BAD uses VARIABLE contribution but is missing contributionStepAmount"}
```

`FIXED` contribution needs `fixedContributionPct`; `VARIABLE` needs
`baseContributionPct`, `contributionDecayRate`, `minContributionPct` and
`contributionStepAmount`. `FIXED` reward needs `fixedRewardChance`; `VARIABLE`
needs `baseRewardChance`, `rewardGrowthRate`, `rewardStepAmount` and `poolLimit`.

---

## 1. List jackpots

```bash
curl -s localhost:8080/jackpots
```

`200`, both pools at their initial `50.00`:

```json
[{"jackpotId":"JP-VARIABLE","initialPoolAmount":50.00,"currentPoolAmount":50.00,"contributionType":"VARIABLE","rewardType":"VARIABLE"},
 {"jackpotId":"JP-FIXED","initialPoolAmount":50.00,"currentPoolAmount":50.00,"contributionType":"FIXED","rewardType":"FIXED"}]
```

## 2. Publish a bet

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"bet-1","userId":"user-1","jackpotId":"JP-FIXED","betAmount":100.00}'
```

`202`. Contributes 5% of `100.00`, so `JP-FIXED` moves `50.00` -> `55.00`.

```json
{"betId":"bet-1","status":"PUBLISHED","message":"Bet published to jackpot-bets"}
```

## 3. Republish the same betId

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"bet-1","userId":"user-1","jackpotId":"JP-FIXED","betAmount":100.00}'
```

`409 Conflict`. Without this the replay would contribute to the pool again while
overwriting its own contribution record, leaving the pool and the contribution
ledger disagreeing.

```json
{"status":409,"error":"Conflict","message":"A bet with id bet-1 has already been published."}
```

## 4. Confirm only one contribution landed

```bash
curl -s localhost:8080/jackpots
```

`JP-FIXED` is still `55.00`, not `65.00`, despite three publish attempts.

## 5. Inspect the published bet

```bash
curl -s localhost:8080/bets/bet-1
```

```json
{"betId":"bet-1","userId":"user-1","jackpotId":"JP-FIXED","betAmount":100.00,"receivedAt":"2026-09-18T16:28:53Z"}
```

## 6. Inspect a betId that was never published

```bash
curl -s localhost:8080/bets/does-not-exist
```

`404`:

```json
{"status":404,"error":"Not Found","message":"No bet found with id: does-not-exist"}
```

## 7. Evaluate the bet

```bash
curl -s -X POST localhost:8080/bets/bet-1/evaluate
```

`200`. `JP-FIXED` has a 10% win chance, so usually:

```json
{"betId":"bet-1","userId":"user-1","jackpotId":"JP-FIXED","won":false,"rewardAmount":null,"alreadyEvaluated":false,"message":"No win this time."}
```

## 8. Evaluate the same bet repeatedly

```bash
for i in 1 2 3 4 5; do curl -s -X POST localhost:8080/bets/bet-1/evaluate; echo; done
```

Every response matches the first, with `"alreadyEvaluated":true`. Losses are
recorded, not just wins - otherwise a losing bet could be retried until it won
(at a 10% chance, roughly 7 calls).

```json
{"betId":"bet-1","won":false,"rewardAmount":null,"alreadyEvaluated":true,"message":"This bet was already evaluated and did not win."}
```

## 9. A win pays the LIVE pool, not the pool at contribution time

This is the behaviour worth understanding. Using `JP-DEMO` from step 0 (10%
contribution, always wins), user A bets first and user B bets after them:

```bash
curl -s -X POST localhost:8080/bets -H 'Content-Type: application/json' \
  -d '{"betId":"userA-bet","userId":"userA","jackpotId":"JP-DEMO","betAmount":100.00}'
```

Pool is now `60.00` — A contributed `10.00`. Then user B:

```bash
curl -s -X POST localhost:8080/bets -H 'Content-Type: application/json' \
  -d '{"betId":"userB-bet","userId":"userB","jackpotId":"JP-DEMO","betAmount":100.00}'
```

Pool is now `70.00`. Evaluating **A** pays out the live `70.00`, not the `60.00`
the pool held when A contributed:

```bash
curl -s -X POST localhost:8080/bets/userA-bet/evaluate
```

```json
{"betId":"userA-bet","userId":"userA","jackpotId":"JP-DEMO","won":true,"rewardAmount":70.00,"alreadyEvaluated":false,"message":"Congratulations, this bet won the jackpot!"}
```

Winning the jackpot means taking what is in the pot now, including everything
contributed after your own bet.

## 9b. Guaranteed win via the pool limit

```bash
curl -s -X POST localhost:8080/bets -H 'Content-Type: application/json' \
  -d '{"betId":"big-1","userId":"user-2","jackpotId":"JP-VARIABLE","betAmount":10000.00}' && curl -s -X POST localhost:8080/bets/big-1/evaluate
```

`10000.00` on `JP-VARIABLE` contributes 10%, pushing the pool past its `1000.00`
limit, so `"won":true` is guaranteed regardless of prior state. The reward is the
live pool — `1050.00` on a fresh app, higher if other bets landed first.

## 10. Re-evaluate the winning bet

```bash
curl -s -X POST localhost:8080/bets/big-1/evaluate
```

Identical payout, `"alreadyEvaluated":true` - paid once, not twice.

## 11. Confirm the pool reset

```bash
curl -s localhost:8080/jackpots
```

`JP-VARIABLE` is back to its initial `50.00` after the win.

## 12. Validation failure

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"bad-1","jackpotId":"JP-FIXED","betAmount":-5}'
```

`400`, listing every field error:

```json
{"status":400,"error":"Bad Request","message":"betAmount: betAmount must be positive; userId: userId is required"}
```

## 13. Unknown jackpotId is rejected at publish

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"nomatch-1","userId":"u","jackpotId":"NOPE","betAmount":100.00}'
```

`404`. The jackpot is validated before anything is published, so a typo fails at
the point it was made rather than being accepted and silently contributing
nothing:

```json
{"status":404,"error":"Not Found","message":"No jackpot found with id: NOPE"}
```

## 14. Fetch a single jackpot

```bash
curl -s localhost:8080/jackpots/JP-DEMO
```

`200`, or `404` for an unknown id.

---

## Note on re-running

Bet IDs are unique per bet, so re-running these against the same instance will
return `409` from step 2 onward. Either restart the app or use fresh betIds.
