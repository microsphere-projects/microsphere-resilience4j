# microsphere-resilience4j-commons — User Guide (1.x)

> Version line: `0.1.x` (source branch `release-1.x`) · latest release `0.1.4` · branch revision `0.1.5-SNAPSHOT`
> Compatible with **Resilience4j 1.3.0 – 1.7.0** (Maven profiles `resilience4j-1.3` … `resilience4j-1.7`, default **1.7.0**)
> This module has **no Spring dependency** — its source imports nothing from `org.springframework`; it is plain Java on top of the Resilience4j registries and `microsphere-java-core`.
> ← [guide index](README.md) · [Developer Guide page](../developer-guide/microsphere-resilience4j-commons.md)

---

## 1. Module overview

`microsphere-resilience4j-commons` is the framework-neutral core of the 1.x line. It puts all six
Resilience4j modules — **CircuitBreaker, RateLimiter, Retry, Bulkhead, ThreadPoolBulkhead, TimeLimiter** —
behind one programming model so application code never has to learn six different decorator APIs. On this
`release-1.x` branch that model is compiled and tested against Resilience4j **1.3.0 through 1.7.0**, with an
optional compatibility shim (see section 8) filling in the event types that only the newer 1.x releases provide.

The model has four moving parts:

| Concept | Type | What it means |
|---------|------|---------------|
| **Module** | `Resilience4jModule` | An enum describing one Resilience4j capability: its entry class, config class, event class, registry class, configuration-properties class and default aspect order. |
| **Entry** | e.g. `CircuitBreaker` | A *named* instance living inside a Resilience4j `Registry`. |
| **Template** | `Resilience4jTemplate` subclass | Wraps exactly one `Registry` and executes callbacks with that module's decoration (`execute` / `call`, and optional `begin` + `end`). |
| **Facade** | `ChainableResilience4jFacade` | Wraps *several* templates and composes them around a single callback, in priority order. |

The packages you work with:

```
io.microsphere.resilience4j.common    <- abstractions: Resilience4jOperations, AdvancedResilience4jOperations,
                                          Resilience4jTemplate, Resilience4jContext, Resilience4jModule,
                                          Resilience4jFacade, ChainableResilience4jFacade, CallbackChain,
                                          Resilience4jConstants
io.microsphere.resilience4j.bulkhead  <- BulkheadTemplate, ThreadPoolBulkheadTemplate
io.microsphere.resilience4j.circuitbreaker / .ratelimiter / .retry / .timelimiter
io.microsphere.resilience4j.util      <- Resilience4jUtils
```

Reasons to use this module instead of raw Resilience4j 1.x:

- One uniform API (`execute` / `call`) for every capability, including checked-exception-friendly `call`.
- Named-instance plumbing (find-in-registry, else create-with-named-configuration) is done for you.
- Multi-module composition (`Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(function))))))`) without hand-written decorator nesting.
- A per-execution `Resilience4jContext` you can carry, decorate and bind to the current thread.
- Registry- and entry-level event subscription with a fluent, module-typed API.

---

## 2. Maven coordinates and BOM usage

Direct dependency (this is the `0.1.x` line — do not mix it with the `0.2.x` line in one application):

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-commons</artifactId>
    <version>0.1.4</version>
</dependency>
```

Or import the project BOM (`microsphere-resilience4j-dependencies`) and drop the version:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.1.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-commons</artifactId>
    </dependency>
</dependencies>
```

> **Every compile dependency of this module is declared `<optional>true</optional>`.** The module ships
> abstraction only; you bring your own Resilience4j artifacts (`resilience4j-core`, `resilience4j-circuitbreaker`,
> `resilience4j-ratelimiter`, `resilience4j-retry`, `resilience4j-bulkhead`, `resilience4j-timelimiter`,
> `resilience4j-framework-common`) and `microsphere-java-core`. This keeps the library neutral about which
> Resilience4j 1.x version you deploy.

The Resilience4j version itself is selected by the parent POM's Maven profiles, resolved by the
`resilience4j.version` property:

| Profile | `resilience4j.version` | Compatible shim attached? |
|---------|------------------------|---------------------------|
| `resilience4j-1.3` | `1.3.0` | yes |
| `resilience4j-1.4` | `1.4.0` | yes |
| `resilience4j-1.5` | `1.5.0` | yes |
| `resilience4j-1.6` | `1.6.0` | yes |
| `resilience4j-1.7` *(activeByDefault)* | `1.7.0` | no (1.7.0 already has the event types) |

