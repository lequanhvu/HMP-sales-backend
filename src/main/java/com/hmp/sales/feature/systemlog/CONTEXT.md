# System Audit Log — Implementation Context
> Written: 2026-10-25 | Author: @antigravity

## Business Context
Provides business and security audit trail recording for sensitive administrative actions (role permission updates, user locking/unlocking, manager provisioning, system settings modifications, catalog modifications, staff order processing, and security events) to satisfy accountability and traceability requirements.

## Technical Decisions
- **Interface + PostgreSQL Implementation (Phase 5)**: `SystemLog` entity (table `system_logs`) persisted directly into PostgreSQL as an append-only audit trail.
- **Transactional Consistency (Decision Confirmed)**: `log` and `logWithChange` run in the same transaction as the calling business operation (`REQUIRED`), ensuring strict audit integrity (rollback together). `logSecurityEvent` runs in an independent transaction (`REQUIRES_NEW`) to record security incidents (e.g. locked account login, refresh token reuse) without impacting the caller.
- **Masking & Sanitization**: `AuditValueUtils` automatically strips forbidden keys (passwords, tokens, OTPs, secrets) and masks phone numbers / emails before serializing `oldValue`/`newValue`.
- **Query & Specifications**: `SystemLogSpecification` supports flexible filtering by actor role, activity, date range, unaccented name, phone, and email with standard pagination.

## Endpoints
- `GET /api/v1/admin/system-logs`: Search audit logs with pagination and filters (permission `SYSTEM_LOG_VIEW`).
- `GET /api/v1/admin/system-logs/{logId}`: Retrieve detailed audit log including before/after values.

## Dependencies
- PostgreSQL `system_logs` table.
- Injected across feature services: `user`, `setting`, `category`, `product`, `order`, `auth`.

## Refactor Log
- **2026-11-08**: Phase 5 completed. Implemented `SystemLog` entity, `SystemLogRepository`, `SystemLogSpecification`, `SystemLogServiceImpl`, and `AdminSystemLogController`. Wired audit logging across all administrative and staff actions.
