# microsphere-resilience4j-compatible — User Guide (1.x)

> Version line: `0.1.x` (source branch `release-1.x`) · latest release `0.1.4` · branch revision `0.1.5-SNAPSHOT`
> Compatible with **Resilience4j 1.3.0 – 1.6.0** (this is the only module that exists to support the pre-1.7 profiles)
> A build-time **compatibility shim** — forked Resilience4j event classes placed under the original `io.github.resilience4j` package names.
> ← [guide index](README.md) · [Developer Guide page](../developer-guide/microsphere-resilience4j-compatible.md)

---

## 1. What this module is

`microsphere-resilience4j-compatible` is a tiny support jar that exists only for the `release-1.x` (0.1.x) line.
It re-publishes a handful of Resilience4j **event** classes under their original
`io.github.resilience4j.*.event` package names. According to the module's own `package-info.java`, these classes
are *"forked from the Resilience4j 1.7.x (latest), which will be used to be compatible with the lower versions of
Resilience4j."*

The reason `microsphere-resilience4j-commons` needs them: its circuit-breaker and rate-limiter templates expose
event-subscription methods typed against event classes (`CircuitBreakerOnFailureRateExceededEvent`,
`CircuitBreakerOnSlowCallRateExceededEvent`, `RateLimiterOnDrainedEvent`) and against event-type enums that gained
those constants only in later 1.x releases. When commons is compiled against an older Resilience4j, these types are
missing from the upstream jars; the shim supplies them so **the same commons sources compile against every release
from 1.3 to 1.7**.

The package-info blockquote, verbatim in intent:

> The classes in this package or sub-packages are forked from the Resilience4j 1.7.x(latest), which will be used
> to be compatible with the lower versions of Resilience4j.

---

## 2. When it is added automatically

You almost never add it yourself. `microsphere-resilience4j-commons/pom.xml` declares it as a dependency inside
four build profiles:

| Build profile (`-P`) | Resilience4j version | compatible shim attached? |
|----------------------|----------------------|---------------------------|
| `resilience4j-1.3` | 1.3.0 | yes |
| `resilience4j-1.4` | 1.4.0 | yes |
| `resilience4j-1.5` | 1.5.0 | yes |
| `resilience4j-1.6` | 1.6.0 | yes |
| `resilience4j-1.7` *(activeByDefault)* | 1.7.0 | **no** — 1.7.0 already ships these event types |

So:

- Building commons with the **default** profile (`resilience4j-1.7`) does **not** pull the shim.
- Building commons with `-P resilience4j-1.3 … 1.6` **does** pull the shim automatically — it is injected as a
  `<dependency>` of commons by the profile, with version `${revision}`.

```xml
<!-- present in microsphere-resilience4j-commons/pom.xml, inside each of resilience4j-1.3 … resilience4j-1.6 -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-compatible</artifactId>
    <version>${revision}</version>
</dependency>
```

The shim is a reactor sibling module (`microsphere-resilience4j-compatible`) and is listed in the root
`pom.xml` `<modules>`; it is **not** managed in the `microsphere-resilience4j-dependencies` BOM and it is **not
deployed** (`maven-deploy-plugin` is configured with `<skip>true</skip>` in its own `pom.xml`). It is a build-time
artifact, nothing more.

---

## 3. Should my application depend on it directly?

**No — normally never.** The module carries no public API of its own; it only back-ports Resilience4j types. You
should not add a `<dependency>` on `microsphere-resilience4j-compatible`. It reaches your classpath only transitively,
through commons, and only when you deliberately build commons against Resilience4j 1.3–1.6. For a normal application
that either targets Resilience4j 1.7 (default) or consumes the published commons jar, the shim is simply not present.

---

## 4. Class-by-class inventory

Every class in the module lives under a real Resilience4j package name (the deliberate package collision that makes
the shim "shadow-compatible"). There are **five event classes** plus `package-info.java`:

