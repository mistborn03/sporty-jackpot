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

## Endpoints

| Method | Path | Purpose | Codes |
|---|---|---|---|
| `POST` | `/bets` | Publish a bet to the mocked `jackpot-bets` topic | `202`, `400`, `409` |
| `POST` | `/bets/{betId}/evaluate` | Evaluate a bet for a jackpot reward | `200`, `404` |
| `GET` | `/bets/{betId}` | Inspect a published bet (debug, not in spec) | `200`, `404` |
| `GET` | `/jackpots` | Inspect jackpot pool state (debug, not in spec) | `200` |

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

## 9. Guaranteed win

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"big-1","userId":"user-2","jackpotId":"JP-VARIABLE","betAmount":10000.00}'
```

Then:

```bash
curl -s -X POST localhost:8080/bets/big-1/evaluate
```

`10000.00` on `JP-VARIABLE` contributes 10%, pushing the pool past its `1000.00`
limit, so `"won":true` is guaranteed regardless of prior state. The reward equals
the pool snapshot at contribution time - `1050.00` on a fresh app, higher if the
pool had already grown (the contribution rate decays as it does).

```json
{"betId":"big-1","userId":"user-2","jackpotId":"JP-VARIABLE","won":true,"rewardAmount":1050.00,"alreadyEvaluated":false,"message":"Congratulations, this bet won the jackpot!"}
```

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

## 13. Unknown jackpotId - accepted at publish

```bash
curl -s -X POST localhost:8080/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"nomatch-1","userId":"u","jackpotId":"NOPE","betAmount":100.00}'
```

`202`. Publish does not check the jackpot exists - that happens in the consumer,
preserving the producer/consumer decoupling a real Kafka setup would have.

## 14. ...but it never contributed, so it cannot be evaluated

```bash
curl -s -X POST localhost:8080/bets/nomatch-1/evaluate
```

`404`:

```json
{"status":404,"message":"No contribution found for bet nomatch-1 - the bet may not exist, or its jackpotId did not match any known jackpot."}
```

## 15. The bet itself is still retrievable

```bash
curl -s localhost:8080/bets/nomatch-1
```

`200`. This is how you tell "never arrived" from "arrived but matched no
jackpot" - the 404 above is ambiguous on its own.

---

## Note on re-running

Bet IDs are unique per bet, so re-running these against the same instance will
return `409` from step 2 onward. Either restart the app or use fresh betIds.
