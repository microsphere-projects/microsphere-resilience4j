# microsphere-resilience4j-commons — User Guide

> Version line: `0.2.x` (source branch `release`) · latest release `0.2.4` · branch revision `0.2.5-SNAPSHOT`
> Compatible with **Resilience4j 2.0.0 – 2.4.0** (Maven profiles `resilience4j-2.0` … `resilience4j-2.4`, default **2.4.0**)
> This module has **no Spring dependency** — it is pure Java on top of the Resilience4j registries.
> ← [User Guide index](README.md) · [Developer Guide page](../developer-guide/microsphere-resilience4j-commons.md)

---

## 1. Module overview

`microsphere-resilience4j-commons` is the framework-neutral core of the project. It puts all six
Resilience4j modules — **CircuitBreaker, RateLimiter, Retry, Bulkhead, ThreadPoolBulkhead, TimeLimiter** —
behind one programming model so that application code never has to learn six different decorator APIs.

The model has four moving parts:

| Concept | Type | What it means |
|---------|------|---------------|
| **Module** | `Resilience4jModule` | An enum describing one Resilience4j capability: its entry class, config class, event class, registry class, configuration-properties class and default aspect order. |
| **Entry** | e.g. `CircuitBreaker` | A *named* instance living inside a Resilience4j `Registry`. |
| **Template** | `Resilience4jTemplate` subclass | Wraps exactly one `Registry` and executes callbacks with that module's decoration (`execute` / `call` / `begin`+`end`). |
| **Facade** | `ChainableResilience4jFacade` | Wraps *several* templates and composes them around a single callback, in priority order. |

Everything you need is in three packages:

```
io.microsphere.resilience4j.common    <- abstractions: Resilience4jOperations, AdvancedResilience4jOperations,
                                          Resilience4jTemplate, Resilience4jContext, Resilience4jModule,
                                          Resilience4jFacade, ChainableResilience4jFacade, CallbackChain,
                                          Resilience4jConstants
io.microsphere.resilience4j.bulkhead  <- BulkheadTemplate, ThreadPoolBulkheadTemplate
io.microsphere.resilience4j.circuitbreaker / .ratelimiter / .retry / .timelimiter
io.microsphere.resilience4j.util      <- Resilience4jUtils
```

Typical reasons to use this module instead of raw Resilience4j:

- One uniform API (`execute` / `call`) for every capability, including checked-exception-friendly `call`.
- Named-instance plumbing (find-in-registry, else create-with-named-configuration) is done for you.
- Multi-module composition (`Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(function))))))`) without hand-written decorator nesting.
- A per-execution `Resilience4jContext` you can carry, decorate and bind to the current thread.
- Registry- and entry-level event subscription with a fluent, module-typed API.

---

## 2. Maven coordinates

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-commons</artifactId>
    <version>0.2.4</version>
</dependency>
```

Or import the project BOM and drop the version:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.2.4</version>
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

> **Important — every compile dependency of this module is declared `<optional>true</optional>`.**
> That includes `resilience4j-core`, `resilience4j-circuitbreaker`, `resilience4j-ratelimiter`,
> `resilience4j-retry`, `resilience4j-bulkhead`, `resilience4j-timelimiter`, `resilience4j-annotations`,
> `resilience4j-consumer`, `resilience4j-framework-common` and `microsphere-java-core`.
> Optional dependencies are **not** transitively inherited by your application, so you must declare the
> Resilience4j artifacts you use yourself (the `io.github.resilience4j:resilience4j-bom` import below keeps
> them on one version):
>
> ```xml
> <dependencyManagement>
>     <dependencies>
>         <dependency>
>             <groupId>io.github.resilience4j</groupId>
>             <artifactId>resilience4j-bom</artifactId>
>             <version>2.4.0</version>
>             <type>pom</type>
>             <scope>import</scope>
>         </dependency>
>     </dependencies>
> </dependencyManagement>
> ```
>
> This is deliberate: the module is a thin abstraction layer and does not want to pin your Resilience4j version.
> The price is that you must keep the six module artifacts on the classpath if you want `Resilience4jUtils`
> to work (see §8.7).

---

## 3. Version and compatibility

| Item | Value | Verified from |
|------|-------|---------------|
| Branch / version line | `release`, `0.2.x` | aggregator `pom.xml` (`<revision>0.2.5-SNAPSHOT</revision>`) |
| Latest published release | `0.2.4` | root `README.md` branch table ("**main** … Latest Version `0.2.4`"); `release-notes.md` on this branch only narrates `v0.2.1` and `v0.2.2` |
| Resilience4j | `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`, `2.4.0` | profiles `resilience4j-2.0` … `resilience4j-2.4` in `microsphere-resilience4j-parent/pom.xml`; `resilience4j-2.4` is `activeByDefault` |
| Spring Cloud (project line, **not** this module) | `2022.0.x` – `2025.0.x` | profiles `spring-cloud-2022` … `spring-cloud-2025` in CI + parent `microsphere-spring-cloud-parent` `0.2.26` |
| JDK | `17` baseline (`<java.version>17</java.version>`), CI runs `17`, `21`, `25` | aggregator `pom.xml`, `.github/workflows/maven-build.yml` |
| Spring dependency of this module | **none** | `microsphere-resilience4j-commons/pom.xml` |

Do not mix this line with the `0.1.x` / `1.x` line (Resilience4j 1.3.0 – 1.7.0); see the
[1.x User Guide](../../1.x/user-guide/README.md).

---

## 4. Core concepts you must understand first

### 4.1 Registry semantics vs. named-instance semantics

A `Resilience4jTemplate` **never owns** entries; it owns a `Registry` and resolves entries lazily by name:

```
getEntry(name)  ->  1) local entries cache (if enabled)
                    2) registry.find(name)
                    3) createEntry(name)  ==  registry.<module>(name, getConfiguration(name))
