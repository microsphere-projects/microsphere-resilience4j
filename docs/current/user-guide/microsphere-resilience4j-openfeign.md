# microsphere-resilience4j-openfeign — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← user guide index](README.md)

## What this plugin does

`microsphere-resilience4j-openfeign` decorates **native OpenFeign clients** (the `io.github.openfeign:feign-core` programming model — `Feign.builder()` + `@RequestLine` interfaces) with the Resilience4j decoration held by a `Resilience4jFacade`. It is delivered as a single `feign.Capability`, so no proxy bean, no annotation and no Spring container are involved.

| Class | Role |
|-------|------|
| `io.microsphere.resilience4j.feign.Resilience4jCapability` | The entry point: a `feign.Capability` that wraps one Feign extension point according to its `DecoratedPoint` |
| `io.microsphere.resilience4j.feign.Resilience4jClient` | `feign.Client` decorator — protects the **HTTP transport call only** (default decoration point) |
| `io.microsphere.resilience4j.feign.Resilience4jInvocationHandlerFactory` | `feign.InvocationHandlerFactory` decorator — builds a resilience-aware JDK proxy handler |
| `io.microsphere.resilience4j.feign.Resilience4jInvocationHandler` | `java.lang.reflect.InvocationHandler` decorator — protects the **whole Feign method invocation** (argument templating, request execution, decoding) |

### Where it hooks into Feign

`Feign.Builder.addCapability(Capability)` is Feign's own extension SPI. During `Feign.Builder#build()`, every registered capability is asked to `enrich(...)` the components it knows about. `Resilience4jCapability` implements exactly two of those hooks:

- `Client enrich(Client client)` → returns a `Resilience4jClient` wrapping the original when `DecoratedPoint.CLIENT` is selected (otherwise returns the argument untouched).
- `InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` → returns a `Resilience4jInvocationHandlerFactory` when `DecoratedPoint.INVOCATION_HANDLER_FACTORY` is selected (otherwise returns the argument untouched).

Because a `Capability` is applied to the object graph at build time, one registration protects every method of every interface created from that `Feign` instance. The decoration is **outside** everything Feign itself does downstream: on the `CLIENT` path it sits between `SynchronousMethodHandler` and the transport; on the `INVOCATION_HANDLER_FACTORY` path it sits between your service interface and Feign's generated proxy.

This is the only plugin of the five that is **framework-agnostic** — it depends on `microsphere-resilience4j-commons` (not on `microsphere-resilience4j-spring`) and ships no `META-INF/spring.factories`.

## Maven coordinates and dependency wiring

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-openfeign</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

Reading `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/pom.xml`, exactly one compile dependency is non-optional:

| Dependency | Scope in the plugin POM | Notes |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-commons` | **compile (non-optional)** | `Resilience4jFacade` / `ChainableResilience4jFacade` — you build the facade yourself |
| `io.github.openfeign:feign-core` | **optional** | must be provided by your application; version is not pinned in this repo |
| `io.github.resilience4j:resilience4j-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter`, `-annotations`, `-consumer`, `-framework-common` | **optional** | `-framework-common` is the only one with an explicit `${resilience4j.version}` in this POM |
| `io.github.resilience4j:resilience4j-feign` | **optional** | referenced only from the Javadoc of `Resilience4jInvocationHandler` (`@see io.github.resilience4j.feign.DecoratorInvocationHandler`); no class of it is used at runtime |
| `io.github.microsphere-projects:microsphere-java-core` | **optional** | supplies `io.microsphere.reflect.MethodUtils`, `io.microsphere.util.StringUtils`, `io.microsphere.constants.SymbolConstants` — declare it yourself if you rely on it |
| `io.github.microsphere-projects:microsphere-annotation-processor` | **optional** | build-time only |
| `junit-jupiter`, `microsphere-java-test`, `feign-gson`, `logback-classic` | **test** | used by the three test classes |

There is **no** dependency on `microsphere-resilience4j-spring`, Spring Framework, or Spring Cloud in this module.

## Version and compatibility

| Item | Requirement | Notes |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x` (`release`) | artifact `microsphere-resilience4j-openfeign` |
| Resilience4j | 2.0.0 – 2.4.0 | selected by the Maven profiles of `microsphere-resilience4j-parent`; default profile `resilience4j-2.4` → `2.4.0` |
| OpenFeign | application-supplied | needs `Capability.enrich(Client)` / `enrich(InvocationHandlerFactory)`, `Request#requestTemplate()`, `RequestTemplate#feignTarget()`, `RequestTemplate#queryLine()`, `MethodMetadata#template()` — the Feign 10.x+ API line used by Spring Cloud 2022.0.x – 2025.0.x |
| Spring Cloud OpenFeign | optional | supported indirectly: you can contribute a `feign.Capability` bean; nothing in this module auto-configures it |
| JDK | 17+ | root POM `java.version = 17` |

