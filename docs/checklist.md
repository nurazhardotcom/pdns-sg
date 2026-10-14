# PDPA obligation checklist for pdns-sg

Scope: this library. It does not run a resolver. Each row says what this repo provides and who must provide the rest. Not legal advice. Statutory references should be checked against the Act (see issue #2).

Status values:
- **Library**: implemented in this repo and tested.
- **Partial**: the library provides a building block; the caller must complete it.
- **Caller**: outside this library. The operator must provide evidence.

| # | Obligation | Status | What this repo provides | Caller must provide |
|---|---|---|---|---|
| 1 | Consent | Caller | Nothing | Notice and consent at service onboarding |
| 2 | Purpose limitation | Partial | `pdns.log` stores eTLD+1 by default, not full names | Document the purpose of each stored field |
| 3 | Notification | Caller | Nothing | Privacy notice to clients |
| 4 | Accuracy | Partial | `pdns.decision` returns a reason and domain for each block | Allowlist and appeal path for false positives |
| 5 | Protection | Partial | HMAC pseudonymisation of client IPs; no raw IP unless opted in; key never stored | Encryption at rest, access control, key custody, logging of access |
| 6 | Retention | Partial | `pdns.retention/partition-by-age` returns the purge set and drops raw IP with the record | Schedule that runs the purge; the 7-day default is a demo value, not a recommendation |
| 7 | Transfer (s26) | Caller | Nothing | Record of any upstream resolver outside Singapore |
| 8 | Access and correction | Partial | Tokens are reproducible with the caller's key, so records can be looked up | Identity check, response process, timing |
| 9 | Withdrawal of consent | Caller | Nothing | Withdrawal channel and downstream stop |
| 10 | Breach notification | Caller | Nothing | Breach plan covering log store and key custody |
| 11 | Accountability / DPO | Caller | Nothing | Named DPO and published contact |

## Checks the library enforces (tested)

- `full` names require an explicit opt-in (`pdns.config`, `pdns.log` tests).
- Client IPs must be valid IPv4 or IPv6 literals; malformed input is refused, not hashed (`pdns.pseudonym`).
- Raw IPs are excluded from records unless `:keep-raw-ip?` is set (`pdns.log`).
- Retention boundary: a record exactly at the window is purged (`pdns.retention`).

## Not claimed

- No auto-tick. The library does not mark any obligation as satisfied. pdpa-sg-clj's checklist auto-tick is not used here because the scanner cannot verify these items for a DNS service.
- No evidence of operation. Tests show the functions behave as specified, not that an operator runs them.