```

- Step 2/3 is why the same name always maps to the *same* entry object: state (failure-rate windows,
  bulkhead permits, retry counters) lives in the registry, not in the template.
- `createEntry(name)` looks the configuration up **by the entry name** (`getConfiguration(name)`).
  If no configuration is registered under that name, the registry's **default configuration** is used.
- The two bulkhead templates additionally propagate the registry's tags:
  `registry.bulkhead(name, getConfiguration(name), registry.getTags())`. Circuit breaker, rate limiter, retry
  and time limiter entries are created without tags.

> **Consequence (this trips people up):** to give one entry a bespoke configuration you must register the
> configuration under the *entry's* name before the entry is first resolved:
>
> ```java
> template.addConfiguration("checkout-service", circuitBreakerConfig); // name == entry name
> CircuitBreaker entry = template.getEntry("checkout-service");        // created WITH that config
> ```
>
> This is exactly what the project's own tests do in their `setUp()`. Registering a configuration under a
> different name has no effect on entry creation.

### 4.2 One-time vs. two-phase operations

`Resilience4jOperations` offers two shapes (the Javadoc of the interface calls them
*One-Time Operations* and *Two-Phase Operations*):

| Shape | API | When to use |
|-------|-----|-------------|
| One-time | `execute(name, …)` / `call(name, …)` | The whole protected call fits inside one lambda. The template handles enter/exit for you. |
| Two-phase | `begin(name)` … `end(context)` | You need to split the decoration around code you cannot move into a lambda (e.g. instrumentation spread over an interceptor's `before`/`after` methods, or a reactive pipeline). |

All one-time overloads funnel into **`call(String, ThrowableSupplier)`**, which is the single decoration
hook. See §7.1.

### 4.3 Priority = aspect order

Every template implements `io.microsphere.lang.Prioritized`. Its default priority is the module's
`defaultAspectOrder`, and `ChainableResilience4jFacade` sorts templates by priority **ascending**
(smaller value = outer layer):

| Module | `defaultAspectOrder` |
|--------|----------------------|
| `RETRY` | `0` |
| `CIRCUIT_BREAKER` | `1` |
| `RATE_LIMITER` | `2` |
| `TIME_LIMITER` | `3` |
| `BULKHEAD` | `4` |
| `THREAD_POOL_BULKHEAD` | `4` (same value as `BULKHEAD`) |

This reproduces the [Resilience4j recommended aspect order](https://resilience4j.readme.io/docs/getting-started-3#aspect-order):
`Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( Function ) ) ) ) )`.
You can change it per template with `setPriority(int)`. Beware of the `Prioritized` constants:
`Prioritized.MAX_PRIORITY` is `Integer.MIN_VALUE` and `MIN_PRIORITY` is `Integer.MAX_VALUE`, so
`setPriority(Prioritized.MAX_PRIORITY)` puts the template **outermost**, not innermost.

---

## 5. API reference — abstractions

### 5.1 `io.microsphere.resilience4j.common.Resilience4jOperations<E>`

The minimal contract for a single module. `E` is the Resilience4j entry type (e.g. `CircuitBreaker`).
All methods except `begin` and `end` are `default` methods.

| Method | Signature | Params | Returns | Throws |
|--------|-----------|--------|---------|--------|
| `execute` | `default void execute(String name, Runnable callback)` | `name` entry name; `callback` code without result | `void` | unchecked only (see note) |
| `execute` | `default <T> T execute(String name, Supplier<T> callback)` | `callback` code with result | result of the callback | any `Throwable` **re-thrown as-is if already a `RuntimeException`, otherwise wrapped into a `RuntimeException`** via `ExceptionUtils.wrap(t, RuntimeException.class)` |
| `call` | `default void call(String name, ThrowableAction callback) throws Throwable` | `ThrowableAction` = `void execute() throws Throwable` | `void` | `Throwable` (any error, including checked ones) |
| `call` | `default <TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR` | + `throwableClass` the exception type the caller declares | `void` | `TR` — the failure is re-typed/wrapped into `throwableClass` |
| `call` | `default <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `ThrowableSupplier<T>` = `T get() throws Throwable` | result | `Throwable` |
| `call` | `default <T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR` | + `throwableClass` | result | `TR` |
| `isBeginSupported` | `default boolean isBeginSupported()` | — | `true` unless the module overrides it | — |
| `begin` | `Resilience4jContext<E> begin(String name)` | **abstract** | non-null context (first phase) | typically `UnsupportedOperationException` when unsupported |
| `isEndSupported` | `default boolean isEndSupported()` | — | `true` unless overridden | — |
| `end` | `void end(Resilience4jContext<E> context)` | **abstract** | `void` (second phase) | typically `UnsupportedOperationException` when unsupported |

Behaviour that matters in practice:

- `execute(name, Runnable)`, `execute(name, Supplier)` and `call(name, ThrowableAction)` are all thin
  adapters on top of `call(name, ThrowableSupplier)`. Overriding `call(String, ThrowableSupplier)` in a
  subclass therefore changes the behaviour of **every** one-time overload.
- `execute` is the *unchecked* façade; `call` is the *throws-Throwable* façade. Pick `call` when your
  business code throws checked exceptions (JDBC, IO, `Feign` decode, …) and you do not want to wrap them.
- `execute(name, supplier)` never swallows anything: `wrap(t, RuntimeException.class)` returns the very same
  instance when `t instanceof RuntimeException`, so `CallNotPermittedException` or `BulkFullException`
  still reach you unchanged.
- The two-phase pair is *not* exception-safe by itself — `begin()` returns a context you must hand to `end()`.

### 5.2 `io.microsphere.resilience4j.common.AdvancedResilience4jOperations<E, C, R extends Registry<E, C>>`

`extends Resilience4jOperations<E>, io.microsphere.lang.Prioritized`. Adds registry introspection,
configuration/entry management, entry-level (`Consumer`/`Function`) execution, and logging access.

**Getters**

| Method | Returns | Notes |
|--------|---------|-------|
| `R getRegistry()` | the wrapped `Registry` | abstract in this interface |
| `Resilience4jModule getModule()` | the module | abstract in this interface |
| `Class<E> getEntryClass()` | e.g. `CircuitBreaker.class` | default: `getModule().getEntryClass()` (unchecked cast) |
| `Class<C> getConfigClass()` | e.g. `CircuitBreakerConfig.class` | default: from the module |
| `Logger getLogger()` | `io.microsphere.logging.Logger` | abstract; the template returns its own logger |

**Configuration operations**

| Method | Params | Returns | Behaviour |
|--------|--------|---------|-----------|
| `getDefaultConfig()` | — | non-null `C` | `registry.getDefaultConfig()` |
| `getConfiguration(String configName)` | `configName` | non-null `C` | `registry.getConfiguration(configName)`; **falls back to `getDefaultConfig()`** when absent (never null) |
| `addConfiguration(String configName, C configuration)` | name + config | `AdvancedResilience4jOperations<E, C, R>` (`this`) | `registry.addConfiguration(...)`; fluent |

**Entry operations**

| Method | Params | Returns | Behaviour |
|--------|--------|---------|-----------|
| `getEntry(String name)` | entry name | non-null `E` | `registry.find(name)` else `createEntry(name)` |
| `createEntry(String name)` | entry name | non-null `E` | **abstract** — implemented per template |
| `removeEntry(String name)` | entry name | `E` or `null` | `registry.remove(name)`; `null` when nothing was registered |
| `replaceEntry(String name, E newEntry)` | name, new entry | old `E` or `null` | `registry.replace(name, newEntry)`; when the name was unknown the new entry is added and `null` is returned |

**Entry-level one-time operations** (note: these do **not** decorate anything — they hand you the raw entry)

| Method | Params | Returns | Throws |
|--------|--------|---------|--------|
| `execute(String name, Consumer<E> entryConsumer)` | raw entry consumer | `this` (fluent) | unchecked only |
| `execute(String name, Function<E, T> entryFunction)` | raw entry function | `T` | unchecked only |
| `call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable` | raw entry consumer | `this` | `Throwable` |
| `call(String name, ThrowableConsumer<E> entryConsumer, Class<TR>) throws TR` | + target exception type | `this` | `TR` |
| `call(String name, ThrowableFunction<E, T> entryFunction) throws Throwable` | raw entry function | `T` | `Throwable` |
| `call(String name, ThrowableFunction<E, T> entryFunction, Class<TR>) throws TR` | + target exception type | `T` | `TR` |

> **Do not confuse the two families.** `execute(name, () -> …)` / `call(name, () -> …)` (zero-argument
> lambdas) are the **decorated** executions. `execute(name, entry -> …)` / `call(name, entry -> …)`
> (one-argument lambdas) are **raw entry access** — no `acquirePermission`, no decoration, no event.
> They exist for administration, e.g. `template.execute(name, CircuitBreaker::reset)` (verified in
> `CircuitBreakerTemplateTest`).

### 5.3 `io.microsphere.resilience4j.common.Resilience4jTemplate<E, C, R extends Registry<E, C>>`

`abstract class`, implements `AdvancedResilience4jOperations<E, C, R>`. This is the base class you extend
(or the six concrete subclasses you instantiate directly).

**Constructors**

| Constructor | Behaviour |
|-------------|-----------|
| `public Resilience4jTemplate(R registry)` | `this(registry, true)` — local entries cache **enabled** |
| `public Resilience4jTemplate(R registry, boolean localEntriesCached)` | `registry` must not be `null` (microsphere `Assert.assertNotNull`, message `"The registry must not be null"`); resolves `module` from `registry.getClass()` via `Resilience4jModule.valueOf(...)`; builds the registry `EventProcessor`; creates the local cache when `localEntriesCached` is `true`; sets `priority = module.getDefaultAspectOrder()` |

**Public / protected API**

