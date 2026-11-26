# Phone OTP Verification — Implementation Context
> Written: 2026-10-25 | Author: @antigravity

## Business Context
Provides phone number verification for Vietnamese individual customers before placing orders or updating user profiles. It prevents spam order placement and guarantees accurate contact information for order fulfillment.

## Technical Decisions
- **Redis for OTP state & rate-limiting**: All verification tokens, resend cooldowns, and wrong-attempt counters use Redis with short TTLs (`otp:phone:{phone}`, `otp:rate:{phone}`, `otp:attempts:{phone}`) to keep ephemeral auth state out of the relational database.
- **Dual persistence on success**: Upon successful OTP verification, the phone number is immediately persisted into the `User` entity in PostgreSQL (if authenticated), while a 10-minute temporary verification proof (`otp:verified:{phone}`) is retained in Redis for order placement validation.
- **Single-use OTP consumption**: OTP key and failed attempt counters are deleted immediately upon successful verification to prevent replay attacks.
- **Rate limiting, attempt caps, and daily quota**: Enforces a 60-second cooldown between resend requests (`otp:rate:{phone}`), locks out verification after 5 failed attempts (`otp:attempts:{phone}`), and enforces a daily cap (default 5 per phone/day via `otp:daily:{phone}` with 24h TTL) to prevent SMS bombing and budget depletion.
- **Pluggable SMS Gateway (`OtpSender` Strategy Pattern)**: Decoupled OTP generation and validation from physical transmission via the `OtpSender` abstraction:
  - `NoOpOtpSender`: Safe simulated logging for local/dev/test (`app.sms.enabled=false`).
  - `EsmsOtpSender`: Production transmission over eSMS.vn JSON Gateway (`app.sms.enabled=true`) using `RestClient`, supporting both Brandname (`SmsType = 2`) and dedicated fixed OTP numbers (`SmsType = 8`).
- **SecureRandom 6-digit generation**: Generates cryptographically secure 6-digit numeric codes with leading-zero formatting (`%06d`).

## Considered and Rejected
- **Storing OTP in PostgreSQL table**: Rejected because OTPs are short-lived (5 min TTL) high-frequency ephemeral data; Redis TTL is cleaner and prevents database bloat.
- **JWT-based phone verification token**: Rejected because server-side Redis key (`otp:verified:{phone}`) with TTL can be easily validated and consumed by `OrderService` atomically without extra cryptographic parsing overhead.
- **Hard-coding telco provider calls inside Service**: Rejected; using the `OtpSender` interface ensures clean separation of concerns and lets local tests run with zero external HTTP traffic.

## Dependencies
- Depends on: `feature/user` (`UserRepository`), `config` (`AppProperties`), `common/exception` (`BusinessRuleException`, `OtpVerificationException`), `common/util` (`AuditValueUtils`), `StringRedisTemplate`, `RestClient`.
- Depended by: `feature/order` (validates `isVerified` and calls `consumeVerification` upon order placement), `UserController` (profile phone updates).

## Known Limitations
- ⚠️ Telephony carrier delivery status callback: eSMS delivery confirmation webhook is not currently wired; delivery success is evaluated synchronously based on the gateway's immediate HTTP response `CodeResult`.

## Refactor Log

### 2026-11-27 | @antigravity
- Implemented Production SMS Gateway Integration & SMS Bombing Protection:
  - Created `OtpSender` abstraction in `feature/otp/sender` with two implementations:
    - `NoOpOtpSender`: active by default when `app.sms.enabled=false`, logs simulated OTP safely to console for dev/test.
    - `EsmsOtpSender`: active when `app.sms.enabled=true`, sends real SMS via eSMS.vn REST API using `RestClient`, supporting both custom Brandname (`SmsType = 2`) and fixed OTP number (`SmsType = 8`), with phone masking in audit logs.
  - Implemented Daily Cap Rate Limiting: Redis key `otp:daily:{phone}` with 24-hour TTL (86400s), enforcing maximum OTP sends per day per phone number (default 5, configurable via `app.sms.max-daily-per-phone`).
  - Updated `PhoneOtpServiceImpl` with `@Autowired` constructor injection for `OtpSender` and daily quota validation.
  - Extended `AppProperties` and `application.yaml` with `app.sms` configuration hierarchy.
  - Added comprehensive test coverage: 6 tests in `EsmsOtpSenderTest` and updated `PhoneOtpServiceImplTest` (all 499 tests passing).