| Class (fully-qualified) | Kind | Purpose |
|-------------------------|------|---------|
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent` | interface | The circuit-breaker event contract: `getCircuitBreakerName()`, `getEventType()`, `getCreationTime()`, plus the `Type` enum. The forked copy declares the full 1.7.x `Type` set — including `FAILURE_RATE_EXCEEDED`, `SLOW_CALL_RATE_EXCEEDED`, `FORCED_OPEN`, `DISABLED` — with a `forcePublish` flag on `STATE_TRANSITION` and `RESET`. |
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnFailureRateExceededEvent` | class | Event fired when the circuit breaker breaches its failure-rate threshold; carries `getFailureRate()` and reports `Type.FAILURE_RATE_EXCEEDED`. Referenced by `CircuitBreakerTemplate.onFailureRateExceededEvent(...)`. |
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSlowCallRateExceededEvent` | class | Event fired when the slow-call-rate threshold is breached; carries `getSlowCallRate()` and reports `Type.SLOW_CALL_RATE_EXCEEDED`. Referenced by `CircuitBreakerTemplate.onSlowCallRateExceededEvent(...)`. |
| `io.github.resilience4j.ratelimiter.event.RateLimiterEvent` | interface | The rate-limiter event contract: `getRateLimiterName()`, `getEventType()`, `getNumberOfPermits()`, `getCreationTime()`, plus `Type { FAILED_ACQUIRE, SUCCESSFUL_ACQUIRE, DRAINED }`. The forked copy adds the `DRAINED` constant used by the drained-event below. |
| `io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent` | class | Event fired when the rate limiter's permission waiters are drained; reports `Type.DRAINED` and `getNumberOfPermits() == 1`. Referenced by `RateLimiterTemplate.onDrainedEvent(...)`. |

Two things to notice about the concrete event classes:

- `CircuitBreakerOnFailureRateExceededEvent` / `CircuitBreakerOnSlowCallRateExceededEvent` extend
  `AbstractCircuitBreakerEvent`, and `RateLimiterOnDrainedEvent` extends `AbstractRateLimiterEvent`. Neither abstract
  base is re-forked here — they come from the real `resilience4j-circuitbreaker` / `resilience4j-ratelimiter` jars,
  which is possible precisely because the shim classes share the same package (see the developer guide).
- `RateLimiterOnDrainedEvent` in this fork exposes `getNumberOfPermits()` returning a fixed `1`, matching the 1.7.x
  behaviour it is back-porting.

The module declares the Resilience4j artifacts it needs as `<optional>` compile dependencies (`resilience4j-circuitbreaker`,
`resilience4j-ratelimiter`, `resilience4j-core`, `resilience4j-framework-common`, and the rest of the family) so that
the abstract event bases and `ZonedDateTime`-style types resolve during compilation without forcing versions downstream.

---

## 5. Build command example and classpath effect

Building commons against Resilience4j **1.4.0** (a pre-1.7 release that lacks the drained/slow-call event types):

```bash
./mvnw -pl microsphere-resilience4j-commons -am clean install -P resilience4j-1.4
```

Effect on the classpath:

1. The parent profile `resilience4j-1.4` sets `resilience4j.version` to `1.4.0`, so all `io.github.resilience4j:*`
   artifacts resolve to `1.4.0`.
2. The commons profile `resilience4j-1.4` activates and adds a dependency on
   `io.github.microsphere-projects:microsphere-resilience4j-compatible:0.1.5-SNAPSHOT` (`${revision}`).
3. The shim jar lands on the compile/runtime classpath **before** the 1.4.0 Resilience4j event classes can be a
   problem, supplying `CircuitBreakerEvent` (with `FAILURE_RATE_EXCEEDED` / `SLOW_CALL_RATE_EXCEEDED`),
   `CircuitBreakerOnFailureRateExceededEvent`, `CircuitBreakerOnSlowCallRateExceededEvent`, `RateLimiterEvent`
   (with `DRAINED`) and `RateLimiterOnDrainedEvent` under the original `io.github.resilience4j.*.event` names.
4. The single source tree — `CircuitBreakerTemplate`, `RateLimiterTemplate`, the tests that reference these types —
   compiles and runs unchanged.

Compare with the default:

```bash
./mvnw -pl microsphere-resilience4j-commons -am clean install          # profile resilience4j-1.7 (activeByDefault)
```

Here `resilience4j.version` is `1.7.0`, the commons shim profile does **not** activate, and the real 1.7.0
Resilience4j jars already contain every event type — so `microsphere-resilience4j-compatible` is **absent** from
the classpath entirely.

---

## 6. Difference from the 0.2.x line

The `compatible` module does **not exist** on the `0.2.x` line. That line targets Resilience4j 2.x, where the event
model is stable across the supported range and no back-port shim is required. If you are maintaining a `0.2.x`
application, this page and this module are irrelevant to you; use the [current guide](../../current/user-guide/README.md).

---

Chinese translations of every page live in `zh/` with identical file names.