| Method | Returns | Notes |
|--------|---------|-------|
| `final R getRegistry()` | non-null | |
| `final Resilience4jModule getModule()` | non-null | |
| `final C getDefaultConfig()` | non-null | delegates to the interface default |
| `final Class<E> getEntryClass()` / `final Class<C> getConfigClass()` | non-null | |
| `final Logger getLogger()` | non-null | |
| `final Resilience4jTemplate<E, C, R> initLocalEntriesCache(String name)` | `this` | warms the cache: resolves (thus **creates**) the entry and stores it |
| `final Resilience4jTemplate<E, C, R> initLocalEntriesCache(Iterable<String> names)` | `this` | same for many names |
| `final E getEntry(String name)` | non-null | cache → registry → `createEntry(name)` |
| `protected Map<String, E> createLocalEntriesCache()` | `HashMap` by default; `null` means "no cache" | override to customise or to disable |
| `protected final boolean isLocalEntriesCached()` | `boolean` | `localEntriesCache != null` |
| `protected final void clearLocalEntriesCache()` | `void` | |
| `protected final E getEntryFromCache(String name)` | `E` or `null` | |
| `final AdvancedResilience4jOperations<E, C, R> addConfiguration(String, C)` | `this` | |
| `final C getConfiguration(String configName)` | non-null | falls back to the default configuration |
| `final Resilience4jContext<E> begin(String name)` | non-null context | resolves the entry, builds `new Resilience4jContext<>(name, entry)`, then calls `doBegin(context)`. **`final`** — override the hook instead |
| `protected void doBegin(Resilience4jContext<E> context)` | `void` | default implementation **throws `UnsupportedOperationException`** with message `"<class> does not support begin operation"` |
| `final void end(Resilience4jContext<E> context)` | `void` | calls `doEnd(context)`. **`final`** |
| `protected void doEnd(Resilience4jContext<E> context)` | `void` | default implementation **throws `UnsupportedOperationException`** (`"… does not support end operation"`) |
| `final Resilience4jTemplate<E, C, R> onEntryAddedEvent(EventConsumer<EntryAddedEvent<E>>)` | `this` | registry-level lifecycle events |
| `final Resilience4jTemplate<E, C, R> onEntryRemovedEvent(EventConsumer<EntryRemovedEvent<E>>)` | `this` | |
| `final Resilience4jTemplate<E, C, R> onEntryReplacedEvent(EventConsumer<EntryReplacedEvent<E>>)` | `this` | |
| `protected final <T extends RegistryEvent> Resilience4jTemplate<E, C, R> registerRegistryEventConsumer(Class<? super T>, EventConsumer<T>)` | `this` | for custom registry events |
| `protected final <T> Resilience4jTemplate<E, C, R> registerEntryEventConsumer(String entryName, Class<? super T>, EventConsumer<T>)` | `this` | used by all the concrete `onXxxEvent(...)` methods; it resolves `getEntry(entryName)` first, so attaching a listener **creates the entry** when it is missing |
| `final int getPriority()` | priority | `Prioritized` implementation |
| `final Resilience4jTemplate<E, C, R> setPriority(int priority)` | `this` | affects facade ordering |
| `public void destroy()` | `void` | clears the local cache; call it when you retire a template |
| `String toString()` | — | `"<className>{registry = …, module = …, registryEventProcessor = …, localEntriesCache = …, priority = …}"` |

Because `begin`, `end`, `getEntry`, `getRegistry`, `getModule` and the priority accessors are `final`,
subclasses customise behaviour through `doBegin` / `doEnd` / `createEntry` / `call(String, ThrowableSupplier)`
only.

**Thread-safety of the local entries cache** — the Javadoc states it plainly: the cache is a `HashMap`,
i.e. *not* thread-safe, and it becomes safe **only if** it is fully populated at initialization time by
`initLocalEntriesCache(...)` before concurrent use. Reads/writes after that are safe because the entries are
already present. If you call `initLocalEntriesCache` lazily from request threads, disable the cache
(`new XxxTemplate(registry, false)`).

### 5.4 `Resilience4jFacade` and `ChainableResilience4jFacade`

`Resilience4jFacade` is a marker interface:

```java
public interface Resilience4jFacade extends Resilience4jOperations<Resilience4jContext[]> {
}
```

Its only entry type is `Resilience4jContext[]` — the array of per-template sub-contexts.

**`ChainableResilience4jFacade`** (`public class`, implements `Resilience4jFacade`)

| Constructor | Params | Behaviour |
|-------------|--------|-----------|
| `ChainableResilience4jFacade(Registry... registries)` | varargs registries | `this(Arrays.asList(registries))` |
| `ChainableResilience4jFacade(Collection<Registry> registries)` | registries | `Resilience4jUtils.createTemplates(registries)` — one template per **distinct** registry instance, already de-duplicated and priority-sorted; empty collection → empty list |
| `ChainableResilience4jFacade(List<Resilience4jTemplate> templates)` | ready-made templates | removes duplicates using `List#contains` (identity, since `Resilience4jTemplate` does not override `equals`), then sorts with `Prioritized.COMPARATOR` (ascending priority) |

| Method | Returns | Behaviour |
|--------|---------|-----------|
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | the callback's result | wraps the callback in a `CallbackChain` over all templates (see §5.5 and §7.9) |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` | outer context | allocates `new Resilience4jContext[size]`, calls `template.begin(name)` for every template whose `isBeginSupported()` is `true`, skips the others (leaving `null` slots) |
| `void end(Resilience4jContext<Resilience4jContext[]> context)` | `void` | calls `template.end(subContexts[i])` for every template whose `isEndSupported()` is `true`, **in the same forward order as `begin`** (not LIFO) |
| `List<Resilience4jTemplate> getTemplates()` | non-null, ordered | the effective template list |
| `int getSize()` | `>= 0` | the effective (de-duplicated) template count |
| `void destroy()` | `void` | `destroy()` on every template |

Inherited from `Resilience4jOperations`: `execute(name, Runnable)`, `execute(name, Supplier)`,
`call(name, ThrowableAction)`, `call(name, ThrowableAction, Class)` and the `isBeginSupported` /
`isEndSupported` defaults (both `true`). The facade does **not** implement
`AdvancedResilience4jOperations`, so `getRegistry()`, `getEntry(name)` and the raw entry-level
`Consumer`/`Function` overloads are **not** available on it.

### 5.5 `CallbackChain<T>` (package-private)

`class CallbackChain<T> implements ThrowableSupplier<T>` — **not public**, it lives in
`io.microsphere.resilience4j.common` and is created by `ChainableResilience4jFacade.call`. Its constructor is
`CallbackChain(String entryName, ThrowableSupplier<T> delegate, List<Resilience4jTemplate> templates)` and
`get()` walks the template list with a cursor (`pos`), calling
`templates.get(pos++).call(entryName, CallbackChain.this::get)` until the list is exhausted, then invoking the
real business callback. You never construct it yourself; you only observe it through facade ordering.

### 5.6 `Resilience4jContext<E>`

`public class`, a mutable per-execution holder created by `begin(...)`. Its constructor is **`protected`**, so
from ordinary application code you cannot call `new Resilience4jContext<>(...)` — you get instances from
`template.begin(name)` (or `facade.begin(name)`). Only classes in `io.microsphere.resilience4j.common`, its
subclasses, and test helpers living in that package (e.g. `AbstractResilience4jTemplateTest#testEnd`) can build
one directly.

