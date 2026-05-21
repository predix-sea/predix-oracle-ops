# Architecture

## Overview

`predix-oracle-ops` is the resolution control plane for PrediX. It does **not** adjudicate outcomes directly; it collects auditable evidence, drives UMA Optimistic Oracle transactions, and syncs final results to `predix-market-schema`.

## Components

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ HTTP Sources│────▶│ Evidence Crawler │────▶│ oracle_evidences│
└─────────────┘     └──────────────────┘     └────────┬────────┘
                                                      │
┌─────────────┐     ┌──────────────────┐              ▼
│  Scheduler  │────▶│   Orchestrator   │────▶ resolution_jobs ──▶ RabbitMQ
└─────────────┘     └──────────────────┘              │
                         │                            ▼
                         │                    ┌───────────────┐
                         └───────────────────▶│ Job Executor  │
                                              └───────┬───────┘
                                                      │
                    ┌─────────────────────────────────┼────────────────────┐
                    ▼                                 ▼                    ▼
            ┌──────────────┐                  ┌──────────────┐      ┌──────────────┐
            │  UMA Client  │                  │ market-schema│      │matching-engine│
            │ (Web3j/Stub) │                  │    client    │      │   (optional)  │
            └──────────────┘                  └──────────────┘      └──────────────┘
```

## Package layout

| Package | Responsibility |
|---------|----------------|
| `crawler` | HTTP fetch, evidence persistence, aggregation |
| `oracle` | UMA chain abstraction (stub + Web3j) |
| `service` | Jobs, orchestration, state machine, retry |
| `scheduler` | Cron scans & retries |
| `mq` | RabbitMQ publish/consume, DLQ |
| `client` | market-schema / matching-engine HTTP |
| `audit` | Append-only resolution audits |
| `repository` | JPA entities |

## Infrastructure

- **PostgreSQL**: sources, evidences, jobs, uma_transactions, audits
- **Redis**: distributed locks, MQ idempotency keys
- **RabbitMQ**: async job execution & DLQ

## Security

- Admin mutations require `X-Oracle-Admin-Token`
- All manual triggers recorded in `resolution_audits`
- Evidence rows are append-only (no silent overwrite)
