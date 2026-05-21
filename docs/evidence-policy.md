# Evidence Policy

## Principles

1. **Traceability**: Every outcome proposal must reference stored evidence rows.
2. **Immutability**: Evidence is append-only; corrections require new rows.
3. **Multi-source**: At least `min-sources` (default 2) independent fetches before auto-request.
4. **Quorum**: Winning outcome must meet `quorum-ratio` (default 51%) agreement.

## Source configuration

Sources are registered in `oracle_sources` with:

- `priority` — fetch order (lower first)
- `enabled` — participate in collection
- `base_url` — HTTP template; `?marketId=` appended

## Aggregation algorithm

1. Group evidences by `normalized_outcome_code`.
2. Pick plurality winner.
3. Compute `agreementRatio = winnerCount / total`.
4. Flag `requiresManualReview` when:
   - `total < min-sources`
   - `agreementRatio < quorum-ratio`
   - Tie on top counts

## Audit binding

On `REQUEST`, audit `input_snapshot` includes aggregated `evidenceOutcome`. On-chain `tx_hash` and `assertionId` stored in `output_snapshot`.

## Hash digest

`hash_digest = SHA-256(marketId|sourceId|raw_payload)` for integrity verification and dedup reference.