The `compatible` shim is added to `commons` automatically by the `resilience4j-1.3` … `resilience4j-1.6`
profiles defined in this module's own `pom.xml`; you do not declare it yourself. See section 8 and the
[compatible user guide](microsphere-resilience4j-compatible.md).

---

## 3. Compatibility table

| microsphere-resilience4j | Resilience4j | Spring Cloud stack (set by the parent, not by this module) | JDK |
|--------------------------|--------------|-------------------------------------------------------------|-----|
| `0.1.x` (`release-1.x`, latest `0.1.4`) | 1.3.0 – 1.7.0 (default 1.7.0) | Hoxton – 2021.0.x | 8+ |

The commons module does not itself reference Spring or Spring Cloud; the Spring Cloud range above is the stack
the surrounding 1.x reactor is built against, provided by `microsphere-spring-cloud-parent` `0.1.26`.

---

## 4. API reference (public types)

Every signature below is taken from the `release-1.x` source. Types with the same name as on the `0.2.x` line may
have different behaviour because they delegate to the Resilience4j **1.x** APIs (see section 8).

### 4.1 `Resilience4jOperations<E>` (interface)

The minimal contract for running a callback under one module's decoration. `E` is the entry type (e.g. `CircuitBreaker`).
All methods except `begin` / `end` are `default` implementations.

| Method | Behaviour |
|--------|-----------|
| `void execute(String name, Runnable callback)` | Runs a void callback; wraps it into the supplier form. |
| `<T> T execute(String name, Supplier<T> callback)` | Runs a value-returning callback; delegates to `call(name, callback::get)` and re-throws any `Throwable` wrapped as `RuntimeException`. |
| `void call(String name, ThrowableAction callback) throws Throwable` | Void callback that may throw; wraps it into the supplier form returning `null`. |
| `<TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR` | Same, but re-throws failures coerced to `throwableClass`. |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | The core method: `begin(name)` → run callback → `end(context)` in a `finally`. Concrete templates may override this. |
| `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR` | Same, coercing thrown errors to `throwableClass`. |
| `boolean isBeginSupported()` | `true` by default. |
| `Resilience4jContext<E> begin(String name)` | Abstract — first phase of the two-phase lifecycle. |
| `boolean isEndSupported()` | `true` by default. |
| `void end(Resilience4jContext<E> context)` | Abstract — second phase of the two-phase lifecycle. |

Two execution styles:

- **One-time**: `execute` / `call` — the template decorates the whole callback and returns its result.
- **Two-phase**: `begin` (acquire permission / record start) → do work → `end` (record success/failure / release).

### 4.2 `AdvancedResilience4jOperations<E, C, R extends Registry<E, C>>` (interface)

Extends `Resilience4jOperations<E>` and `io.microsphere.lang.Prioritized`. Adds getters, configuration/entry
management, and raw-entry `Consumer`/`Function` operations. `E` = entry, `C` = config, `R` = registry.

| Group | Methods |
|-------|---------|
| Getters | `R getRegistry()`, `Resilience4jModule getModule()`, `default Class<E> getEntryClass()`, `default Class<C> getConfigClass()`, `Logger getLogger()` |
| Configuration | `default C getConfiguration(String configName)` (falls back to default), `default C getDefaultConfig()`, `default AdvancedResilience4jOperations<E,C,R> addConfiguration(String configName, C configuration)` |
| Entry | `default E getEntry(String name)` (find in registry, else `createEntry`), `E createEntry(String name)`, `default E removeEntry(String name)`, `default E replaceEntry(String name, E newEntry)` |
| Raw entry, one-time | `default AdvancedResilience4jOperations<E,C,R> execute(String name, Consumer<E> entryConsumer)`, `default <T> T execute(String name, Function<E,T> entryFunction)` |
| Raw entry, throwable | `default AdvancedResilience4jOperations<E,C,R> call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable`, `default <TR extends Throwable> AdvancedResilience4jOperations<E,C,R> call(String name, ThrowableConsumer<E> entryConsumer, Class<TR> throwableClass) throws TR`, `default <T> T call(String name, ThrowableFunction<E,T> entryFunction) throws Throwable`, `default <T, TR extends Throwable> T call(String name, ThrowableFunction<E,T> entryFunction, Class<TR> throwableClass) throws TR` |

