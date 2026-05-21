# Resolution Flow

## State machine (minimal)

| Step | Market status | Action | Next |
|------|---------------|--------|------|
| 1 | CLOSED | Collect evidence | Quorum check |
| 2 | CLOSED/RESOLVING | UMA `REQUEST` | RESOLVING |
| 3 | RESOLVING | UMA `PROPOSE` | Wait window |
| 4 | RESOLVING | Disputed? → `DISPUTE` | Branch |
| 5 | RESOLVING | UMA `SETTLE` | RESOLVED |
| 6 | RESOLVED | Write winning outcome to market-schema | Done |

## Re-entrancy & idempotency

- Each phase uses `idempotency_key = {marketId}:{JOB_TYPE}`.
- Running/succeeded jobs block duplicates (`JOB_IDEMPOTENCY_CONFLICT`).
- Redis `SET NX` prevents duplicate MQ consumption per key (24h TTL).
- Scheduler locks: `scheduler:market-scan`, `scheduler:retry`.

## Failure handling

1. Job fails → `FAILED` or `RETRYING` with `next_retry_at`.
2. Scheduler re-enqueues retryable jobs.
3. After `max-attempts` → `DEAD` + DLQ message.

## Manual operations

| Endpoint | Effect |
|----------|--------|
| `POST /jobs/trigger/request` | Force REQUEST pipeline |
| `POST /jobs/trigger/propose` | Force PROPOSE |
| `POST /jobs/trigger/settle` | Force SETTLE |
| `POST /jobs/{id}/retry` | Re-queue specific job |

All manual calls require admin token and write audit rows (`actor_type=MANUAL`).

## Dispute branch

When assertion status reports `disputed=true`, orchestrator enqueues `DISPUTE` job. Dispute does not directly resolve the market; final outcome still follows UMA settlement.