## Setup

### 1. Build a facade

The plugin needs a `Resilience4jFacade`. Outside Spring, `ChainableResilience4jFacade` from commons is the usual choice — this is exactly what the test base class does:

```java
Resilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(),
        RetryRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults());
```

Registries are var-arg `Registry...`; the facade builds one `Resilience4jTemplate` per registry and orders them by `Resilience4jModule` priority.

### 2. Register the capability with your `Feign.Builder`

```java
Resilience4jCapability capability = new Resilience4jCapability(facade); // CLIENT point, "feign@" prefix

Feign feign = Feign.builder()
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .addCapability(capability)   // add it last if other capabilities must stay inside the decoration
        .build();
```

### 3. (Optional) choose the decoration point or the entry-name prefix

```java
// protect the whole method invocation instead of the transport call
new Resilience4jCapability(facade, Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);

// custom prefix (must be unique if several Feign client groups share a registry)
new Resilience4jCapability(facade, "billing-feign@");

// both
new Resilience4jCapability(facade, "billing-feign@", Resilience4jCapability.DecoratedPoint.CLIENT);
```

Inside Spring, you may reuse the facade registered by `microsphere-resilience4j-spring` (`LazyResilience4jFacade.BEAN_NAME = "lazyResilience4jFacade"`) and expose the capability yourself:

```java
@Configuration
public class FeignResilience4jConfig {

    @Bean
    public Resilience4jCapability resilience4jCapability(Resilience4jFacade lazyResilience4jFacade) {
        return new Resilience4jCapability(lazyResilience4jFacade);
    }
}
```

> This module registers **no** `Resilience4jPlugin` and no `@Enable*` support: unlike the Druid, MyBatis, Hibernate and Spring Web plugins, nothing happens automatically. Whether a `Capability` bean is picked up by Spring Cloud OpenFeign depends on that project's per-client configuration mechanism — check it against the Spring Cloud version you use, or build the `Feign` instance yourself as above.

## Public API

### `Resilience4jCapability`