The `Consumer`/`Function` overloads operate on the resolved entry itself (not on a callback) — for example
`template.execute(entryName, CircuitBreaker::reset)` or `template.execute(entryName, e -> e)` — and are useful
for reading or mutating an entry without running a decorated business call.

### 4.3 `Resilience4jTemplate<E, C, R extends Registry<E, C>>` (abstract class)

The base implementation of `AdvancedResilience4jOperations`. It owns a `Registry`, resolves the module from the
registry class, and (by default) maintains a local, non-thread-safe `HashMap` entry cache for performance.

Constructors:

| Constructor | Notes |
|-------------|-------|
| `Resilience4jTemplate(R registry)` | Equivalent to `(registry, true)` — local entries cached. |
| `Resilience4jTemplate(R registry, boolean localEntriesCached)` | `assertNotNull(registry)`; resolves `module` via `Resilience4jModule.valueOf(registry.getClass())`, obtains the registry `EventProcessor`, and seeds `priority` from `module.getDefaultAspectOrder()`. |

Key methods:

| Method | Visibility | Notes |
|--------|-----------|-------|
| `final R getRegistry()` / `final Resilience4jModule getModule()` / `final C getDefaultConfig()` / `final Class<E> getEntryClass()` / `final Class<C> getConfigClass()` / `final Logger getLogger()` | public final | Delegates to the interface defaults, so subclasses cannot change them. |
| `final E getEntry(String name)` | public final | Reads the local cache first, else falls through to the registry lookup/create. |
| `final Resilience4jTemplate<E,C,R> initLocalEntriesCache(String name)` / `...(Iterable<String> names)` | public final | Pre-populates the local cache (the only way it becomes effectively thread-safe). |
| `protected Map<String,E> createLocalEntriesCache()` | protected | Returns a new `HashMap`; `null` means "no cache". |
| `protected final boolean isLocalEntriesCached()` / `clearLocalEntriesCache()` / `getEntryFromCache(String)` | protected final | Cache helpers. |
| `final Resilience4jContext<E> begin(String name)` | public final | `getEntry(name)` → `new Resilience4jContext(name, entry)` → `doBegin(context)`. |
| `protected void doBegin(Resilience4jContext<E> context)` | protected | Hook; default throws `UnsupportedOperationException`. |
| `final void end(Resilience4jContext<E> context)` | public final | Calls `doEnd(context)`. |
| `protected void doEnd(Resilience4jContext<E> context)` | protected | Hook; default throws `UnsupportedOperationException`. |
| `final Resilience4jTemplate<E,C,R> onEntryAddedEvent(EventConsumer<EntryAddedEvent<E>>)` / `onEntryRemovedEvent(...)` / `onEntryReplacedEvent(...)` | public final | Registry-level event subscriptions. |
| `protected final <T extends RegistryEvent> Resilience4jTemplate<E,C,R> registerRegistryEventConsumer(Class<? super T> eventType, EventConsumer<T> consumer)` | protected final | |
| `protected final <T> Resilience4jTemplate<E,C,R> registerEntryEventConsumer(String entryName, Class<? super T> eventType, EventConsumer<T> consumer)` | protected final | Entry-level event subscription (used by every concrete template). |
| `final int getPriority()` / `final Resilience4jTemplate<E,C,R> setPriority(int priority)` | public final | `Prioritized` support; drives facade ordering. |
| `void destroy()` | public | Clears the local cache. |

Note the `begin`/`end`/`getEntry`/`getRegistry` methods are `final`: subclasses customise behaviour only through the
`doBegin`/`doEnd` hooks, by overriding `call(String, ThrowableSupplier)`, `isBeginSupported()` / `isEndSupported()`,
`createEntry(String)`, or `createLocalEntriesCache()`.

### 4.4 `Resilience4jContext<E>` (class)

Mutable per-execution state. The constructor is `protected` (created by templates and by same-package collaborators).

