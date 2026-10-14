# microsphere-resilience4j-hibernate-core — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← user guide index](README.md)

## What this plugin does

`microsphere-resilience4j-hibernate-core` puts entity-persistence operations under Resilience4j control by implementing the **Hibernate entity-callback extension point** shipped by `io.github.microsphere-projects:microsphere-hibernate-core`. One class — `Resilience4jHibernateEntityCallback` — observes the four entity lifecycle event pairs Hibernate fires (`LOAD`, `INSERT`, `UPDATE`, `DELETE`) and turns each pair into one **two-phase** Resilience4j call:

| Pre-event | Post-event | Resilience4j operation | Default entry name |
|-----------|------------|------------------------|--------------------|
| `onPreLoad` | `onPostLoad` | `facade.begin(name)` … `facade.end(context)` | `hibernate@<entity class name>@LOAD` |
| `onPreInsert` | `onPostInsert` | `begin` … `end` | `hibernate@<entity class name>@INSERT` |
| `onPreUpdate` | `onPostUpdate` | `begin` … `end` | `hibernate@<entity class name>@UPDATE` |
| `onPreDelete` | `onPostDelete` | `begin` … `end` | `hibernate@<entity class name>@DELETE` |

### The problem it solves

The other plugins of this library wrap a **call you can see**: a Feign request, a MyBatis statement, an HTTP handler method, a Druid connection. Entity state changes do not look like calls at all — `session.save(order)` returns immediately, and the SQL is flushed later, often inside a transaction commit that no application code explicitly invokes. There is nothing to decorate with `facade.call(...)`.

This plugin gives you a resource-protection boundary **per entity class and per lifecycle action**, without changing your DAO code:

- a `CircuitBreaker` per entity action can stop a persist/merge storm when the database is degraded,
- a `Bulkhead` caps how many concurrent writes of a given entity are in flight,
- a `RateLimiter` throttles writes to a hot table.

Because pre- and post-events are two different callbacks, the plugin uses the `begin()/end()` pair of `Resilience4jFacade` and carries the `Resilience4jContext` between them in a `ThreadLocal` — the same mechanism the [Spring Web plugin](microsphere-resilience4j-spring-web.md) uses (there with a request attribute instead).