| Member | Behaviour (from source) |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "feign@"` | default prefix |
| `public static final DecoratedPoint DEFAULT_DECORATED_POINT = CLIENT` | default decoration point |
| `Resilience4jCapability(Resilience4jFacade)` | prefix = `feign@`, point = `CLIENT` |
| `Resilience4jCapability(Resilience4jFacade, DecoratedPoint)` | prefix = `feign@`, custom point |
| `Resilience4jCapability(Resilience4jFacade, String entryNamePrefix)` | custom prefix, point = `CLIENT` |
| `Resilience4jCapability(Resilience4jFacade, String, DecoratedPoint)` | the only constructor that assigns fields; no null checks |
| `Client enrich(Client)` | decorates only when `isDecoratedClient()` |
| `InvocationHandlerFactory enrich(InvocationHandlerFactory)` | decorates only when `isDecoratedInvocationHandlerFactory()` |
| `getDecoratedPoint()`, `isDecoratedClient()`, `isDecoratedInvocationHandlerFactory()` | accessors / predicates on the single selected `DecoratedPoint` |
| `enum DecoratedPoint { CLIENT, INVOCATION_HANDLER_FACTORY }` | exactly one point can be decorated per capability |

### `Resilience4jClient` (transport boundary)

```java
public Resilience4jClient(Client delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public Response execute(Request request, Request.Options options) throws IOException {
    String entryName = buildEntryName(request);
    return this.facade.call(entryName, () -> this.delegate.execute(request, options), IOException.class);
}
```

Entry name, assembled by the private `buildEntryName(Request)`:

```
<entryNamePrefix> + httpMethod.name() + " " + requestTemplate.feignTarget().url()
                  + requestTemplate.methodMetadata().template().path()
                  + requestTemplate.queryLine()   // appended only when StringUtils.isNotBlank(queryLine)
```

| Feign declaration | Resulting entry name |
|-------------------|----------------------|
| `@RequestLine("POST /user/{name}")` against `http://localhost:8080` | `feign@POST http://localhost:8080/user/{name}` |
| `@RequestLine("GET /user")` with query `name=test-user` | `feign@GET http://localhost:8080/user?name=test-user` |

The path is read from the **method metadata's** template (so `{name}`-style variables keep their declared form) while the query line is read from the **per-request** template (so query values are usually expanded). Because the two halves come from different templates, print one entry name in your environment before writing per-entry configuration.

### `Resilience4jInvocationHandlerFactory` / `Resilience4jInvocationHandler` (method boundary)

```java
// factory
public Resilience4jInvocationHandlerFactory(InvocationHandlerFactory delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
    InvocationHandler delegate = this.delegate.create(target, dispatch);
    return new Resilience4jInvocationHandler(delegate, facade, entryNamePrefix);
}

// handler
public Resilience4jInvocationHandler(InvocationHandler delegate, Resilience4jFacade facade, String entryNamePrefix)
// requireNonNull on all three arguments; messages:
// "The 'delegate' argument must not be null!", "The 'facade' argument must not be null!",
// "The 'entryNamePrefix' argument must not be null!"

@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String entryName = buildEntryName(method);   // entryNamePrefix + MethodUtils.getSignature(method)
    return this.facade.call(entryName, () -> delegate.invoke(proxy, method, args));
}
```

`getDelegate()` and `getFacade()` are `@Nonnull` accessors. The entry name is one stable resource **per interface method** (declaring type + method name + parameter types as rendered by `io.microsphere.reflect.MethodUtils#getSignature(Method)`), e.g. `feign@<signature of UserService.getUser(String)>`.

### Per-entry Resilience4j configuration

`AdvancedResilience4jOperations#getEntry(name)` looks the name up in the registry and, on a miss, creates the entry with `getConfiguration(name)` — that is, the registry's **named configuration whose key equals the entry name**, falling back to `registry.getDefaultConfig()`. So with a plain Resilience4j registry:

```java
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
circuitBreakerRegistry.addConfiguration(
        "feign@POST http://localhost:8080/user/{name}",                  // the entry name
        CircuitBreakerConfig.custom().failureRateThreshold(20).build()); // your config
```

The `microsphere.resilience4j.circuit-breaker.` / `microsphere.resilience4j.bulkhead.` … property prefixes (Spring side) are documented on the [spring module page](microsphere-resilience4j-spring.md).

## Complete runnable examples

### 1. Default setup (`CLIENT` point) — adapted from the unit tests

The upstream test replaces the real HTTP transport with a local in-JVM `Client` so the whole round trip runs without a server; a production application uses `Feign.builder()… .target(UserService.class, "http://localhost:8080")` (or `hardCodedTarget`) instead.

```java
import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.feign.Resilience4jCapability;

public class OpenFeignResilience4jDemo {

    public interface User {
        String getName();
    }

    public interface UserService {
        @RequestLine("POST /user/{name}")
        User createUser(@Param("name") String name);

        @RequestLine("GET /user")
        User getUser(@Param("name") String name);
    }

    public static void main(String[] args) {
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        GsonEncoder encoder = new GsonEncoder();

        UserService userService = Feign.builder()
                .client(new YourClientOrTheDefaultOne(encoder))     // feign.Client
                .decoder(new GsonDecoder())
                .encoder(encoder)
                .addCapability(new Resilience4jCapability(facade))   // DecoratedPoint.CLIENT
                .build()
                .newInstance(new Target.HardCodedTarget<>(UserService.class, "http://localhost:8080"));

        User created = userService.createUser("test-user");          // feign@POST http://localhost:8080/user/{name}
        User found = userService.getUser("test-user");               // feign@GET http://localhost:8080/user?name=test-user
        System.out.println(created.getName() + " / " + found.getName());
    }
}
```

`AbstractResilience4jFeignTest#testUserService` asserts exactly this round trip (`assertEquals(userName, user.getName())`, `assertEquals(user, foundUser)`), and `Resilience4jCapabilityTest#testConstants` asserts `DEFAULT_ENTRY_NAME_PREFIX == "feign@"` and `DEFAULT_DECORATED_POINT == CLIENT`.

### 2. Method-level decoration (`INVOCATION_HANDLER_FACTORY` point)

```java
Resilience4jCapability capability = new Resilience4jCapability(
        facade, Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);

UserService userService = Feign.builder()
        .client(client)
        .encoder(encoder).decoder(decoder)
        .addCapability(capability)
        .build()
        .newInstance(new Target.HardCodedTarget<>(UserService.class, "http://localhost:8080"));

// the proxy handler is now the decoration carrier
InvocationHandler handler = Proxy.getInvocationHandler(userService);
// Resilience4jInvocationHandler -> (whatever Feign built) -> feign.ReflectiveFeign$FeignInvocationHandler
```

`AbstractResilience4jFeignTest#testProxy` walks precisely that chain: it asserts `handler instanceof Resilience4jInvocationHandler`, `getDelegate() instanceof` the test's own wrapper, the innermost delegate class name equals `feign.ReflectiveFeign$FeignInvocationHandler`, and `assertSame(facade, resilience4jInvocationHandler.getFacade())`. With the `CLIENT` point the same test asserts the handler is **not** decorated (only the `Client` is).

## Behavioural notes and caveats

- **Exception shape depends on the decoration point.**
  - `CLIENT`: `facade.call(name, supplier, IOException.class)` declares `throws IOException`; any failure — including Resilience4j's `CallNotPermittedException`, `MaximumWaitTimeExceededException` or `TimeoutException` — is surfaced as an **`IOException`**, which is what a Feign caller normally expects (and what `Retryer`/Spring Cloud's error handling already deals with). The original cause is retained by the wrapping step.
  - `INVOCATION_HANDLER_FACTORY`: `facade.call(name, supplier)` rethrows as **`Throwable`**, so callers see the **original** exception type, including `io.github.resilience4j.CallNotPermittedException` when the circuit is open. Your interface signature cannot declare it, so it travels as an unchecked exception or through `UndeclaredThrowableException` at the proxy boundary — catch it explicitly or use the `CLIENT` point instead.