| Member | Description |
|--------|-------------|
| `Resilience4jContext(String entryName, E entry)` | `protected`; both arguments asserted non-null. |
| `String getEntryName()` / `E getEntry()` | The resolved entry and its name. |
| `setStartTime(Long)` / `getStartTime()` | Execution start timestamp (`null` until set). |
| `setResult(Object)` / `getResult()` | Execution result. |
| `setFailure(Throwable)` / `getFailure()` | Execution failure. |
| `setAttribute(String, Object)` / `hasAttribute(String)` / `getAttribute(String)` / `getAttribute(String, T)` / `removeAttribute(String)` / `removeAttributes()` / `getAttributes()` | Attribute bag; `getAttributes()` returns an unmodifiable view (empty map when none). |
| `withinContext()` | Binds this context to the current thread and returns it. |
| `static Resilience4jContext getContext()` | Current thread-bound context or `null`. |
| `static void setContext(Resilience4jContext)` | Binds (or clears with `null`) on the thread. |
| `static Resilience4jContext removeContext()` | Removes and returns the thread-bound context. |
| `static void doInContext(Consumer<Resilience4jContext>)` / `doInContext(Consumer<Resilience4jContext>, boolean forRemoval)` | Runs the consumer if a context is bound; `forRemoval=true` removes it first. |

The setters (`setStartTime`, `setResult`, `setFailure`, `setAttribute`, `removeAttributes`, `withinContext`)
are fluent and return `this`. Thread binding has two spellings that play different roles: the **instance** method
`withinContext()` binds the current context to the thread, while the **static** `setContext(Resilience4jContext)`
binds (or, with `null`, clears) an arbitrary context. The release notes record an earlier rename of the instance
helper to `withinContext()`; the static `setContext` remains as a distinct API.

### 4.5 `Resilience4jFacade` (interface)

A marker interface: `Resilience4jFacade extends Resilience4jOperations<Resilience4jContext[]>`. Its "entry" type is
the **array of sub-contexts**, one per wrapped template.

### 4.6 `ChainableResilience4jFacade` (class)

The default `Resilience4jFacade`. It builds one `Resilience4jTemplate` per supplied `Registry` (through
`Resilience4jUtils.createTemplates`), de-duplicates templates, and sorts them by priority (`Prioritized.COMPARATOR`).

Constructors:

| Constructor | Notes |
|-------------|-------|
| `ChainableResilience4jFacade(Registry... registries)` | Varargs convenience; wraps into the `Collection` form. |
| `ChainableResilience4jFacade(Collection<Registry> registries)` | Creates + sorts the templates. |
| `ChainableResilience4jFacade(List<Resilience4jTemplate> templates)` | De-duplicates and sorts the given templates. |

Methods:

