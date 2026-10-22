# Source Diff: 0.2.4 vs 0.1.4 (`src/main`)

Module-by-module comparison of `src/main` content between the two newest tags, matching modules by **artifactId/path name**:

| Tag | Maven version (`revision`) | Resilience4j supported range | Java | Namespace |
|---|---|---|---|---|
| `0.2.4` | `0.2.4-SNAPSHOT` | 2.0.0 / 2.1.0 / 2.2.0 / 2.3.0 / 2.4.0 (Maven profiles) | 17 | `jakarta.*` |
| `0.1.4` | `0.1.4-SNAPSHOT` | 1.3.0 / 1.4.0 / 1.5.0 / 1.6.0 / 1.7.0 (Maven profiles) | 8 | `javax.*` |

Excluded by design: Java 8 ↔ Java 17 syntax/API differences, Java EE ↔ Jakarta EE namespace differences, and modules that exist on only one side (ignored, not reported as removed/added).

## Module Overview

| Module (`src/main`) | 0.1.4 files | 0.2.4 files | Matched | Identical | Modified | Only in 0.1.4 | Only in 0.2.4 |
|---|---|---|---|---|---|---|---|
| `microsphere-resilience4j-commons` | 17 | 17 | 17 | 12 | 5 | 0 | 0 |
| `microsphere-resilience4j-spring` | 31 | 30 | 30 | 27 | 3 | 1 | 0 |
| `microsphere-resilience4j-plugins/alibaba-druid` | 3 | 3 | 3 | 3 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/hibernate-core` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/mybatis` | 4 | 4 | 4 | 4 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/openfeign` | 4 | 4 | 4 | 4 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/spring-web` | 3 | 3 | 3 | 3 | 0 | 0 | 0 |
| `-parent` / `-dependencies` / `-spring-boot` / `-spring-cloud` | 0 | 0 | 0 | — | — | — | — |
| **Matched total** | **63** | **62** | **62** | **54** | **8** | **1** | **0** |
| `microsphere-resilience4j-compatible` | 6 | — | *ignored* | — | — | — | — |

- `-parent`, `-dependencies`, `-spring-boot`, `-spring-cloud` are POM-only (parent/BOM/aggregator) modules; they have no `src/main`.
- `microsphere-resilience4j-compatible` has no 0.2.4 counterpart, so it is **ignored** per the rules. Context for the reader: it existed only to inject Resilience4j 2.x event types (`CircuitBreakerEvent`, `CircuitBreakerOnFailureRateExceededEvent`, `CircuitBreakerOnSlowCallRateExceededEvent`, `RateLimiterEvent`, `RateLimiterOnDrainedEvent`) into a 1.x runtime; on 2.x those types are native, so the shim is unnecessary.

## Why the two lines differ

`microsphere-resilience4j-parent` declares one Maven profile per supported Resilience4j version. Source in `commons`/`spring` therefore has to compile against the **lowest** version of its line (1.3.0 for `0.1.x`, 2.0.0 for `0.2.x`). Several API differences verified in the published jars:

| API | 1.4.0 | 1.6.1 | 1.7.0 | 2.0.2 | Impact |
|---|---|---|---|---|---|
| `CircuitBreaker.getCurrentTimestamp()` / `getTimestampUnit()` | ✘ | ✔ | ✔ | ✔ | 0.1.x could not use the entry clock (1.3 floor) |
| `CircuitBreaker.onResult(long, TimeUnit, Object)` | ✘ | ✘ | ✔ | ✔ | same as above |
| `RateLimiter.onSuccess()` / `onError()` / `onResult()` | ✘ | ✘ | ✔ | ✔ | same as above |
| `decorateCheckedSupplier(...)` return type | `io.vavr.CheckedFunction0` | `io.vavr.CheckedFunction0` | `io.vavr.CheckedFunction0` | `io.github.resilience4j.core.functions.CheckedSupplier` | `.apply()` → `.get()` |
| `Registry.getTags()` value type | `io.vavr.collection.Map` | `io.vavr.collection.Map` | `io.vavr.collection.Map` | `java.util.Map` | vavr dropped in 2.x |
| `io.github.resilience4j.common.*.configuration.*ConfigurationProperties` | unprefixed | unprefixed | unprefixed | `Common`-prefixed | class rename in 2.x `resilience4j-framework-common` |
| `CircuitBreakerConfiguration` `@Import({FallbackConfiguration, SpelResolverConfiguration})` | ✘ | ✘ | ✘ | ✔ | 0.1.x had to register those beans manually |