| Member | Signature | Notes |
|--------|-----------|-------|
| constructor | `protected Resilience4jContext(String entryName, E entry)` | rejects `null` name/entry (microsphere `Assert.assertNotNull`) |
| `String getEntryName()` | non-null | |
| `E getEntry()` | non-null (for the facade it is the `Resilience4jContext[]` array) | |
| `setStartTime(Long)` / `getStartTime()` | fluent / nullable | `Long` (boxed) — `CircuitBreakerTemplate.doBegin` fills it, `BulkheadTemplate` and `RateLimiterTemplate` do not |
| `setResult(Object)` / `getResult()` | fluent / nullable | you must set it in two-phase use |
| `setFailure(Throwable)` / `getFailure()` | fluent / nullable | you must set it in two-phase use |
| `setAttribute(String, Object)` / `hasAttribute(String)` / `getAttribute(String)` / `getAttribute(String, T)` / `removeAttribute(String)` / `removeAttributes()` / `getAttributes()` | fluent / typed | attributes map is created lazily; `getAttributes()` returns `Collections.emptyMap()` until then, otherwise a read-only *live* view |
| `Resilience4jContext<E> withinContext()` | `this` | binds this context to the current thread (`ThreadLocal`) |
| `static Resilience4jContext getContext()` | nullable | current thread's context |
| `static void setContext(Resilience4jContext context)` | — | **replaces** the current context (`null` allowed) |
| `static Resilience4jContext removeContext()` | nullable | returns and unbinds |
| `static void doInContext(Consumer<Resilience4jContext> consumer)` | — | convenience: runs the consumer on the current context |
| `static void doInContext(Consumer<Resilience4jContext> consumer, boolean forRemoval)` | — | when `forRemoval` is `true`, the context is removed first |
| `String toString()` | — | `Resilience4jContext{entryName='…', entry=…, startTime=…, result=…, failure=…, attributes=…}` |

Two behaviours worth pinning down (both proven by `Resilience4jContextTest`):

1. **`doInContext` is silent when nothing is bound.** The test's last statement runs a consumer that
   throws `new RuntimeException("Impossible")` and expects nothing to happen — because
   `getContext()` returned `null`, the consumer is never invoked.
2. **The thread binding is a single slot, not a stack.** `setContext` overwrites; nested
   `begin(...)`/`withinContext()` calls lose the outer context unless you save and restore it yourself.

### 5.7 `Resilience4jModule`

`public enum` with six constants and their metadata. Constructor is enum-private; you consume it.

| Method | Returns |
|--------|---------|
| `String getName()` | `"retry"`, `"circuit-breaker"`, `"rate-limiter"`, `"time-limiter"`, `"bulkhead"`, `"thread-pool-bulkhead"` |
| `Class<?> getEntryClass()` | `Retry` / `CircuitBreaker` / `RateLimiter` / `TimeLimiter` / `Bulkhead` / `ThreadPoolBulkhead` |
| `Class<?> getConfigClass()` | `RetryConfig` / `CircuitBreakerConfig` / `RateLimiterConfig` / `TimeLimiterConfig` / `BulkheadConfig` / `ThreadPoolBulkheadConfig` |
| `Class<? extends CommonProperties> getConfigurationPropertiesClass()` | `CommonRetryConfigurationProperties` / `CommonCircuitBreakerConfigurationProperties` / `CommonRateLimiterConfigurationProperties` / `CommonTimeLimiterConfigurationProperties` / `CommonBulkheadConfigurationProperties` / `CommonThreadPoolBulkheadConfigurationProperties` |
| `Class<?> getEventClass()` | `RetryEvent` / `CircuitBreakerEvent` / `RateLimiterEvent` / `TimeLimiterEvent` / `BulkheadEvent` / `BulkheadEvent` (shared with `BULKHEAD`) |
| `Class<? extends Registry> getRegistryClass()` | `RetryRegistry` / `CircuitBreakerRegistry` / `RateLimiterRegistry` / `TimeLimiterRegistry` / `BulkheadRegistry` / `ThreadPoolBulkheadRegistry` |
| `int getDefaultAspectOrder()` | `0`, `1`, `2`, `3`, `4`, `4` |
| `static Resilience4jModule valueOf(Class<?> type)` | the module whose entry/config/properties/event/registry class is assignable from `type` |
| `String toString()` | `"Resilience4jModule{name = …, entryClass = …, configClass = …, configurationPropertiesClass = …, eventClass = …, registryClass = …, defaultAspectOrder = …}"` |

`valueOf(Class)` details (proven by `Resilience4jModuleTest`):

- It is an **overload** of the implicit `Enum.valueOf(String)`; call `valueOf(Retry.class)`,
  `valueOf(RetryConfig.class)`, `valueOf(RetryEvent.class)` or `valueOf(RetryRegistry.class)` — all four
  return `RETRY`.
- Members are scanned in declaration order (`RETRY`, `CIRCUIT_BREAKER`, `RATE_LIMITER`, `TIME_LIMITER`,
  `BULKHEAD`, `THREAD_POOL_BULKHEAD`) and the **first** match wins. Because `BULKHEAD` and
  `THREAD_POOL_BULKHEAD` share `BulkheadEvent`, `valueOf(BulkheadEvent.class)` yields `BULKHEAD`.
- No match → `IllegalArgumentException("The 'type' can't be found in Resilience4jModule : " + type.getName())`,
  e.g. `valueOf(Resilience4jModule.class)`.

### 5.8 `Resilience4jConstants`

`public interface` holding `String` constants (configuration-namespace building blocks, used by the Spring
modules and by plugin authors):

| Constant | Value |
|----------|-------|
| `PREFIX` | `"microsphere.resilience4j."` |
| `RETRY_MODULE_NAME` / `RETRY_PREFIX` | `"retry"` / `"microsphere.resilience4j.retry."` |
| `CIRCUIT_BREAKER_MODULE_NAME` / `CIRCUIT_BREAKER_PREFIX` | `"circuit-breaker"` / `"microsphere.resilience4j.circuit-breaker."` |
| `RATE_LIMITER_MODULE_NAME` / `RATE_LIMITER_PREFIX` | `"rate-limiter"` / `"microsphere.resilience4j.rate-limiter."` |
| `TIME_LIMITER_MODULE_NAME` / `TIME_LIMITER_PREFIX` | `"time-limiter"` / `"microsphere.resilience4j.time-limiter."` |
| `BULKHEAD_MODULE_NAME` / `BULKHEAD_PREFIX` | `"bulkhead"` / `"microsphere.resilience4j.bulkhead."` |
| `THREAD_POOL_BULKHEAD_MODULE_NAME` / `THREAD_POOL_BULKHEAD_PREFIX` | `"thread-pool-bulkhead"` / `"microsphere.resilience4j.thread-pool-bulkhead."` |

### 5.9 `io.microsphere.resilience4j.util.Resilience4jUtils`

`public abstract class ... implements io.microsphere.util.Utils` with a **private constructor** — static-only.

| Static method | Params | Returns | Throws |
|---------------|--------|---------|--------|
| `createTemplate(R registry)` | any `Registry` | the module's default `Resilience4jTemplate`, reflectively instantiated with the registry as the single constructor argument | — |
| `createTemplates(Collection<Registry> registries)` | registries | non-null **read-only** `List<Resilience4jTemplate>`, one per distinct registry, sorted by priority ascending; empty input → `Collections.emptyList()` | — |
| `getEntry(Registry<E, C> registry, String name)` | registry + name | entry created/fetched with the registry's **default** configuration | — |
| `getEntry(Registry<E, C> registry, String name, C configuration)` | + explicit configuration | entry created with that configuration via the registry's `"<entry>"(String, Config)` method | `IllegalArgumentException` wrapping the reflection failure when the configuration type does not belong to that registry |
| `getEventProcessor(E entry)` | any supported entry | `EventProcessor` for the entry's event publisher | `UnsupportedOperationException` for an entry of an unknown type |
| `getEventProcessor(Retry / TimeLimiter / RateLimiter / Bulkhead / ThreadPoolBulkhead / CircuitBreaker)` | typed entry | `EventProcessor` | — |
| `getEventProcessor(Registry registry)` | registry | the registry's `EventPublisher` as `EventProcessor` | `IllegalArgumentException` if the publisher is not an `EventProcessor` (or `null`) |

Notes verified from `Resilience4jUtilsTest`:

- `Resilience4jUtils.getEntry(registry, name)` bypasses both the local cache **and** named configurations;
  prefer `template.getEntry(name)`.
