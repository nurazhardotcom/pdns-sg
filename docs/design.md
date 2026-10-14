# pdns-sg design

Engineering guidance for a privacy-by-design DNS data library. This is not legal advice. Statutory citations should be checked against the PDPA 2012 text before external use.

## 1. Scope

A Clojure library. It does not run a resolver, forward queries, or parse the DNS wire protocol. The caller runs the resolver and passes events to the library.

## 2. Data flows and obligations

| # | Flow | Library action | PDPA obligation |
|---|---|---|---|
| 1 | Query event in | `pdns.log` builds a record with a pseudonymised client token and a name at the configured granularity | Protection (s24), Purpose (s18) |
| 2 | Decision | `pdns.decision` returns allow or block with a reason | Accuracy (s23), via an allowlist and appeal path (caller) |
| 3 | Log store | Caller persists records. `pdns.retention` returns the purge set | Retention (s25) |
| 4 | Export | Aggregates only. No raw IP. No full name unless `:full` is opted in | Purpose (s18) |
| 5 | Subject access | Lookup by pseudonym using the caller's key | Access (s21), Correction (s22) |

Upstream forwarding is out of scope. Transfer (s26) is a caller responsibility: any upstream resolver outside Singapore needs a transfer record.

## 3. Design decisions

| ID | Decision | Rationale |
|---|---|---|
| D1 | Portfolio project, complements pdpa-sg-clj | Audience is recruiters and hiring managers |
| D2 | Clojure, `:local/root` reuse of pdpa rules | Same toolchain as pdpa-sg-clj |
| D3 | Query-name mode: `etld1` default, `full` opt-in, `hashed` keyed | Minimise by default; allow analysis only when justified |
| D4 | Retention default 7 days | Demo default. Not a legal recommendation |
| D5 | No upstream forwarding | Keeps the library free of network code |
| D6 | Library plus design doc, no service | Reviewable in minutes; avoids a half-built resolver |
| D7 | Raw client IP kept only for the record window, then purged | User choice. More retention than a pseudonym-only design; stated as a trade-off |
| D8 | Blocklist supplied in memory by the caller | Keeps the library pure |
| D9 | HMAC key caller-supplied, env helper for demo | A restart with a new key breaks token continuity, so the key must be managed by the operator |
| D10 | Design doc, no filled policy templates | No real operator exists |

## 4. Known trade-offs

- **Raw IP (D7).** The record holds a raw client IP for the window. Minimisation would drop it. The window is the same as the record, so exposure is bounded but real. Tests confirm the raw IP is dropped at purge.
- **Key rotation.** Rotating the HMAC key breaks token continuity. Counts across a rotation boundary are not comparable.
- **NAT.** Clients behind one NAT IP share a token.
- **eTLD+1 accuracy.** The bundled suffix list is a fixture. Production needs the full Public Suffix List. Wrong suffixes can merge unrelated domains.
- **Query names can be personal.** A subdomain can reveal a clinic or bank relationship. `etld1` is the default for this reason.

## 5. Known issues in pdpa-sg-clj

These are reported upstream, not fixed here.

1. README penalty wording ("over S$1M") is imprecise. Current penalty cap is up to 10% of annual Singapore turnover where turnover exceeds S$10M, otherwise up to S$1M (secondary sources; verify against the Act).
2. Checklist items 6, 9, 10, 11 present internal targets (90-day soft delete, 7-day withdrawal, 30-day individual notice, 3-year log retention) as obligations. Only s25 retention is statutory text read so far.
3. "Safe NRIC" wording says collection must stop. PDPC's 2 Feb 2026 announcement scopes the deadline to authentication use. Identification remains permitted under existing rules.
4. Phone redaction regex uses `;;` where a comment was intended. SG phone fixtures are not redacted.
5. Checklist markers arm only the next checkbox. Several sit after their boxes, so auto-tick is inert for those sections.

## 6. Verification gaps

- PDPA s26, s26B to s26D and s48J were not extracted from the statute page.
- PDPC primary pages were not readable. The NRIC deadline and GenAI guidance rely on secondary sources.
- The 13 Oct 2026 announcement the user referred to has not been identified.

## 7. Testing

27 tests, 49 assertions, run on Babashka and the JVM. All fixtures are fictional.