## `microsphere-resilience4j-commons` (src/main)

| File | Implementation change | Reason |
|---|---|---|
| `bulkhead/BulkheadTemplate.java` | `registry.bulkhead(name, configuration)` → `registry.bulkhead(name, configuration, registry.getTags())` | **Feature parity / functional need.** The 3-arg overload exists in 1.x too, but its `tags` parameter is a `io.vavr.collection.Map`; 2.x switched tags to `java.util.Map`, so the template can now forward registry-level tags without pulling in vavr. Effect: `Bulkhead` instances created through the template inherit registry tags. |
| `circuitbreaker/CircuitBreakerTemplate.java` | `call(...)`: `decorateCheckedSupplier(callback::get).apply()` → `.get()` | **API compatibility.** 2.x dropped vavr and returns its own `CheckedSupplier`, whose single-abstract method is `get()`. |
| `circuitbreaker/CircuitBreakerTemplate.java` | `doBegin`: `System.nanoTime()` → `circuitBreaker.getCurrentTimestamp()`; `doEnd`: duration = `getCurrentTimestamp() - startTime`, unit = `circuitBreaker.getTimestampUnit()` instead of the literal `NANOSECONDS` | **API compatibility + correctness.** The clock/unit pair is only available from 1.6 onwards (above the 0.1.x floor), and it is the right source of truth: metrics are now reported in the unit the `CircuitBreaker` itself uses instead of a hard-coded one. |
| `circuitbreaker/CircuitBreakerTemplate.java` | `doEnd`: when `context.getResult() != null` call `onResult(duration, unit, result)`, else `onSuccess(duration, unit)` | **New capability (result-based recording).** `onResult` is available from 1.7/2.x only; it lets the circuit breaker record by return value, which the `Resilience4jContext.result` field already carried but could not feed back under the 1.3 floor. |
| `common/Resilience4jModule.java` | All six property classes rebound: `Retry/CircuitBreaker/RateLimiter/TimeLimiter/Bulkhead/ThreadPoolBulkhead ConfigurationProperties` → `Common…ConfigurationProperties` | **API compatibility.** 2.x renamed the framework-neutral property holders in `resilience4j-framework-common` and reserved the unprefixed names for Boot-specific subclasses. The enum keys the module→properties mapping, so it must follow the rename. |
| `ratelimiter/RateLimiterTemplate.java` | `call(...)`: `.apply()` → `.get()` | **API compatibility.** Same vavr → `CheckedSupplier` change. |
| `ratelimiter/RateLimiterTemplate.java` | `doEnd(...)`: dropped the second `waitForPermission(rateLimiter)` call; now reports the outcome via `onSuccess()` / `onResult(result)` / `onError(failure)` | **Functional need + API floor.** `doBegin` already acquires the permission, so calling `waitForPermission` again in `doEnd` was a 1.x-era workaround for outcome signalling. With 2.x (and per the table above, ≥1.7) the `RateLimiter` exposes explicit `onSuccess/onError/onResult`, which is what success/failure/drain events and metrics depend on. |
| `retry/RetryTemplate.java` | `call(...)`: `.apply()` → `.get()` | **API compatibility.** Same vavr → `CheckedSupplier` change. |