> **Scope note.** Protection is *admission control around the entity event*, not around the JDBC statement. The flush may happen after the post-event, and failures raised during flush/commit are reported to Hibernate, not to this callback. Use the [Druid](microsphere-resilience4j-alibaba-druid.md) or [MyBatis](microsphere-resilience4j-mybatis.md) plugins when you need per-SQL decoration or retry/timeout behaviour.

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.2.4</version>   <!-- REQUIRED: not managed by the BOM, see below -->
</dependency>
```

> **Important — this artifact is NOT managed by the BOM (verified).** `microsphere-resilience4j-dependencies` lists exactly six artifacts (`commons`, `spring`, `alibaba-druid`, `mybatis`, `openfeign`, `spring-web`) in its `<dependencyManagement>`; `microsphere-resilience4j-hibernate-core` is **absent**, even though it is built in the reactor under `microsphere-resilience4j-plugins`. Importing the BOM will therefore **not** fill in a version for it — you must pin the version yourself, or add your own `<dependencyManagement>` entry:
>
> ```xml
> <dependencyManagement>
>     <dependencies>
>         <dependency>
>             <groupId>io.github.microsphere-projects</groupId>
>             <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
>             <version>0.2.4</version>
>         </dependency>
>     </dependencies>
> </dependencyManagement>
> ```
>
> Keeping that version in sync with your BOM import is manual. See the [dependencies page](microsphere-resilience4j-dependencies.md#gotcha-the-hibernate-plugin-is-not-in-the-bom).

### What the module POM pulls in

From `microsphere-resilience4j-plugins/microsphere-resilience4j-hibernate-core/pom.xml`:

| Dependency | Scope in this POM | Consequence for you |
|------------|-------------------|---------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | **compile** (version `${revision}`) | brings `microsphere-resilience4j-commons` (facade, templates) and the Spring `@Enable*` layer |
| `io.github.microsphere-projects:microsphere-hibernate-core` | **compile**, no `<version>` | supplies the `EntityCallback` interface; version comes from `microsphere-hibernate-dependencies:0.2.10`, imported by `microsphere-resilience4j-parent` |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | only needed by the Spring-side helpers of the `spring` layer |
| `io.github.microsphere-projects:microsphere-annotation-processor` | **optional** | build-time only |
| `org.hibernate.orm:hibernate-core` | **optional** | **you must provide it** — Hibernate 6.x (`org.hibernate.orm` groupId), because `org.hibernate.type.Type` appears in the API |
| `io.github.resilience4j:resilience4j-annotations`, `-consumer`, `-framework-common`, `-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter` | **optional** | `-framework-common` is the only one with an explicit `${resilience4j.version}`; add the modules you actually use |
| `org.springframework:spring-context` | **optional** | needed only for the Spring wiring shown below |
| `junit-jupiter`, `spring-test`, `microsphere-spring-test`, `microsphere-hibernate-test`, `h2`, `logback-classic` | **test** | used by the module's own test class only |

## Version and compatibility

| Item | Requirement | Where verified |
|------|-------------|----------------|
| microsphere-resilience4j | `0.2.x` (`release` branch), artifact `microsphere-resilience4j-hibernate-core` | `microsphere-resilience4j-plugins/pom.xml` `<modules>` |
| Introduced in | `0.2.2` | `release-notes.md` — “Hibernate Support: Added Hibernate core plugin and test utilities” |
| Resilience4j | 2.0.0 – 2.4.0 | parent `<profiles>` `resilience4j-2.0` … `resilience4j-2.4` (default `2.4.0`) |
| Hibernate | `org.hibernate.orm:hibernate-core` (application-supplied, optional) | module POM |
| microsphere-hibernate | `0.2.10` (BOM `microsphere-hibernate-dependencies`) | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud | 2022.0.x – 2025.0.x | CI matrix profiles `spring-cloud-2022` … `spring-cloud-2025` |
| JDK | **17+** | root `pom.xml` `<java.version>17</java.version>`; CI builds on 17 / 21 / 25 |

The `1.x` (`release-1.x`, v0.1.x) line also carries a `microsphere-resilience4j-hibernate-core` module; use the [1.x guide](../../1.x/user-guide/README.md) for that version line.

## Public API

`io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback` — the only production class in the module. `public class Resilience4jHibernateEntityCallback implements EntityCallback` (`io.microsphere.hibernate.entity.EntityCallback`).

### Construction and configuration

The class declares **no constructor at all**, so the implicit public no-arg constructor is the only way to instantiate it. Both collaborators are set through JavaBean setters (this is what makes it usable as a Spring bean with property injection):

| Member | Behaviour (from source) |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "hibernate@"` | initial value of the `entryNamePrefix` field |
| `Resilience4jHibernateEntityCallback()` | implicit default constructor; `facade` field is **uninitialised** (`null`) |
| `void setFacade(@Nonnull Resilience4jFacade facade)` | `assertNotNull(facade, …)` then assigns; message: `'Resilience4jFacade' argument must not be null!` |
| `void setEntryNamePrefix(@Nonnull String entryNamePrefix)` | `assertNotNull(entryNamePrefix, …)` then assigns; message: `'entryNamePrefix' argument must not be null!` |
| `protected String buildEntryName(Object entity, String action)` | `entryNamePrefix + entity.getClass().getName() + '@' + action` |

There are **no getters** on this class, and no `getFacade()` / `getEntryNamePrefix()` accessor (unlike the MyBatis and Spring Web plugins).

### Intercepted lifecycle events

All eight callbacks are `@Override` implementations of the `EntityCallback` contract. Signatures are reproduced exactly as declared in the source:

| Event pair | Method signatures as declared | What the plugin does |
|------------|-------------------------------|----------------------|
| LOAD | `onPreLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `begin("…@LOAD")` + `withinContext()` |
| LOAD | `onPostLoad(Object entity, Object id, String[] propertyNames, Type[] types)` | `doInContext(facade::end, true)` |
| INSERT | `onPreInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `begin("…@INSERT")` + `withinContext()` |
| INSERT | `onPostInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `doInContext(facade::end, true)` |
| UPDATE | `onPreUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `begin("…@UPDATE")` + `withinContext()` |
| UPDATE | `onPostUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `doInContext(facade::end, true)` |
| DELETE | `onPreDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `begin("…@DELETE")` + `withinContext()` |
| DELETE | `onPostDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `doInContext(facade::end, true)` |

Notes you need to write correct code against it:

- **Only these four actions exist** — there is no collection (`@OneToMany` dirtying) or flush/commit callback here.
- `Type` is `org.hibernate.type.Type` (imported directly from Hibernate 6.x).
- **`onPostLoad` has no `state` parameter** while every other method does; the pre/post signatures are not symmetric.
- The parameter is named `types` in the LOAD/DELETE methods and `propertyTypes` in the INSERT/UPDATE methods — the same value, different names.
- `@Nonnull` on the fields is documentation-level: the fields are declared `@Nonnull` but `facade` starts as `null`, so an un-wired callback throws `NullPointerException` on the first event.

### Entry names

```java
protected String buildEntryName(Object entity, String action) {
    Class<?> entityClass = entity.getClass();
    return entryNamePrefix + entityClass.getName() + '@' + action;
}
```

| Entity / action | Entry name (default prefix) |
|-----------------|-----------------------------|
| `com.acme.order.Order`, insert | `hibernate@com.acme.order.Order@INSERT` |
| `com.acme.order.Order`, update | `hibernate@com.acme.order.Order@UPDATE` |
| `com.acme.Customer`, load | `hibernate@com.acme.Customer@LOAD` |
| `com.acme.Customer`, delete | `hibernate@com.acme.Customer@DELETE` |

`entity.getClass().getName()` means **proxy-safe naming only for concrete entity classes**: if Hibernate hands the callback a ByteBuddy/Javassist proxy instance, the generated proxy class name enters the entry name and creates a distinct resource. Keep this in mind for lazy-loaded entities.

## Setup

This module registers **nothing by itself**: it contains no `META-INF/spring.factories`, no `Resilience4jPlugin` implementation and no auto-configuration class. You create the callback, give it a facade, and hand it to your Hibernate bootstrap (the `EntityCallback` extension point is defined by `microsphere-hibernate-core`, so consult that project for the registration entry point your stack exposes).

### Option A — programmatic

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;

Resilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
        RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults());

Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
callback.setFacade(facade);                       // mandatory
callback.setEntryNamePrefix("entity@");           // optional, defaults to "hibernate@"
```