- `createTemplate(ThreadPoolBulkheadRegistry.ofDefaults())` returns a `ThreadPoolBulkheadTemplate`, i.e. all
  six modules are wired through `META-INF/default/templates.properties`.

---

## 6. API reference — the six templates

Every template exposes two constructors, `(registry)` and `(registry, boolean localEntriesCached)`, plus
module-typed `onXxxEvent(String entryName, EventConsumer<...>)` fluent registrations returning the template
itself. `TimeLimiterTemplate` has two extra constructors (below).

### 6.1 `CircuitBreakerTemplate` (`io.microsphere.resilience4j.circuitbreaker`)

`extends Resilience4jTemplate<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry>`

| Member | Details |
|--------|---------|
| `CircuitBreakerTemplate(CircuitBreakerRegistry registry)` | cache enabled |
| `CircuitBreakerTemplate(CircuitBreakerRegistry registry, boolean localEntriesCached)` | |
| `CircuitBreaker createEntry(String name)` | `registry.circuitBreaker(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `circuitBreaker.decorateCheckedSupplier(callback::get).get()` — full decorator, **bypasses `begin`/`end`** |
| `doBegin(Resilience4jContext<CircuitBreaker>)` | `acquirePermission()` + records `startTime` from `circuitBreaker.getCurrentTimestamp()` |
| `doEnd(Resilience4jContext<CircuitBreaker>)` | computes `duration = getCurrentTimestamp() - startTime`, then `onSuccess(duration, unit)` when no failure and no result, `onResult(duration, unit, result)` when a result is present, `onError(duration, unit, failure)` otherwise |
| `isBeginSupported()` / `isEndSupported()` | inherited `true` |

Event registrations: `onCallNotPermittedEvent`, `onErrorEvent`, `onFailureRateExceededEvent`,
`onIgnoredErrorEvent`, `onResetEvent`, `onSlowCallRateExceededEvent`, `onStateTransitionEvent`,
`onSuccessEvent` — each `(String entryName, EventConsumer<CircuitBreakerOn…Event>)`.

### 6.2 `RateLimiterTemplate` (`io.microsphere.resilience4j.ratelimiter`)

`extends Resilience4jTemplate<RateLimiter, RateLimiterConfig, RateLimiterRegistry>`

| Member | Details |
|--------|---------|
| constructors | `(RateLimiterRegistry)` / `(RateLimiterRegistry, boolean)` |
| `RateLimiter createEntry(String name)` | `registry.rateLimiter(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `RateLimiter.decorateCheckedSupplier(rateLimiter, callback::get).get()` |
| `public void doBegin(Resilience4jContext<RateLimiter>)` | `RateLimiter.waitForPermission(rateLimiter)` (declared `public`, widening the base class' `protected` hook) |
| `doEnd(Resilience4jContext<RateLimiter>)` | `onSuccess()` / `onResult(result)` / `onError(failure)` — no duration involved |
| Events | `onSuccessEvent`, `onFailureEvent`, `onDrainedEvent` |

### 6.3 `RetryTemplate` (`io.microsphere.resilience4j.retry`)

`extends Resilience4jTemplate<Retry, RetryConfig, RetryRegistry>`

| Member | Details |
|--------|---------|
| constructors | `(RetryRegistry)` / `(RetryRegistry, boolean)` |
| `Retry createEntry(String name)` | `registry.retry(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `Retry.decorateCheckedSupplier(retry, callback::get).get()` |
| `isBeginSupported()` | **`false`** |
| `isEndSupported()` | **`false`** |
| Events | `onErrorEvent`, `onIgnoredErrorEvent`, `onRetryEvent`, `onSuccessEvent` |

`begin(name)` / `end(context)` throw `UnsupportedOperationException`: retry needs the whole call body at
once and cannot be expressed as an enter/exit pair.

### 6.4 `BulkheadTemplate` (`io.microsphere.resilience4j.bulkhead`)

`extends Resilience4jTemplate<Bulkhead, BulkheadConfig, BulkheadRegistry>`

| Member | Details |
|--------|---------|
| constructors | `(BulkheadRegistry)` / `(BulkheadRegistry, boolean)` |
| `Bulkhead createEntry(String name)` | `registry.bulkhead(name, getConfiguration(name), registry.getTags())` — the registry's tags are propagated to the instance |
| `call(String name, ThrowableSupplier<T>)` | **not overridden** → the interface default `begin → callback → end` runs, so `execute`/`call` on a bulkhead go through the two-phase path |
| `doBegin(Resilience4jContext<Bulkhead>)` | `bulkhead.acquirePermission()` (throws `BulkFullException` when saturated past `maxWaitDuration`; it does **not** record `startTime`) |
| `doEnd(Resilience4jContext<Bulkhead>)` | `bulkhead.onComplete()` |
| Events | `onCallPermittedEvent`, `onCallRejectedEvent`, `onCallFinishedEvent` |

### 6.5 `ThreadPoolBulkheadTemplate` (`io.microsphere.resilience4j.bulkhead`)

`extends Resilience4jTemplate<ThreadPoolBulkhead, ThreadPoolBulkheadConfig, ThreadPoolBulkheadRegistry>`

| Member | Details |
|--------|---------|
| constructors | `(ThreadPoolBulkheadRegistry)` / `(ThreadPoolBulkheadRegistry, boolean)` |
| `ThreadPoolBulkhead createEntry(String name)` | `registry.bulkhead(name, getConfiguration(name), registry.getTags())` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | submits `() -> ThrowableSupplier.execute(callback)` to the bulkhead's pool via `executeCallable`, converts to `CompletableFuture` and **blocks on `future.get()`**; any `ExecutionException` is unwrapped to its **root cause** and rethrown |
| `isBeginSupported()` / `isEndSupported()` | **`false`** (`begin`/`end` throw `UnsupportedOperationException`) |
| Events | `onCallPermittedEvent`, `onCallRejectedEvent`, `onCallFinishedEvent` |
| helper | `static Throwable getRootCause(Throwable)` — package-private, walks `getCause()` to the deepest cause |

Consequences: the callback runs on the pool thread (no `ThreadLocal` inheritance), failures arrive as the
*deepest* cause (a user exception that itself has a cause loses its wrapper), and `call` blocks, so the
"async" bulkhead behaves synchronously through this template.

### 6.6 `TimeLimiterTemplate` (`io.microsphere.resilience4j.timelimiter`)

`extends Resilience4jTemplate<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry>`

| Member | Details |
|--------|---------|
| `TimeLimiterTemplate(TimeLimiterRegistry registry)` | executor = `ForkJoinPool.commonPool()`, cache enabled |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, boolean localEntriesCached)` | executor = common pool |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, ExecutorService executorService)` | cache enabled |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, ExecutorService executorService, boolean localEntriesCached)` | full control |
| `TimeLimiter createEntry(String name)` | `registry.timeLimiter(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `timeLimiter.decorateFutureSupplier(() -> executorService.submit(...)).call()`; inside the task the callback's `Throwable` is converted with `ExceptionUtils.wrap(t, Exception.class)` because `Callable` may only throw `Exception` |
| `isBeginSupported()` / `isEndSupported()` | **`false`** |
| Events | `onSuccessEvent`, `onErrorEvent`, `onTimeoutEvent` |

A `TimeLimiterConfig` with `cancelRunningFuture(true)` cancels the submitted task on timeout — but the
callback thread itself is only interrupted; it is not guaranteed to stop.

### 6.7 Template capability matrix

| Template | overrides `call(String, ThrowableSupplier)` | `doBegin` / `doEnd` | `isBeginSupported` / `isEndSupported` | Extra state |
|----------|-------------------------------------------|---------------------|----------------------------------------|-------------|
| `CircuitBreakerTemplate` | yes (decorator) | implemented | `true` / `true` | records `startTime`, duration-based reporting |
| `RateLimiterTemplate` | yes (decorator) | implemented | `true` / `true` | `doBegin` is `public` |
| `RetryTemplate` | yes (decorator) | not implemented | **`false` / `false`** | — |
| `BulkheadTemplate` | **no** → uses `begin`/`end` | implemented | `true` / `true` | does not record `startTime` |
| `ThreadPoolBulkheadTemplate` | yes (pool + blocking `get()`) | not implemented | **`false` / `false`** | root-cause unwrapping |
| `TimeLimiterTemplate` | yes (executor + `Callable`) | not implemented | **`false` / `false`** | owns an `ExecutorService` |

---

## 7. Code examples

All examples are taken or adapted from the module's own test sources
(`.../src/test/java/io/microsphere/resilience4j/...`); they compile against the signatures above.
`entryName` is always `"test-entry"` or `"test"` as in the tests.

### 7.1 Circuit breaker: one-time execution + success event

Adapted from `CircuitBreakerTemplateTest#testExecute`.

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
CircuitBreakerTemplate template = new CircuitBreakerTemplate(registry);

String entryName = "test-entry";

template.onSuccessEvent(entryName, event -> {
    // event is a CircuitBreakerOnSuccessEvent
    System.out.printf("circuit breaker '%s' succeeded%n", event.getCircuitBreakerName());
});

// execute(String, Supplier<String>) -> the decorator path (CircuitBreaker.decorateCheckedSupplier)
String result = template.execute(entryName, () -> entryName);
assert result.equals("test-entry");

template.destroy();
```

### 7.2 Circuit breaker: bespoke configuration per entry name

Adapted from `AbstractResilience4jTemplateTest#setUp` + `CircuitBreakerTemplateTest#createEntryConfig`.

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

import java.time.Duration;

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .maxWaitDurationInHalfOpenState(Duration.ofMillis(50))
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .slowCallDurationThreshold(Duration.ofMillis(50))
        .permittedNumberOfCallsInHalfOpenState(2)
        .slowCallRateThreshold(10)
        .writableStackTraceEnabled(false)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();

// The configuration must be registered under the ENTRY name before the entry is first resolved.
template.addConfiguration("test-entry", config);

CircuitBreaker entry = template.getEntry("test-entry"); // created with `config`
assert entry.getState() == CircuitBreaker.State.CLOSED;
assert template.getEntry("test-entry") == entry;        // registry keeps one instance per name
```

### 7.3 Circuit breaker: error, ignored error, reset, and the raw-entry overload

Adapted from `CircuitBreakerTemplateTest#testExecuteOnCallFailure`. Shows how the one-argument
`execute(String, Consumer<CircuitBreaker>)` overload differs from the zero-argument decorated one, and how
checked exceptions surface from `call`.

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

String entryName = "test-entry";

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
template.addConfiguration(entryName, CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .build());

template.onErrorEvent(entryName, event -> System.out.println("ERROR: " + event.getThrowable()));
template.onIgnoredErrorEvent(entryName, event -> System.out.println("IGNORED: " + event.getThrowable()));
template.onFailureRateExceededEvent(entryName, event -> System.out.println("failure rate exceeded"));
template.onStateTransitionEvent(entryName, event -> System.out.println("state: " + event.getStateTransition()));
template.onCallNotPermittedEvent(entryName, event -> System.out.println("not permitted"));
template.onResetEvent(entryName, event -> System.out.println("reset"));

// call(String, ThrowableAction) throws Throwable - checked exceptions are recorded, not wrapped
try {
    template.call(entryName, () -> {
        throw new Exception("For testing");
    });
} catch (Throwable ignored) {
    // the recorded failure reaches the circuit breaker first
}

// RuntimeException is in `ignoreExceptions(...)`, so it is reported as an ignored error
try {
    template.call(entryName, () -> {
        throw new RuntimeException();
    });
} catch (RuntimeException ignored) {
}

// One-argument lambda == AdvancedResilience4jOperations.execute(String, Consumer<CircuitBreaker>):
// raw entry access, NO decoration.
template.execute(entryName, CircuitBreaker::reset);
```

### 7.4 Bulkhead: limit concurrency to one call

Adapted from `BulkheadTemplateTest#testExecute` / `#testExecuteOnCallRejected`.

```java
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
BulkheadTemplate template = new BulkheadTemplate(registry);

String entryName = "test-entry";
template.addConfiguration(entryName, BulkheadConfig.custom()
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ofMillis(100))
        .build());

template.onCallPermittedEvent(entryName, event -> System.out.println("permitted " + event.getBulkheadName()));
template.onCallFinishedEvent(entryName, event -> System.out.println("finished " + event.getBulkheadName()));
template.onCallRejectedEvent(entryName, event -> System.out.println("rejected " + event.getBulkheadName()));

// BulkheadTemplate does not override call(...), so this runs begin -> callback -> end
Object result = template.execute(entryName, () -> null);
assert result == null;

// Contention: a saturated bulkhead throws BulkFullException from execute(...)
ExecutorService executorService = newFixedThreadPool(3);
for (int i = 0; i < 3; i++) {
    executorService.execute(() -> template.execute(entryName, () -> {
    }));
}
executorService.shutdown();

template.destroy();
```

### 7.5 Rate limiter: permits per period

Adapted from `RateLimiterTemplateTest#execute`.

```java
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate;

import java.time.Duration;

RateLimiterTemplate template = new RateLimiterTemplate(RateLimiterRegistry.ofDefaults());

String entryName = "test-entry";
template.addConfiguration(entryName, RateLimiterConfig.custom()
        .limitForPeriod(1)
        .limitRefreshPeriod(Duration.ofMillis(50))
        .timeoutDuration(Duration.ofMillis(50))
        .build());

template.onSuccessEvent(entryName, event ->
        System.out.println(event.getRateLimiterName() + " permits=" + event.getNumberOfPermits()));
template.onFailureEvent(entryName, event -> System.out.println("acquire failed"));
template.onDrainedEvent(entryName, event -> System.out.println("drained"));

for (int i = 0; i < 5; i++) {
    template.execute(entryName, () -> {          // execute(String, Runnable)
    });
    template.execute(entryName, () -> "For testing"); // execute(String, Supplier<String>)
}
```

### 7.6 Retry with a checked exception

Adapted from `RetryTemplateTest#testExecute` and `#testExecuteOnIgnoredException`.
Note the enclosing method must declare `throws Throwable` (or catch it) because
`call(String, ThrowableSupplier)` declares `Throwable`.

```java
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.retry.RetryTemplate;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public void retryExample() throws Throwable {
    RetryTemplate template = new RetryTemplate(RetryRegistry.ofDefaults());

    String entryName = "test-entry";
    int maxAttempts = 3;
    template.addConfiguration(entryName, RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .ignoreExceptions(IllegalStateException.class)
            .retryExceptions(TimeoutException.class)
            .build());

    AtomicInteger attempts = new AtomicInteger(maxAttempts);

    template.onRetryEvent(entryName, event ->
            System.out.println("retry #" + event.getNumberOfRetryAttempts()));
    template.onSuccessEvent(entryName, event ->
            System.out.println("success after " + event.getNumberOfRetryAttempts() + " retries"));
    template.onErrorEvent(entryName, event ->
            System.out.println("final error: " + event.getThrowable()));
    template.onIgnoredErrorEvent(entryName, event ->
            System.out.println("ignored error: " + event.getThrowable()));

    String result = template.call(entryName, () -> {
        if (attempts.decrementAndGet() == 0) {
            return "OK";
        }
        throw new TimeoutException("For testing"); // retried
    });
    assert "OK".equals(result);

    // IllegalStateException is in `ignoreExceptions` -> propagated immediately, no retry
    try {
        template.call(entryName, () -> {
            throw new IllegalStateException("For testing");
        });
    } catch (IllegalStateException expected) {
    }
}
```

### 7.7 Time limiter with your own executor

Adapted from `TimeLimiterTemplateTest`.

```java
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newFixedThreadPool;

ExecutorService executorService = newFixedThreadPool(1);
TimeLimiterTemplate template = new TimeLimiterTemplate(TimeLimiterRegistry.ofDefaults(), executorService);

String entryName = "test-entry";
long timeoutMillis = 50;
template.addConfiguration(entryName, TimeLimiterConfig.custom()
        .cancelRunningFuture(true)
        .timeoutDuration(Duration.ofMillis(timeoutMillis))
        .build());

template.onSuccessEvent(entryName, event -> System.out.println("ok " + event.getTimeLimiterName()));
template.onErrorEvent(entryName, event -> System.out.println("error " + event.getThrowable()));
template.onTimeoutEvent(entryName, event -> System.out.println("timeout " + event.getTimeLimiterName()));

template.execute(entryName, () -> {   // completes within the timeout
});

// Blocking two-phase calls are not supported:
// assertThrows(UnsupportedOperationException.class, () -> template.begin(entryName));

try {
    template.call(entryName, () -> {
        Thread.sleep(timeoutMillis * 3);
    });
} catch (TimeoutException expected) {
    // the callback's own exception (if any) arrives wrapped as an Exception subtype
}

executorService.shutdown();
```

### 7.8 Thread-pool bulkhead

Adapted from `ThreadPoolBulkheadTemplateTest#testOnEvents`.

```java
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate;

ThreadPoolBulkheadTemplate template = new ThreadPoolBulkheadTemplate(ThreadPoolBulkheadRegistry.ofDefaults());

String entryName = "test-entry";
template.addConfiguration(entryName, ThreadPoolBulkheadConfig.custom()
        .coreThreadPoolSize(1)
        .maxThreadPoolSize(2)
        .queueCapacity(10)
        .build());

template.onCallPermittedEvent(entryName, event -> System.out.println("permitted " + event.getBulkheadName()));
template.onCallFinishedEvent(entryName, event -> System.out.println("finished " + event.getBulkheadName()));
template.onCallRejectedEvent(entryName, event -> System.out.println("rejected " + event.getBulkheadName()));

// Runs on a pool thread but blocks until the result is available.
String result = template.execute(entryName, () -> entryName);
assert "test-entry".equals(result);
```

### 7.9 Chainable facade: six modules around one call

Adapted from `ChainableResilience4jFacadeTest` with `Resilience4jTestUtils.createChainableResilience4jFacade()`
inlined (that helper lives in `src/test/java`, so it is **not** on your classpath — copy the logic):

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry,
        bulkheadRegistry,          // duplicated registries collapse into one template
        rateLimiterRegistry,
        rateLimiterRegistry,
        timeLimiterRegistry,
        timeLimiterRegistry,
        retryRegistry,
        circuitBreakerRegistry,
        circuitBreakerRegistry);