| Method | Notes |
|--------|-------|
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | Wraps the callback in a `CallbackChain` and runs the full template chain (see below). |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` | Allocates a `Resilience4jContext[]` of sub-contexts and calls each supporting template's `begin`. |
| `void end(Resilience4jContext<Resilience4jContext[]> context)` | Calls each supporting template's `end` with its sub-context. |
| `List<Resilience4jTemplate> getTemplates()` / `int getSize()` / `void destroy()` | Accessors and lifecycle. |

### 4.7 `CallbackChain<T>` (package-private class)

Not part of the public API (declared without a modifier, i.e. package-private in `io.microsphere.resilience4j.common`).
It is a `ThrowableSupplier<T>` that walks the template list, nesting each template's `call(name, …)` around the
remaining chain, and finally runs the user callback:

```java
public T get() throws Throwable {
    if (pos < size) {
        Resilience4jTemplate template = templates.get(pos++);
        return (T) template.call(this.entryName, CallbackChain.this::get);
    }
    return delegate.get();
}
```

### 4.8 `Resilience4jModule` (enum)

Metadata table for the six capabilities. Constants (name / entry / config / config-properties / event / registry / default aspect order):

| Constant | Module name | Entry | Event class | Registry | Order |
|----------|-------------|-------|-------------|----------|-------|
| `RETRY` | `retry` | `Retry` | `RetryEvent` | `RetryRegistry` | 0 |
| `CIRCUIT_BREAKER` | `circuit-breaker` | `CircuitBreaker` | `CircuitBreakerEvent` | `CircuitBreakerRegistry` | 1 |
| `RATE_LIMITER` | `rate-limiter` | `RateLimiter` | `RateLimiterEvent` | `RateLimiterRegistry` | 2 |
| `TIME_LIMITER` | `time-limiter` | `TimeLimiter` | `TimeLimiterEvent` | `TimeLimiterRegistry` | 3 |
| `BULKHEAD` | `bulkhead` | `Bulkhead` | `BulkheadEvent` | `BulkheadRegistry` | 4 |
| `THREAD_POOL_BULKHEAD` | `thread-pool-bulkhead` | `ThreadPoolBulkhead` | `BulkheadEvent` | `ThreadPoolBulkheadRegistry` | 4 |

The order mirrors Resilience4j's documented aspect order (`Retry > CircuitBreaker > RateLimiter > TimeLimiter >
Bulkhead`); `ThreadPoolBulkhead` shares order `4` with `Bulkhead`.

Methods: `getName()`, `getEntryClass()`, `getConfigClass()`, `getConfigurationPropertiesClass()`,
`getEventClass()`, `getRegistryClass()`, `getDefaultAspectOrder()`, `toString()`, and the static
`valueOf(Class<?> type)` lookup which matches a class against any of the five known classes of each module and
throws `IllegalArgumentException` if none matches.

### 4.9 `Resilience4jConstants` (interface)

String constants for configuration prefixes. `PREFIX = "microsphere.resilience4j."` and per-module
`<MODULE>_MODULE_NAME` / `<MODULE>_PREFIX`, e.g. `RETRY_MODULE_NAME = "retry"`, `RETRY_PREFIX =
"microsphere.resilience4j.retry."`. Covers `retry`, `circuit-breaker`, `rate-limiter`, `time-limiter`,
`bulkhead`, `thread-pool-bulkhead`.

### 4.10 `Resilience4jUtils` (abstract class, implements `Utils`)

Reflection/registry helpers. Public statics:

| Method | Notes |
|--------|-------|
| `static <E, C> E getEntry(Registry<E, C> registry, String name)` | Uses `registry.getDefaultConfig()`. |
| `static <E, C> E getEntry(Registry<E, C> registry, String name, C configuration)` | Resolves the module, then invokes the registry's `entryName(name, config)` method reflectively. |
| `static <E> EventProcessor<E> getEventProcessor(E entry)` | Dispatches on entry type (`CircuitBreaker`/`Bulkhead`/`ThreadPoolBulkhead`/`RateLimiter`/`TimeLimiter`/`Retry`); throws `UnsupportedOperationException` otherwise. |
| `static <E> EventProcessor<E> getEventProcessor(Retry/TimeLimiter/RateLimiter/Bulkhead/ThreadPoolBulkhead/CircuitBreaker entry)` | Per-type forms returning `entry.getEventPublisher()` as an `EventProcessor`. |
| `static <T> EventProcessor<T> getEventProcessor(Registry registry)` | Registry-level event publisher. |
| `static <E, C, R extends Registry<E, C>, T extends Resilience4jTemplate<E, C, R>> T createTemplate(R registry)` | Instantiates the default template class for the registry's module (from `META-INF/default/templates.properties`). |
| `static List<Resilience4jTemplate> createTemplates(Collection<Registry> registries)` | Builds, de-duplicates and priority-sorts templates; returns an unmodifiable list (empty for empty input). |

The mapping from module to template class comes from the bundled resource
`META-INF/default/templates.properties`:

```properties
bulkhead = io.microsphere.resilience4j.bulkhead.BulkheadTemplate
thread-pool-bulkhead = io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate
circuit-breaker = io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate
rate-limiter = io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate
retry = io.microsphere.resilience4j.retry.RetryTemplate
time-limiter = io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate
```

### 4.11 The six concrete templates

All six live in their own sub-package and all accept `(Registry)` or `(Registry, boolean localEntriesCached)`.
`TimeLimiterTemplate` additionally accepts an `ExecutorService`.

| Template | `call` implementation (1.x Resilience4j API) | Two-phase (`begin`/`end`) | Entry-event registration methods |
|----------|---------------------------------------------|---------------------------|----------------------------------|
| `CircuitBreakerTemplate` | `circuitBreaker.decorateCheckedSupplier(callback::get).apply()` | supported — `doBegin` calls `acquirePermission()` + records `nanoTime()`; `doEnd` calls `onSuccess(duration, NANOSECONDS)` or `onError(duration, NANOSECONDS, failure)` | `onCallNotPermittedEvent`, `onErrorEvent`, `onFailureRateExceededEvent`, `onIgnoredErrorEvent`, `onResetEvent`, `onSlowCallRateExceededEvent`, `onStateTransitionEvent`, `onSuccessEvent` |
| `RateLimiterTemplate` | `RateLimiter.decorateCheckedSupplier(rateLimiter, callback::get).apply()` | supported — `doBegin` **and** `doEnd` both call `RateLimiter.waitForPermission(rateLimiter)` | `onSuccessEvent`, `onFailureEvent`, `onDrainedEvent` |
| `RetryTemplate` | `Retry.decorateCheckedSupplier(retry, callback::get).apply()` | **not supported** (`isBeginSupported()`/`isEndSupported()` → `false`) | `onErrorEvent`, `onIgnoredErrorEvent`, `onRetryEvent`, `onSuccessEvent` |
| `BulkheadTemplate` | *does not override `call`* → runs through `begin`/`end` | supported — `doBegin` = `acquirePermission()`, `doEnd` = `onComplete()` | `onCallPermittedEvent`, `onCallRejectedEvent`, `onCallFinishedEvent` |
| `ThreadPoolBulkheadTemplate` | `threadPoolBulkhead.executeCallable(...).toCompletableFuture().get()`, unwrapping the root cause on failure | **not supported** | `onCallPermittedEvent`, `onCallRejectedEvent`, `onCallFinishedEvent` |
| `TimeLimiterTemplate` | `timeLimiter.decorateFutureSupplier(() -> executorService.submit(...)).call()` | **not supported** | `onSuccessEvent`, `onErrorEvent`, `onTimeoutEvent` |

Constructors (verified on this branch):

- `CircuitBreakerTemplate`, `RateLimiterTemplate`, `RetryTemplate`, `BulkheadTemplate`, `ThreadPoolBulkheadTemplate`: `(Registry)` and `(Registry, boolean localEntriesCached)`.
- `TimeLimiterTemplate` has **four** constructors: `(TimeLimiterRegistry)`, `(TimeLimiterRegistry, boolean)`,
  `(TimeLimiterRegistry, ExecutorService)` and `(TimeLimiterRegistry, ExecutorService, boolean)`. When no executor
  is supplied it defaults to `ForkJoinPool.commonPool()`.

The event-registration methods all follow the same fluent pattern: `(String entryName, EventConsumer<SomeEvent> consumer)`
returning `this`. The `…OnFailureRateExceededEvent`, `…OnSlowCallRateExceededEvent` and `…OnDrainedEvent` types are the
ones supplied by the `compatible` shim when you build against Resilience4j 1.3–1.6.

---

## 5. Code examples

These are adapted from the `release-1.x` test suite
(`src/test/java/io/microsphere/resilience4j/...`).

### 5.1 Single template — CircuitBreaker (`CircuitBreakerTemplateTest`)

```java
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