### Option B — Spring bean

`microsphere-resilience4j-spring` registers the lazy facade as a bean named `lazyResilience4jFacade` (`LazyResilience4jFacade.BEAN_NAME`, declared *primary* by `EnableResilience4jRegistrar`). Because the callback is setter-configured, a plain `@Bean` method is enough:

```java
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.spring.LazyResilience4jFacade;
import io.microsphere.resilience4j.spring.common.annotation.EnableBulkhead;
import io.microsphere.resilience4j.spring.common.annotation.EnableCircuitBreaker;
import io.microsphere.resilience4j.spring.common.annotation.EnableRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCircuitBreaker
@EnableRateLimiter
@EnableBulkhead
public class Resilience4jHibernateConfig {

    @Bean
    public Resilience4jHibernateEntityCallback resilience4jHibernateEntityCallback(
            @Qualifier(LazyResilience4jFacade.BEAN_NAME) Resilience4jFacade facade) {
        Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
        callback.setFacade(facade);
        return callback;   // register it with your microsphere-hibernate bootstrap
    }
}
```

The `@Enable*` annotations decide which templates the lazy facade actually assembles — enable **exactly** the modules you want on the two-phase path (see the caveats below). The annotation package and binding prefixes are documented on the [spring module page](microsphere-resilience4j-spring.md).

## Example

### The test that ships with the module

`microsphere-resilience4j-hibernate-core/src/test/java/io/microsphere/resilience4j/hibernate/Resilience4jHibernateEntityCallbackTest.java` is, verbatim, the whole test class:

```java
/**
 * {@link Resilience4jHibernateEntityCallback} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jHibernateEntityCallback
 * @since 1.0.0
 */
class Resilience4jHibernateEntityCallbackTest extends AbstractHibernateH2Test {


}
```

It declares **no test methods**. Its value to you is the wiring it inherits: extending `io.microsphere.hibernate.test.AbstractHibernateH2Test` (from the `microsphere-hibernate-test` test dependency) gives a bootstrapped Hibernate session factory against in-memory H2, which is where an entity-driven assertion would be added. There is no assertion in the repository to copy, so the driver below is written from the production source, not from the test.

### A complete driver (pre → post, as Hibernate fires them)

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;
import org.hibernate.type.Type;
import org.hibernate.type.StandardBasicTypes;

public class HibernateCallbackDriver {

    static class Order {
        Long id;
        String customer;
    }