assert facade.getSize() == 5;                 // 5 distinct registries -> 5 templates
assert facade.getTemplates().size() == 5;
// priority-sorted, lowest first:
// RetryTemplate(0) > CircuitBreakerTemplate(1) > RateLimiterTemplate(2)
//   > TimeLimiterTemplate(3) > BulkheadTemplate(4)

String result = facade.execute("test-entry", () -> "Hello,World"); // wrapped by all five
assert "Hello,World".equals(result);

facade.destroy();
```

The same facade with explicit templates (also de-duplicated, note the `List<Resilience4jTemplate>`
constructor):

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

import static java.util.Arrays.asList;

BulkheadTemplate bulkheadTemplate = new BulkheadTemplate(BulkheadRegistry.ofDefaults());

// The inline list lets Java infer the constructor's List<Resilience4jTemplate> parameter
// (the same way ChainableResilience4jFacadeTest#testConstructor does).
ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        asList(bulkheadTemplate, bulkheadTemplate, bulkheadTemplate));

assert facade.getSize() == 1;   // duplicates removed by identity
```

### 7.10 Facade two-phase execution

Adapted from `ChainableResilience4jFacadeTest#testBeginAndEnd`:

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        RetryRegistry.ofDefaults(), CircuitBreakerRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
        BulkheadRegistry.ofDefaults());