Unchanged (12 files): `ThreadPoolBulkheadTemplate`, `TimeLimiterTemplate`, `Resilience4jTemplate`, `Resilience4jFacade`, `AdvancedResilience4jOperations`, `Resilience4jOperations`, `ChainableResilience4jFacade`, `CallbackChain`, `Resilience4jContext`, `Resilience4jConstants`, `util/Resilience4jUtils`, `resources/META-INF/default/templates.properties`.
`TimeLimiter`/`ThreadPoolBulkhead` templates need no edit because they use decorator signatures whose shapes did not change between 1.x and 2.x.

## `microsphere-resilience4j-spring` (src/main)

| File | Implementation change | Reason |
|---|---|---|
| `bulkhead/annotation/EnableBulkhead.java` | Thread-pool-bulkhead binding type `ThreadPoolBulkheadConfigurationProperties` → `CommonThreadPoolBulkheadConfigurationProperties` (import updated) | **API compatibility** — 2.x class rename. The `resilience4j.bulkhead.*` binding keeps the Boot-side `BulkheadConfigurationProperties` (present in both 1.7 and 2.0), only the framework-common holder was renamed. |
| `bulkhead/annotation/EnableThreadPoolBulkhead.java` | Same rename (import + binding type) | **API compatibility** — as above. |
| `common/annotation/EnableResilience4jRegistrar.java` | Removed the `registerCommonConfigurations(registry)` step and its static import from the registration sequence | **Functional need.** In 1.x, `CircuitBreakerConfiguration` did not import `FallbackConfiguration`/`SpelResolverConfiguration`, so the registrar had to register them itself (`FallbackConfiguration` directly, `SpelResolverConfiguration` reflectively when the optional module was present). In 2.x `CircuitBreakerConfiguration` carries `@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})`, so the manual step is redundant. |
| `common/Resilience4jUtils.java` | **Deleted** (only in 0.1.4) | Follows directly from the row above: the class held nothing but `registerCommonConfigurations` / `registerFallbackConfiguration` / `registerSpelResolverConfiguration` plus the `SPEL_RESOLVER_CONFIGURATION_CLASS_NAME` constant. Obsolete under 2.x. |

Unchanged (27 files): `LazyResilience4jFacade`, all remaining `Enable*` annotations and `*Registrar` classes, all `*ApplicationEventPublisher` / `*EventConsumerBeanRegistrar` classes, `common/event/EventConsumerMethodFilter` and `Resilience4jEvent*` bases, `common/Resilience4jPlugin`, `resources/META-INF/spring.factories`.

## `microsphere-resilience4j-plugins/*` (src/main)

All five plugins — `alibaba-druid`, `hibernate-core`, `mybatis`, `openfeign`, `spring-web` — are **byte-identical** across the two tags, including their `META-INF/spring.factories`.

Cause: the plugins are written against this project's own abstractions (`Resilience4jTemplate`, `ChainableResilience4jFacade`) plus third-party SPIs (Druid `Filter`, MyBatis `Interceptor`, Feign `Capability`/`Client`, Spring Web `HandlerMethodInterceptor`, JPA `EntityCallback`). The Resilience4j 1.x→2.x break is fully absorbed by `commons` and `spring`, and the `javax`→`jakarta` switch is delegated to the parent POM plus the microsphere druid/mybatis/hibernate BOMs, so no plugin source edit was required.

## Summary

The whole `src/main` delta reduces to three causes:

1. **API compatibility (6 of 8 modified files)** — `Common*ConfigurationProperties` rename; `.apply()` → `.get()` from the vavr removal; entry clock/unit and `onResult` now reachable because the version floor moved from 1.3 to 2.0.
2. **New / corrected behaviour** — registry tags forwarded to `Bulkhead`; rate-limiter `doEnd` reports `onSuccess`/`onResult`/`onError` instead of waiting for permission twice.
3. **Removal of 1.x scaffolding** — the `-compatible` module (ignored here), the manual `FallbackConfiguration`/`SpelResolverConfiguration` registration and its `Resilience4jUtils` helper.

No Java 8/17 or `javax`/`jakarta` source-level differences were found in the compared files: the language level and namespace split lives entirely in the parent POM and the imported BOMs.