import java.time.Duration;

import static java.time.Duration.ofMillis;

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .slowCallDurationThreshold(ofMillis(50))
        .permittedNumberOfCallsInHalfOpenState(2)
        .slowCallRateThreshold(10)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> {
    // event.getCircuitBreakerName(), event.getEventType() == Type.SUCCESS
});

String result = template.execute("test-entry", () -> "test-entry");

// Read/mutate the resolved entry directly (AdvancedResilience4jOperations):
template.execute("test-entry", io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);

template.destroy();
```

The `call` path can also throw checked exceptions and be coerced to a declared type:

```java
template.call("test-entry", () -> {
    throw new java.util.concurrent.TimeoutException("boom");
}, java.util.concurrent.TimeoutException.class);   // re-thrown as TimeoutException
```

### 5.2 Bulkhead — one-time execution goes through begin/end (`BulkheadTemplateTest`)

Because `BulkheadTemplate` does not override `call`, `execute`/`call` acquire and release the bulkhead permit
around the callback:

```java
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;

import static java.time.Duration.ofMillis;

BulkheadConfig config = BulkheadConfig.custom()
        .maxConcurrentCalls(2)
        .maxWaitDuration(ofMillis(100))
        .build();

BulkheadTemplate template = new BulkheadTemplate(BulkheadRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onCallPermittedEvent("test-entry", event -> { /* Type.CALL_PERMITTED */ });
template.onCallFinishedEvent("test-entry", event -> { /* Type.CALL_FINISHED */ });

Object result = template.execute("test-entry", () -> null); // null
```

### 5.3 RateLimiter (`RateLimiterTemplateTest`)

```java
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate;

import static java.time.Duration.ofMillis;

RateLimiterConfig config = RateLimiterConfig.custom()
        .limitForPeriod(1)
        .limitRefreshPeriod(ofMillis(50))
        .timeoutDuration(ofMillis(50))
        .build();

RateLimiterTemplate template = new RateLimiterTemplate(RateLimiterRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> { /* RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE */ });
template.onFailureEvent("test-entry", event -> { /* ... */ });
template.onDrainedEvent("test-entry", event -> { /* RateLimiterEvent.Type.DRAINED */ });

for (int i = 0; i < 5; i++) {
    template.execute("test-entry", () -> "For testing");
}
```

### 5.4 Retry (`RetryTemplateTest`)

`RetryTemplate` has no two-phase support, so use `execute`/`call`; retry state is observed through events:

```java
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.retry.RetryTemplate;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

RetryConfig config = RetryConfig.custom()
        .maxAttempts(3)
        .ignoreExceptions(IllegalStateException.class)
        .retryExceptions(TimeoutException.class)
        .build();

RetryTemplate template = new RetryTemplate(RetryRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

AtomicInteger attempts = new AtomicInteger(3);
template.onRetryEvent("test-entry", event -> { /* Type.RETRY, event.getNumberOfRetryAttempts() */ });
template.onSuccessEvent("test-entry", event -> { /* Type.SUCCESS */ });

String result = template.call("test-entry", () -> {
    if (attempts.decrementAndGet() == 0) {
        return "OK";
    }
    throw new TimeoutException("For testing");
});
// result == "OK" after retries
```

### 5.5 TimeLimiter with an explicit executor (`TimeLimiterTemplateTest`)

```java
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;

ExecutorService executorService = newFixedThreadPool(1);

TimeLimiterConfig config = TimeLimiterConfig.custom()
        .cancelRunningFuture(true)
        .timeoutDuration(ofMillis(50))
        .build();

TimeLimiterTemplate template = new TimeLimiterTemplate(TimeLimiterRegistry.ofDefaults(), executorService);
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> { /* TimeLimiterEvent.Type.SUCCESS */ });
template.onTimeoutEvent("test-entry", event -> { /* TimeLimiterEvent.Type.TIMEOUT */ });

template.execute("test-entry", () -> { /* fast call succeeds */ });

executorService.shutdown();
```

A callback that runs longer than `timeoutDuration` throws `TimeoutException` from `call`.

### 5.6 ThreadPoolBulkhead (`ThreadPoolBulkheadTemplateTest`)

```java
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate;

ThreadPoolBulkheadTemplate template = new ThreadPoolBulkheadTemplate(ThreadPoolBulkheadRegistry.ofDefaults());

template.onCallPermittedEvent("test-entry", event -> { /* event.getBulkheadName() */ });
template.onCallFinishedEvent("test-entry", event -> { /* ... */ });

String result = template.execute("test-entry", () -> "test-entry");
// the callable runs on the thread pool; failures have their root cause re-thrown
```

### 5.7 Chainable facade (`ChainableResilience4jFacadeTest2`)

Passing multiple registries builds one template per module, de-duplicated and priority-sorted:

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(),
        BulkheadRegistry.ofDefaults(),     // duplicate -> collapsed
        RateLimiterRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(),   // duplicate -> collapsed
        TimeLimiterRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults(),   // duplicate -> collapsed
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults() // duplicate -> collapsed
);

facade.getSize();        // 5 effective templates (bulkhead, rate-limiter, time-limiter, retry, circuit-breaker)
facade.execute("test-entry", () -> { /* whole chain */ });
facade.call("test-entry", () -> { /* whole chain */ });

facade.destroy();
```

The two-phase form builds one sub-context per supporting template:

```java
import io.microsphere.resilience4j.common.Resilience4jContext;

Resilience4jContext<Resilience4jContext[]> context = facade.begin("test-entry");
Resilience4jContext[] subContexts = context.getEntry();   // length == facade.getSize()
facade.end(context);
```

### 5.8 Context helpers (`Resilience4jContextTest`)

```java
import io.microsphere.resilience4j.common.Resilience4jContext;

Resilience4jContext<String> context = newContext();   // created via a template, e.g. template.begin(name)
context.withinContext();                              // bind to the current thread
Resilience4jContext.getContext();                     // -> the bound context

Resilience4jContext.doInContext(ctx -> {
    ctx.setAttribute("key", "value");
    ctx.getAttribute("key");                          // "value"
    ctx.getAttribute("missing", "default");           // "default"
});

Resilience4jContext.doInContext(ctx -> { /* ... */ }, true); // forRemoval == true, clears afterwards
Resilience4jContext.getContext();                     // -> null
```

> The `Resilience4jContext` constructor is `protected`; application code obtains contexts from
> `template.begin(name)` or `facade.begin(name)` rather than constructing them directly.

### 5.9 Utilities (`Resilience4jUtilsTest`)

```java
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;
import io.microsphere.resilience4j.retry.RetryTemplate;
import io.microsphere.resilience4j.util.Resilience4jUtils;

import static java.util.Arrays.asList;

Bulkhead bulkhead = Resilience4jUtils.getEntry(BulkheadRegistry.ofDefaults(), "test");
BulkheadTemplate bt = Resilience4jUtils.createTemplate(BulkheadRegistry.ofDefaults()); // -> BulkheadTemplate
RetryTemplate rt  = (RetryTemplate) Resilience4jUtils.createTemplate(RetryRegistry.ofDefaults());

Resilience4jUtils.createTemplates(asList(RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults())).size(); // 2
```

`createTemplate` picks the right subclass per module and `createTemplates` returns a de-duplicated,
priority-sorted, unmodifiable list — the machinery `ChainableResilience4jFacade` builds on.

---

## 6. Two-phase vs one-time: which template does what

| Template | `execute`/`call` path | Explicit `begin`/`end` | `isBeginSupported()` / `isEndSupported()` |
|----------|-----------------------|------------------------|--------------------------------------------|
| CircuitBreaker | `decorateCheckedSupplier(...).apply()` | yes | `true` / `true` |
| RateLimiter | `decorateCheckedSupplier(...).apply()` | yes (both phases `waitForPermission`) | `true` / `true` |
| Bulkhead | base `call` → `begin` + `end` | yes | `true` / `true` |
| Retry | `decorateCheckedSupplier(...).apply()` | no | `false` / `false` |
| ThreadPoolBulkhead | `executeCallable(...).toCompletableFuture().get()` | no | `false` / `false` |
| TimeLimiter | `decorateFutureSupplier(...).call()` | no | `false` / `false` |

---

## 7. Logging

Templates use `io.microsphere.logging.Logger` (a thin abstraction over the logging backend) and emit `trace`
messages for creation, cache hits/misses, event registrations and `begin`/`end` transitions. No logging framework
is forced on you at compile time — `logback-classic` is only a test-scoped dependency of this module.

---

## 8. Difference from the 0.2.x line

This page documents the `release-1.x` (`0.1.x`) line. The API surface (`execute` / `call` / `begin` / `end`,
the six templates, `ChainableResilience4jFacade`) is intentionally the same shape as the `0.2.x` line, but the
internals target **Resilience4j 1.x**:

- **Compatible shim.** The extra Maven module `microsphere-resilience4j-compatible` back-ports Resilience4j
  event classes (`CircuitBreakerOnFailureRateExceededEvent`, `CircuitBreakerOnSlowCallRateExceededEvent`,
  `CircuitBreakerEvent`, `RateLimiterEvent`, `RateLimiterOnDrainedEvent`) under the original
  `io.github.resilience4j.*` package names, so the commons code compiles against 1.3–1.6. It is attached to
  commons automatically under the `resilience4j-1.3` … `resilience4j-1.6` profiles only; the default 1.7 build
  and the 0.2.x line do not need it. See [compatible user guide](microsphere-resilience4j-compatible.md).
- **Resilience4j 1.x decorator APIs.** The templates call the 1.x `decorateCheckedSupplier(...).apply()`
  (a `CheckedFunction0`-style throwing `apply()`), `TimeLimiter.decorateFutureSupplier(...).call()`, and
  `ThreadPoolBulkhead.executeCallable(...)`. On Resilience4j 2.x (the 0.2.x line) these decorator factories and
  functional interfaces return the vavr-free 2.x types with different method names, so the bodies differ even
  though the entry points look the same.
- **Event type names.** The 1.x event enums (e.g. `CircuitBreakerEvent.Type.SUCCESS`,
  `RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE`, `BulkheadEvent.Type.CALL_PERMITTED`) are used directly; some of
  these are renamed or reshaped in the 2.x event model.
- **Version range and parent.** This line targets Resilience4j `1.3.0 – 1.7.0` and
  `microsphere-spring-cloud-parent` `0.1.26` (Spring Cloud Hoxton – 2021.0.x), against JDK 8. The 0.2.x line
  targets Resilience4j 2.x and a newer Spring stack on JDK 17.

For the current line see [../../current/user-guide/microsphere-resilience4j-commons.md](../../current/user-guide/microsphere-resilience4j-commons.md).

---

Chinese translations of every page live in `zh/` with identical file names.