Resilience4jContext<Resilience4jContext[]> context = facade.begin("test-entry");
Resilience4jContext[] subContexts = context.getEntry();
assert subContexts.length == facade.getSize();   // 5 slots

// Only CircuitBreaker / RateLimiter / Bulkhead are begin-supported, so the Retry
// and TimeLimiter slots stay null.
for (Resilience4jContext sub : subContexts) {
    System.out.println(sub == null ? "skipped" : sub.getEntryName());
}

facade.end(context);   // symmetric forward-order end of the non-null slots
```

### 7.11 Manual two-phase circuit breaker (correct pattern)

Derived from `AbstractResilience4jTemplateTest#testEnd`, which is the only place the module itself drives
`begin`/`end` directly. The important part: **you must populate `result` or `failure` yourself** — the
`Resilience4jContext` is not filled automatically, and `CircuitBreakerTemplate.doEnd` reads both fields.

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jContext;

String entryName = "test-entry";
CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());

// doBegin() already records startTime for the circuit breaker
Resilience4jContext<CircuitBreaker> context = template.begin(entryName);
try {
    String result = invokeRemoteService();
    context.setResult(result);      // -> circuitBreaker.onResult(duration, unit, result)
} catch (Throwable failure) {
    context.setFailure(failure);    // -> circuitBreaker.onError(duration, unit, failure)
} finally {
    template.end(context);          // -> circuitBreaker.onSuccess(duration, unit) if both are null
}
```

If you use `begin`/`end` with `BulkheadTemplate`, no `startTime` is recorded, so never call `end()` on a
context you created yourself for a circuit breaker without a prior `begin()` — `doEnd` unboxes
`context.getStartTime()` into a `long` and would raise a `NullPointerException`.

### 7.12 `Resilience4jContext` attributes and thread binding

Adapted from `Resilience4jContextTest`:

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jContext;

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
Resilience4jContext<CircuitBreaker> context = template.begin("test-entry");

context.setAttribute("tenant", "acme");
assert context.hasAttribute("tenant");
String tenant = context.getAttribute("tenant");            // <T> T getAttribute(String)
String fallback = context.getAttribute("operator", "none"); // default value variant
context.removeAttribute("tenant");
assert !context.hasAttribute("tenant");
assert context.getAttributes().isEmpty();                    // read-only view

// Thread binding (single slot - not a stack)
context.withinContext();
assert Resilience4jContext.getContext() == context;

Resilience4jContext.doInContext(ctx -> ctx.setResult("For testing"));        // keeps the binding
Resilience4jContext.doInContext(ctx -> System.out.println(ctx.getEntryName()), true); // removes it

assert Resilience4jContext.getContext() == null;
// doInContext silently does nothing when no context is bound
Resilience4jContext.doInContext(ctx -> {
    throw new IllegalStateException("never reached");
});
```

### 7.13 `Resilience4jUtils` and `Resilience4jModule`

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.resilience4j.util.Resilience4jUtils;

import java.util.Arrays;
import java.util.List;

CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

// Factory: registry -> the module's default template, resolved from
// META-INF/default/templates.properties
CircuitBreakerTemplate template = Resilience4jUtils.createTemplate(circuitBreakerRegistry);

// Bulk factory: one template per distinct registry, priority-sorted, read-only list
List<Resilience4jTemplate> templates = Resilience4jUtils.createTemplates(Arrays.asList(
        RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults()));
assert templates.size() == 2;

// Module metadata
Resilience4jModule module = Resilience4jModule.valueOf(CircuitBreakerRegistry.class);
assert module == Resilience4jModule.CIRCUIT_BREAKER;
assert module.getName().equals("circuit-breaker");
assert module.getDefaultAspectOrder() == 1;

