# Source Diff: 0.2.4 vs 0.1.4 (`src/test`)

Module-by-module comparison of `src/test` content between the two newest tags, matching modules by **artifactId/path name**. `0.2.4` = Resilience4j 2.0.0–2.4.0 line, `0.1.4` = Resilience4j 1.3.0–1.7.0 line.

Excluded by design: Java 8 ↔ Java 17 syntax/API differences and Java EE ↔ Jakarta EE namespace differences. The only language-level change found is listed separately and marked as such. Modules present on only one side are ignored.

## Module Overview

| Module (`src/test`) | 0.1.4 files | 0.2.4 files | Matched | Identical | Modified | Only in 0.1.4 | Only in 0.2.4 |
|---|---|---|---|---|---|---|---|
| `microsphere-resilience4j-commons` | 13 | 14 | 12 | 7 | 5 | 1 | 2 |
| `microsphere-resilience4j-spring` | 14 | 13 | 13 | 6 | 7 | 1 | 0 |
| `microsphere-resilience4j-plugins/alibaba-druid` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/hibernate-core` | 0 | 1 | 0 | 0 | 0 | 0 | 1 |
| `microsphere-resilience4j-plugins/mybatis` | 2 | 2 | 2 | 2 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/openfeign` | 11 | 11 | 11 | 11 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/spring-web` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| **Matched total** | **42** | **42** | **40** | **28** | **12** | **2** | **3** |

- `microsphere-resilience4j-compatible` (0.1.4 only) and the POM-only modules (`-parent`, `-dependencies`, `-spring-boot`, `-spring-cloud`) contribute no comparable test sources.

## `microsphere-resilience4j-commons` (src/test)

| File | Test change | Reason |
|---|---|---|
| `bulkhead/BulkheadTemplateTest.java` | `maxConcurrentCalls` `2` → `1` | **Deterministic rejection.** With a limit of 2 the "call not permitted" branch depends on thread interleaving; a limit of 1 makes the rejection path reproducible. |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | Config builder adds `.maxWaitDurationInHalfOpenState(duration)` | **Coverage of an option unavailable at the 0.1.x floor.** The builder method does not exist in Resilience4j 1.3/1.4 (it appears at 1.6), so the 0.1.x test had to leave it out; on the 2.0 floor it can be configured. |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | Config builder adds `.writableStackTraceEnabled(false)` | **Test output noise control** — stack traces in recorded events are no longer needed for the assertions; the option exists in both lines, so this is a test-hygiene choice, not a forced change. |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | `onFailureRateExceededEvent` / `onSlowCallRateExceededEvent` consumers add `assertSame(FAILURE_RATE_EXCEEDED / SLOW_CALL_RATE_EXCEEDED, event.getEventType())` | **Stronger assertion + rationale for the `-compatible` shim.** In 0.1.x those event classes were supplied by `microsphere-resilience4j-compatible`, so the type constants were only asserted where 1.x natively guaranteed them; in 0.2.x they are native Resilience4j types, so the event type is asserted directly. |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | `testExecute()` no longer declares `throws Throwable` | **Cleanup, no behaviour change** — the method body only calls `template.execute(...)`, which does not propagate a checked exception, so the redundant declaration was dropped. |
| `common/AbstractResilience4jTemplateTest.java` | `testBegin()` / `testEnd()` restructured: the `ValueHolder<Object>` + `switch (module)` capture-then-assert pattern becomes a direct `if (template.isBeginSupported()) { …assert… } else { assertThrows(UnsupportedOperationException.class, …) }` | **Functional need.** The old shape existed because `begin`/`end` threw for the modules that do not support them, so the throwable had to be stashed before the module branch. The guard-first form asserts the contract directly and, in `testEnd`, now exercises **all three** reporting paths — plain success, `context.setResult("For testing")` (the new `onResult(...)` branch), and failure — which is exactly what the `RateLimiterTemplate`/`CircuitBreakerTemplate` changes in `src/main` require. |
| `common/AbstractResilience4jTemplateTest.java` | `testIsEndSupported()` drops `assertEquals(isBeginSupported(), isEndSupported())` | **Redundancy removal** — the per-module `switch` immediately below asserts each flag independently; the cross-flag equality was duplicative. (The implementations remain symmetric: `RetryTemplate`/`TimeLimiterTemplate` return `false` for both in both tags.) |
| `common/AbstractResilience4jTemplateTest.java` | `io.microsphere.util.ValueHolder` import deleted; `case A: case B: case C:` → `case A, B, C:` | **Language-level only** (Java 14+ multi-label `case`, enabled by the Java 17 line) — recorded for diff traceability, **not** an implementation difference. |
| `common/Resilience4jModuleTest.java` | All six `assertModule(...)` calls pass `Common*ConfigurationProperties.class` | **API compatibility** — mirrors the `Resilience4jModule` enum rebinding; 2.x renamed the framework-common property holders. |
| `ratelimiter/RateLimiterTemplateTest.java` | `SUCCESSFUL_ACQUIRE` consumer adds `assertEquals(1, event.getNumberOfPermits())` | **Coverage made meaningful by the new flow.** `getNumberOfPermits()` already exists in 1.x, but the 0.1.x template never fed the outcome back to the `RateLimiter`; now that `doEnd` calls `onSuccess()`, the emitted event carries the granted permit count and can be asserted. |

Added / removed files:

| Path | Status | Reason |
|---|---|---|
| `common/ChainableResilience4jFacadeTest2.java` | removed (0.1.4 only) | Renamed — see next row. |
| `common/ChainableResilience4jFacadeTest.java` | added (0.2.4 only) | **Test refactoring, no coverage change.** The temporary `2` suffix is dropped and the inline construction of five `*Registry.ofDefaults()` registries is replaced by `createChainableResilience4jFacade()`. |
| `test/Resilience4jTestUtils.java` | added (0.2.4 only) | **Shared fixture extraction** — a `@Nonnull` factory for a default `ChainableResilience4jFacade`, created so the commons facade test and the newly added hibernate-core plugin test can both build a facade without duplicating the registry wiring. |

Unchanged (7 files): `ThreadPoolBulkheadTemplateTest`, `CallbackChainTest`, `Resilience4jConstantsTest`, `Resilience4jContextTest`, `RetryTemplateTest`, `TimeLimiterTemplateTest`, `util/Resilience4jUtilsTest`.

## `microsphere-resilience4j-spring` (src/test)

| File | Test change | Reason |
|---|---|---|
| `bulkhead/annotation/EnableBulkheadTest.java` | `InstanceProperties` local variable retyped to `CommonBulkheadConfigurationProperties.InstanceProperties` | **API compatibility** — 2.x moved `InstanceProperties` under the `Common*` holder. Asserted values (`maxConcurrentCalls` 10, buffer 100, `maxWaitDuration` 30s) are unchanged. |
| `bulkhead/annotation/EnableThreadPoolBulkheadTest.java` | Field and nested-type imports `ThreadPoolBulkheadConfigurationProperties` → `CommonThreadPoolBulkheadConfigurationProperties` | **API compatibility** — same rename; assertions unchanged. |
| `circuitbreaker/annotation/EnableCircuitBreakerTest.java` | `InstanceProperties` retyped to `CommonCircuitBreakerConfigurationProperties.InstanceProperties` | **API compatibility** — same rename; assertions unchanged. |
| `circuitbreaker/event/CircuitBreakerApplicationEventPublisherTest.java` | `@ContextConfiguration` loses `initializers = {CommonConfigurationsApplicationContextInitializer.class}`; `CircuitBreakerConfigurationProperties` and `CircuitBreakerConfiguration` swap declaration order | **Functional need.** The initializer only existed to inject the `FallbackConfiguration`/`SpelResolverConfiguration` beans that 1.x's `CircuitBreakerConfiguration` did not import. Under 2.x that class carries `@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})`, so the context loads without the initializer. |
| `circuitbreaker/event/CircuitBreakerEventConsumerBeanRegistrarTest.java` | Same initializer removal; `static import ofMillis(100)` → explicit `Duration.ofMillis(100)` | **Functional need** (as above) + **style-only** import change; the elapsed-duration assertion is identical in meaning. |
| `ratelimiter/annotation/EnableRateLimiterTest.java` | Added `rateLimiter.onSuccess();` before the end of the test | **Functional need (2.x flow).** The rate limiter only emits the success event/metrics once the outcome is reported explicitly; the call keeps the test aligned with the new `RateLimiterTemplate.doEnd` contract. |
| `retry/annotation/EnableRetryTest.java` | Added `assertEquals(valueOf(1), instanceProperties.getMaxAttempts())` | **Coverage** — verifies that the configured `max-attempts` binds through `EnableRetry`; the surrounding assertions are unchanged. |

Removed file:

| Path | Status | Reason |
|---|---|---|
| `context/CommonConfigurationsApplicationContextInitializer.java` | removed (0.1.4 only) | **Compatibility scaffolding retired.** It registered the 1.x fallback/SpEL-resolver common configurations for the two circuit-breaker event tests; the matching production helper (`Resilience4jUtils.registerCommonConfigurations`) and its invocation in `EnableResilience4jRegistrar` were removed in 0.2.4 for the same reason. |

Unchanged (6 files): `LazyResilience4jFacadeTest`, `LoggingResilience4jPlugin`, `NoOpResilience4jPlugin`, `common/event/EventConsumerMethodFilterTest`, `timelimiter/annotation/EnableTimeLimiterTest`, `resources/META-INF/spring.factories`.

## `microsphere-resilience4j-plugins/*` (src/test)

| Module | Change | Reason |
|---|---|---|
| `hibernate-core` | **New** `Resilience4jHibernateEntityCallbackTest`, extending `AbstractHibernateH2Test` | **Coverage gap closed.** The plugin had zero tests in 0.1.4. The 0.2.4 module POM adds `microsphere-hibernate-test`, `microsphere-spring-test`, `spring-test` and H2/logback test dependencies, which is what makes an H2-backed entity-callback test possible. Not an API-porting artefact. |
| `alibaba-druid` | identical | — |
| `mybatis` | identical | — |
| `openfeign` | identical (11 files) | — |
| `spring-web` | identical | — |

## Summary

The `src/test` delta tracks the `src/main` delta almost one-to-one:

1. **Rename fallout (3 files)** — every `*ConfigurationProperties` reference in the tests follows the `Common*` rename in `Resilience4jModule` and the `Enable*` annotations.
2. **Behaviour fallout (3 files)** — `AbstractResilience4jTemplateTest` restructured to drive the success/result/failure reporting paths, `EnableRateLimiterTest` gains an explicit `onSuccess()`, `RateLimiterTemplateTest` gains the permit-count assertion.
3. **Coverage growth (4 files)** — `maxWaitDurationInHalfOpenState`, event-type assertions for the failure/slow-call-rate events, `max-attempts` binding, and the brand-new hibernate-core callback test.
4. **Scaffolding removal (2 files + 1 rename)** — the `CommonConfigurationsApplicationContextInitializer` and its `initializers = {…}` wiring disappear; `ChainableResilience4jFacadeTest2` becomes `ChainableResilience4jFacadeTest` with its fixture extracted into the new shared `Resilience4jTestUtils`.

The only language-level difference observed anywhere in the compared test sources is the Java 17 multi-label `case` form in `AbstractResilience4jTemplateTest`; no `javax`/`jakarta` differences appear.
