# PrediX Oracle Ops ↔ CTF mapping (Phase 0 sketch)

> Companion to [`predix-ctf-contracts/docs/architecture.md`](../../predix-ctf-contracts/docs/architecture.md).  
> Full Adapter implementation lands in Phase 2 (`PredixUmaCtfAdapter` + settle → `reportPayouts`).  
> Indexer event aliases: `blockchain-lottery-java/docs/predix-ctf-uma-mapping.md`.

## 1. Purpose

`predix-oracle-ops` drives UMA Optimistic Oracle jobs (`REQUEST` → `PROPOSE` → `DISPUTE` → `SETTLE`) and writes the final outcome to `predix-market-schema`.

After CTF integration, **SETTLE must also unlock CTF redemption** via the on-chain adapter calling ConditionalTokens `reportPayouts`. Until Phase 2 ships, this service remains market-schema–centric with **no CTF contract calls**.

## 2. Lifecycle mapping

| oracle-ops job / state | UMA OO | CTF / Adapter | User impact |
|------------------------|--------|---------------|-------------|
| Evidence + quorum | — | — | Off-chain only |
| `REQUEST` | Request price / assert | `questionId` already prepared (or prepare-on-request) | Condition exists; no payout |
| `PROPOSE` | Propose outcome | — | Bond locked on OO |
| Waiting liveness | Proposed | — | Challenge window |
| `DISPUTE` | Dispute | — | Still no `reportPayouts` |
| `SETTLE` | Settle assertion | Adapter → `reportPayouts(questionId, payouts)` | Winners can `redeem` |
| market-schema `RESOLVED` | — | — | Off-chain market closed |

Binary MVP payouts: `[1, 0]` (Yes) or `[0, 1]` (No) in CTF units after settle.

## 3. Field mapping

| Field | Produced by | Consumed by | Notes |
|-------|-------------|-------------|-------|
| `marketId` | market-schema | oracle-ops, matching, gateway | Off-chain key |
| `questionId` (`bytes32`) | market create / adapter `prepareMarket` | CTF `prepareCondition`, OO ancillary | Stable per market |
| `conditionId` (`bytes32`) | CTF `getConditionId(oracle, questionId, 2)` | Indexer, gateway, frontend | `oracle` = Adapter address |
| `assertionId` | UMA OO tx receipt | `UmaTransactionService` | Link settle → payouts |
| `chainId` | config (`UMA_CHAIN_ID`, default `137`) | `uma_transactions` | Use `80002` on Amoy |
| `outcomeSlotCount` | fixed `2` | CTF | Binary Yes/No |

Suggested persistence (Phase 2+): store `questionId` / `conditionId` on market-schema or a small `oracle_market_ctf` table keyed by `marketId`.

## 4. Code touchpoints (no code change in Phase 0)

| Component | Path | Future change |
|-----------|------|---------------|
| Job executor | `ResolutionJobExecutor` | After successful `SETTLE`, invoke Adapter or enqueue CTF report job |
| UMA client | `UmaClient` / `Web3jUmaClient` | Replace stub UUID txs with real OO encoding |
| Tx audit | `UmaTransactionService` | Persist `assertionId` + optional `conditionId` |
| Config | `application.yml` `predix.oracle.uma.*` | Add `ctf-adapter-address`, Amoy `chain-id` |

## 5. Amoy vs mainnet

| Env | `chain-id` | Notes |
|-----|------------|-------|
| Polygon Amoy | `80002` | Integration; POL faucet |
| Polygon mainnet | `137` | Current default in `application.yml` |

Do not point production OO addresses at Amoy adapter deployments.

## 6. Out of scope here

- Implementing `PredixUmaCtfAdapter.sol`
- Changing bond-token mint policy
- Full dispute game UX

Phase 0 deliverable: this sketch + architecture cross-links only.