    public static void main(String[] args) {
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
        callback.setFacade(facade);                       // without this: NPE on first event

        Order order = new Order();
        order.id = 1L;
        order.customer = "Mercy";

        Object id = order.id;
        Object[] state = {order.id, order.customer};
        String[] propertyNames = {"id", "customer"};
        Type[] types = {StandardBasicTypes.LONG, StandardBasicTypes.STRING};

        // --- INSERT cycle ---------------------------------------------------
        callback.onPreInsert(order, id, state, propertyNames, types);
        // entry "hibernate@…$Order@INSERT" is open; the bound context is visible here:
        System.out.println(((Resilience4jContext<?>) Resilience4jContext.getContext())
                .getEntryName());
        callback.onPostInsert(order, id, state, propertyNames, types);          // end + unbind

        // --- LOAD cycle (note: no state argument) ---------------------------
        callback.onPreLoad(order, id, state, propertyNames, types);
        callback.onPostLoad(order, id, propertyNames, types);

        // --- UPDATE and DELETE cycles ---------------------------------------
        callback.onPreUpdate(order, id, state, propertyNames, types);
        callback.onPostUpdate(order, id, state, propertyNames, types);
        callback.onPreDelete(order, id, state, propertyNames, types);
        callback.onPostDelete(order, id, state, propertyNames, types);

        System.out.println("Resilience4jContext after the last post-event: "
                + Resilience4jContext.getContext());     // null -> every cycle was closed
    }
}
```

What this exercises, and what it proves from the source:

- each `onPreXxx` **binds** a `Resilience4jContext` to the current thread (`withinContext()`), so `Resilience4jContext.getContext()` is non-`null` between the pair;
- each `onPostXxx` **removes and ends** it (`doInContext(consumer, true)` calls `removeContext()` first), which is why the last print shows `null`;
- the four actions produce four distinct entry names, hence four independent `CircuitBreaker` / `Bulkhead` / `RateLimiter` instances in the registries behind the facade.

In a real application you never call these methods yourself — Hibernate fires them through the `EntityCallback` chain while your DAO performs `save` / `get` / `delete`.

## Behavioural notes and caveats

- **Only three Resilience4j modules take part.** `begin()`/`end()` consult `isBeginSupported()` / `isEndSupported()` on every template; `RetryTemplate`, `TimeLimiterTemplate` and `ThreadPoolBulkheadTemplate` return `false` and are skipped. `CircuitBreakerTemplate`, `RateLimiterTemplate` and `BulkheadTemplate` implement the pair. If you need retry or timeout around SQL, use the [MyBatis](microsphere-resilience4j-mybatis.md), [Druid](microsphere-resilience4j-alibaba-druid.md) or [OpenFeign](microsphere-resilience4j-openfeign.md) plugins, which use the one-time `call(...)` path.
- **Rejection happens in the pre-event.** `CircuitBreaker#acquirePermission()` throws `CallNotPermittedException`, `Bulkhead#acquirePermission()` throws `RequestNotPermitted`, and `RateLimiterTemplate.doBegin` waits via `RateLimiter.waitForPermission(...)` before throwing `RequestNotPermitted`. The exception propagates out of the `onPreXxx` callback into Hibernate's event dispatch, i.e. into your `save()`/`get()` call — expect it to surface as a persistence-layer exception, not as a Resilience4j type, once Hibernate wraps it.
- **Every completed event pair is recorded as a success.** The plugin never calls `context.setResult(...)` / `setFailure(...)` on the way out (compare `Resilience4jHandlerMethodInterceptor.afterExecute`, which does). `CircuitBreakerTemplate.doEnd` and `RateLimiterTemplate.doEnd` branch on `getFailure()`/`getResult()`, so with both `null` they call `onSuccess(...)` / `onSuccess()`-style paths. The breaker's error rate therefore only moves when a **pre-event rejection** is itself observed by the entry — plan your alerting around `CallNotPermittedEvent` and the bulkhead/rate-limiter rejection events instead.
- **An aborted operation leaks the context and the bulkhead permit.** `end()` runs only in the post-event. If the insert/update/delete throws before Hibernate fires the post-event, the `ThreadLocal` still holds the context and `BulkheadTemplate.doEnd` (`bulkhead.onComplete()`) is never called, so the concurrency permit stays acquired on that thread's logical call. Consider this when sizing `maxConcurrentCalls`.
- **One shared `ThreadLocal`, nested events overwrite it.** `Resilience4jContext.setContext(...)` stores a single value per thread. Cascading saves (entity A's insert firing entity B's insert inside the same thread before A's post-event) mean the inner pair overwrites the outer context, and the outer `onPostXxx` then ends the **inner** entry. Keep cascades off the protected path, or accept that nesting is not tracked.
- **Reads are decorated too.** `onPreLoad`/`onPostLoad` protect every entity materialisation, including rows pulled by association loading. A `RateLimiter` on `…@LOAD` throttles fetches, not writes — usually you want a much higher limit there.
- **`entryNamePrefix` is mutable after wiring** (no `final`, no guard) and the class is not thread-safe by design: instances are meant to be shared, but the field setters should only be called during initialisation.
- **No plugin SPI, no auto-registration.** Unlike the Druid, MyBatis, Spring Web plugins, this class does not implement `io.microsphere.resilience4j.spring.common.Resilience4jPlugin`, so `plugins = "…"` attributes on the `@Enable*` annotations do nothing for it. Registration is entirely your responsibility.
- **Version drift risk.** Because the BOM does not manage this artifact, a BOM bump to `0.2.(n+1)` with a pinned `0.2.n` Hibernate plugin silently mixes two library versions. Grep your POMs for the artifact when upgrading.

## Related pages

- [Developer guide — microsphere-resilience4j-hibernate-core](../developer-guide/microsphere-resilience4j-hibernate-core.md)
- [microsphere-resilience4j-dependencies (BOM scope gotcha)](microsphere-resilience4j-dependencies.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-spring-web (the other two-phase plugin)](microsphere-resilience4j-spring-web.md)