// Event plumbing: registry- and entry-level EventProcessor access
// (getEventProcessor(Object) would throw UnsupportedOperationException)
Resilience4jUtils.getEventProcessor(circuitBreakerRegistry);
Resilience4jUtils.getEventProcessor(template.getEntry("test-entry"));
```

---

## 8. Behavioral notes

### 8.1 Which overload am I calling?

| Your code | Resolved method | Decorated? |
|-----------|-----------------|------------|
| `execute(name, () -> {})` | `execute(String, Runnable)` | yes |
| `execute(name, () -> value)` | `execute(String, Supplier<T>)` | yes |
| `execute(name, entry -> {})` | `AdvancedResilience4jOperations.execute(String, Consumer<E>)` | **no** (raw entry) |
| `execute(name, entry -> entry)` | `AdvancedResilience4jOperations.execute(String, Function<E, T>)` | **no** (raw entry) |
| `call(name, () -> value)` | `call(String, ThrowableSupplier<T>)` | yes |
| `call(name, () -> {})` | `call(String, ThrowableAction)` | yes |
| `call(name, entry -> {})` | `call(String, ThrowableConsumer<E>)` | **no** (raw entry) |
| `call(name, entry -> value)` | `call(String, ThrowableFunction<E, T>)` | **no** (raw entry) |
| `call(name, action, IllegalStateException.class)` | `call(String, ThrowableAction, Class<TR>)` | yes, failure re-typed to `TR` |

Java picks the void-compatible overload for `{}` bodies and the value-compatible one for bodies that return,
so both families coexist without ambiguity. If you want no surprises, assign the lambda to an explicitly
typed local variable.

### 8.2 `execute` vs `call`

- `execute` never declares checked exceptions. Failures that are already `RuntimeException` instances are
  re-thrown untouched; anything else (checked exceptions, `Error`) is wrapped with
  `ExceptionUtils.wrap(t, RuntimeException.class)`, which returns the same instance when the target type is
  assignable and otherwise creates a new instance of the target type.
- `call` declares `throws Throwable`. Use it when you want the original exception identity preserved
  (`TimeoutException` from `TimeLimiterTemplate`, `BulkFullException`, `CallNotPermittedException`, your own
  checked exceptions).
- `call(name, callback, SomeException.class)` is the ergonomic variant for a method that declares
  `throws SomeException`.

### 8.3 `execute` / `call` vs `begin` / `end`

- The one-time path always ends up in `call(String, ThrowableSupplier)`. Five of the six templates override
  it with a Resilience4j decorator; `BulkheadTemplate` keeps the interface default and therefore runs
  `begin(name)` → callback → `end(context)` in a `finally` block.
- The two-phase path is only about the *entry-level* hooks. Because `begin`/`end` are `final` in
  `Resilience4jTemplate`, the per-module semantics live in `doBegin`/`doEnd`:
  `CircuitBreaker` = permission + timing report; `RateLimiter` = permission + result report;
  `Bulkhead` = permission + completion; `Retry` / `TimeLimiter` / `ThreadPoolBulkhead` = unsupported.
- `begin()` does **not** catch failures from earlier phases and `end()` is not invoked automatically — if
  you use two-phase calls, put `end(context)` in a `finally`.
- A context obtained from `begin()` carries no result/failure unless you set them (see §7.11).

### 8.4 Facade ordering and nesting

Templates are sorted by priority **ascending**, and the `CallbackChain` calls index `0` first, so a smaller
priority value means the module is the **outer** wrapper. With all six modules you get the Resilience4j
recommended order. `THREAD_POOL_BULKHEAD` and `BULKHEAD` share priority `4`; a tie is broken by the order in
which the templates were collected (the sort is stable), so if you need `ThreadPoolBulkhead` strictly inside
`Bulkhead`, set priorities explicitly with `setPriority`.

Note also that `facade.begin()/facade.end()` unwind in the **same** forward order as they were opened
(not LIFO), and templates that report `isBeginSupported() == false` leave `null` slots in the sub-context
array.

### 8.5 Duplication rules for the facade

- `(Registry...)` / `(Collection<Registry>)` constructors collapse **equal registry instances**
  (`Resilience4jUtils.createTemplates` keys a map on the `Registry` object), so passing the same registry
  twice yields one template.
- The `(List<Resilience4jTemplate>)` constructor collapses **identical template instances**
  (`List#contains`, i.e. reference equality because `Resilience4jTemplate` does not override `equals`).
  Two `BulkheadTemplate`s wrapping two *different* `BulkheadRegistry` instances are therefore both kept.

### 8.6 Local entries cache

Enabled by default. It short-circuits `getEntry(name)` and is a plain `HashMap`, so: entries pre-created via
`initLocalEntriesCache(...)` at startup are safe for concurrent reads; entries resolved concurrently at
runtime are not. `clearLocalEntriesCache()` is invoked by `destroy()`. Removing an entry from the registry
(`removeEntry`) does **not** evict it from the cache — re-create the template or clear it if you rely on
fresh lookups.

### 8.7 `Resilience4jUtils` initialization

`Resilience4jUtils` loads `META-INF/default/templates.properties` during class initialization and maps every
`Resilience4jModule` to its template class
(`retry → RetryTemplate`, `circuit-breaker → CircuitBreakerTemplate`, `rate-limiter → RateLimiterTemplate`,
`time-limiter → TimeLimiterTemplate`, `bulkhead → BulkheadTemplate`,
`thread-pool-bulkhead → ThreadPoolBulkheadTemplate`). Because the map is built eagerly for **all six**
modules, using `Resilience4jUtils` (or `ChainableResilience4jFacade(Registry...)`) requires every Resilience4j
module artifact — including `resilience4j-timelimiter` and `resilience4j-framework-common` — on the classpath,
even if your application only uses circuit breakers. Instantiate the templates directly if you want to ship a
smaller dependency set.

### 8.8 Lifecycle

Templates are cheap wrappers around a registry but they do keep state (local cache, event consumers, and for
`TimeLimiterTemplate` a reference to an `ExecutorService`). Call `destroy()` (or `facade.destroy()`) when you
retire them; the tests use `@AfterEach` for exactly that.

---

## 9. Known limitations

| Limitation | Evidence | Workaround |
|------------|----------|------------|
| `Retry`, `TimeLimiter` and `ThreadPoolBulkhead` cannot be used through `begin`/`end`. | `isBeginSupported()`/`isEndSupported()` return `false`; `doBegin`/`doEnd` throw `UnsupportedOperationException`. | Use the one-time `execute`/`call` forms, or extend the template and implement the hooks yourself. |
| The default `call(String, ThrowableSupplier)` never writes `result`/`failure` into the context. | `Resilience4jOperations.call` body. | Only `BulkheadTemplate` uses that path today; for two-phase circuit-breaker use, set the fields yourself (§7.11). |
| `Resilience4jContext` thread binding is one slot, no nesting stack, and `doInContext` no-ops silently when unbound. | `ThreadLocal` + `Resilience4jContextTest`. | Save/restore the previous context around nested scopes. |
| All module dependencies are `optional`, so nothing is transitive. | `microsphere-resilience4j-commons/pom.xml`. | Declare your own Resilience4j + `microsphere-java-core` dependencies (or use the Spring modules, which bring them). |
| `ThreadPoolBulkheadTemplate.call` blocks and rethrows only the *root* cause. | `executeCallable(...).toCompletableFuture().get()` + `getRootCause`. | Don't rely on intermediate wrappers (`ExecutionException`, `CompletionException`) surviving. |
| `Resilience4jModule.valueOf(Class)` returns the **first** matching constant; `BulkheadEvent` resolves to `BULKHEAD`, not `THREAD_POOL_BULKHEAD`. | declaration order in `Resilience4jModule`. | Match on the registry/entry class you actually own. |

---

## 10. Next steps

- Internals, flow diagrams and extension points →
  [Developer Guide — microsphere-resilience4j-commons](../developer-guide/microsphere-resilience4j-commons.md)
- Spring annotation-driven usage → [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- Back to the guide index → [README.md](README.md) · 中文 → [zh/README.md](zh/README.md)