- **Open circuit during a Feign call means no HTTP traffic.** The decoration is entered before the transport is invoked, so a blocked call never touches the network and never consumes a connection from the client's pool.
- **All modules apply on both points.** Both paths use the one-time `call(...)` operation, so `RetryTemplate`, `TimeLimiterTemplate` and `ThreadPoolBulkheadTemplate` participate — unlike the two-phase `begin()/end()` paths used by the [Spring Web](microsphere-resilience4j-spring-web.md) and [Hibernate Core](microsphere-resilience4j-hibernate-core.md) plugins, which skip those three modules.
- **`TimeLimiter` and `ThreadPoolBulkhead` move the call to another thread.** On the `CLIENT` point the Feign request is already fully built, so only the transport runs elsewhere. On the `INVOCATION_HANDLER_FACTORY` point the whole proxy call runs elsewhere, which breaks anything bound to the calling thread (request context, `ThreadLocal` propagation, security holders).
- **Resource cardinality.** With the `CLIENT` point the query line is part of the entry name, so high-cardinality query parameters (ids, timestamps) multiply Resilience4j instances — one `CircuitBreaker`/`RateLimiter`/`Bulkhead` per distinct name, each holding its own ring buffer. Use `INVOCATION_HANDLER_FACTORY` for a fixed, per-method resource set.
- **`Retryer` and Resilience4j `Retry` are different things.** Feign's own retry mechanism retries `RetryableException` at the `ErrorDecoder` level; with the `CLIENT` point, failures wrapped as `IOException` may no longer look `RetryableException`-shaped to a custom `ErrorDecoder`. If you combine both, count retries once.
- **Decoration order is build order.** Each capability's `enrich(...)` wraps what the previous one returned, so the last-added capability ends up outermost. Add `Resilience4jCapability` after the capabilities whose work you want counted inside the resilience boundary (the tests add a `DelegatingCapability` first for exactly this reason).
- **Two points at once requires two capabilities.** `DecoratedPoint` is single-valued; adding two `Resilience4jCapability` instances with different points double-counts every call against rate limiters and bulkheads. Prefer one point.
- **Facade readiness.** If you inject the Spring `lazyResilience4jFacade`, note that its delegate chain is assembled on `ContextRefreshedEvent`; issuing Feign calls during context startup can therefore behave differently from the steady state.

## Related pages

- [Developer guide — microsphere-resilience4j-openfeign](../developer-guide/microsphere-resilience4j-openfeign.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-spring-web](microsphere-resilience4j-spring-web.md)
