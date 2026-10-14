# microsphere-resilience4j-openfeign — User Guide (1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud **Hoxton – 2021.0.x** (parent `microsphere-spring-cloud-parent` `0.1.26`) · JDK **8** baseline · [← guide index](README.md)

Module path in the repository: `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign`.
Java package: `io.microsphere.resilience4j.feign`.

## What this plugin does

It wraps an OpenFeign client so that **every Feign call goes through the microsphere-resilience4j facade** (`Resilience4jFacade` from `microsphere-resilience4j-commons`) instead of being decorated one module at a time by hand. The facade is backed by the Resilience4j registries you provide, so a single entry name is decorated by the whole chain — bulkhead, rate limiter, retry, time limiter and circuit breaker — in a fixed, prioritized order.

The plugin is **framework-only**: it depends on Feign and on the commons module, and on nothing in the Spring family. It has no `@Enable*` annotation, no `META-INF/spring.factories` and no `Resilience4jPlugin` SPI implementation; you register it with Feign's own extension mechanism.

## Where it hooks in

Feign exposes `feign.Capability`, an interface of `enrich(...)` hooks that `Feign.Builder` folds over the components it assembles. This plugin implements exactly two of them, and only **one of the two is active per capability instance**, chosen by the `DecoratedPoint` constructor argument:

| `DecoratedPoint` | `feign.Capability` hook used | Wrapper class produced | What is intercepted |
|------------------|------------------------------|------------------------|---------------------|
| `CLIENT` (default) | `Client enrich(Client client)` | `Resilience4jClient` | the HTTP/`Client` execution step, i.e. `Client#execute(Request, Request.Options)` |
| `INVOCATION_HANDLER_FACTORY` | `InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` | `Resilience4jInvocationHandlerFactory` → `Resilience4jInvocationHandler` | the JDK-proxy method invocation, i.e. `InvocationHandler#invoke(Object, Method, Object[])` |

The resulting wrapping chain (the class names below are the ones the module's own tests assert or that the plugin source imports; the intermediate Feign handler is Feign's own, not this plugin's):

```text
CLIENT mode
  proxy -> feign.ReflectiveFeign$FeignInvocationHandler -> ... -> Resilience4jClient -> your feign.Client

INVOCATION_HANDLER_FACTORY mode
  proxy -> Resilience4jInvocationHandler -> <handler returned by the delegate InvocationHandlerFactory>
```

The two points put the resilience boundary at different heights: `CLIENT` decorates the **transport call** (`Client#execute`), so what is measured and protected is one execution of Feign's request/response round-trip; `INVOCATION_HANDLER_FACTORY` decorates the **proxy method call**, so everything Feign itself does below the handler (template building, encoding/decoding, its own retry and error-decoding policy) sits inside the decoration, and one logical method call equals one resilience pass.

## Compatibility

| Item | Value | Verified from |
|------|-------|---------------|
| microsphere-resilience4j-openfeign | `0.1.0` – `0.1.4` released; `${revision}` = `0.1.5-SNAPSHOT` | module `pom.xml`, root `pom.xml` |
| Resilience4j | `1.3.0`, `1.4.0`, `1.5.0`, `1.6.0`, `1.7.0` (default `1.7.0`) | `microsphere-resilience4j-parent/pom.xml` profiles `resilience4j-1.3` … `resilience4j-1.7` |
| Spring Cloud (only if you embed it in a Spring app) | Hoxton – 2021.0.x | parent `microsphere-spring-cloud-parent` `0.1.26` profiles |
| JDK baseline | **8** | the 1.x reactor POM declares no `java.version`; the inherited grandparent `microsphere-build` `0.3.14` sets `<java.version>8</java.version>` with `maven.compiler.source/target = ${java.version}`, and the CI matrix (`.github/workflows/maven-build.yml`) starts at `java: '8'` |
| Feign (`feign-core`) | managed by the Spring Cloud BOM of the active profile; the module itself pins no version | module `pom.xml` (`<optional>true</optional>`, no `<version>`) |
| Since | `1.0.0` (Javadoc `@since`), renamed from `feign` to `openfeign` in `0.1.1` | source headers, `release-notes.md` |

## Installation

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
        <artifactId>microsphere-resilience4j-openfeign</artifactId>
    </dependency>
</dependencies>
```

`microsphere-resilience4j-openfeign` is managed by that BOM (`…/microsphere-resilience4j-dependencies/pom.xml`).

### Scopes declared in the plugin's own POM — and what you must add

Almost everything the plugin compiles against is `<optional>true</optional>`, which means **it is not transitive**: your application has to bring those jars itself. The only non-optional compile dependency is the commons module.

| Dependency (module `pom.xml`) | Scope / optionality | You must provide it? |
|-------------------------------|---------------------|----------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-commons` | compile, **non-optional** (`${revision}`) | comes transitively |
| `io.github.openfeign:feign-core` | `optional` | yes (normally already there from OpenFeign) |
| `io.github.microsphere-projects:microsphere-java-core` | `optional` | yes — used for `MethodUtils.getSignature`, `StringUtils.isNotBlank`, `SymbolConstants.SPACE_CHAR` |
| `io.github.resilience4j:resilience4j-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter`, `-annotations`, `-consumer`, `-framework-common`, `-feign` | `optional` | yes for the modules whose registries you hand to the facade |
| `io.github.microsphere-projects:microsphere-annotation-processor` | `optional` (build-time) | no |
| `org.junit.jupiter:junit-jupiter`, `microsphere-java-test`, `ch.qos.logback:logback-classic`, `io.github.openfeign:feign-gson` | `test` | no |

There is **no `provided` scope** in this POM; the only modifiers used are `optional` and `test`.

Note also that `resilience4j-feign` is declared but **not used** by any class in the plugin — the only reference to it is a `@see io.github.resilience4j.feign.DecoratorInvocationHandler` Javadoc link in `Resilience4jInvocationHandler`.

## API reference

### `Resilience4jCapability` (`implements feign.Capability`)

| Member | Signature |
|--------|-----------|
| constant | `public static final String DEFAULT_ENTRY_NAME_PREFIX = "feign@"` |
| constant | `public static final DecoratedPoint DEFAULT_DECORATED_POINT = CLIENT` |
| constructor | `Resilience4jCapability(Resilience4jFacade facade)` |
| constructor | `Resilience4jCapability(Resilience4jFacade facade, DecoratedPoint decoratedPoint)` |
| constructor | `Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix)` |
| constructor | `Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix, DecoratedPoint decoratedPoint)` |
| hook | `@Override public Client enrich(Client client)` |
| hook | `@Override public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` |
| accessor | `@Nonnull public DecoratedPoint getDecoratedPoint()` |
| predicate | `public boolean isDecoratedClient()` |
| predicate | `public boolean isDecoratedInvocationHandlerFactory()` |
| nested enum | `public static enum DecoratedPoint { CLIENT, INVOCATION_HANDLER_FACTORY }` |

Both `enrich` methods are conditional: they return the new wrapper only when the configured `DecoratedPoint` selects them, otherwise the argument is returned untouched — so adding the same capability twice with different points is possible, but a single instance decorates exactly one place.

### `Resilience4jClient` (`implements feign.Client`)

```java
public Resilience4jClient(Client delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public Response execute(Request request, Request.Options options) throws IOException {
    String entryName = buildEntryName(request);
    return this.facade.call(entryName, () -> this.delegate.execute(request, options), IOException.class);
}
```

`execute` keeps the `throws IOException` contract: the facade call uses the `<T, TR extends Throwable> T call(String, ThrowableSupplier<T>, Class<TR>)` overload, which delegates to the plain `call(String, ThrowableSupplier<T>)` and then hands any `Throwable` to `io.microsphere.util.ExceptionUtils.wrap(t, throwableClass)` so the transport's `IOException` is not hidden behind an unchecked wrapper. Resilience4j's own rejection signals are unchecked (`CallNotPermittedException`, `RequestNotPermitted`, `TimeoutException`) and therefore reach your caller as runtime exceptions regardless — install a fallback (`Targeter`/`ErrorDecoder`/circuit-breaker fallback) if you want to convert them into a response. The exact `wrap(...)` semantics live in `microsphere-java-core`, outside this snapshot.

### `Resilience4jInvocationHandlerFactory` (`implements feign.InvocationHandlerFactory`)

```java
public Resilience4jInvocationHandlerFactory(InvocationHandlerFactory delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
    InvocationHandler delegate = this.delegate.create(target, dispatch);
    return new Resilience4jInvocationHandler(delegate, facade, entryNamePrefix);
}
```

### `Resilience4jInvocationHandler` (`implements java.lang.reflect.InvocationHandler`)

```java
public Resilience4jInvocationHandler(InvocationHandler delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String entryName = buildEntryName(method);
    return this.facade.call(entryName, () -> delegate.invoke(proxy, method, args));
}

@Nonnull public InvocationHandler getDelegate()
@Nonnull public Resilience4jFacade getFacade()
```

This is the only class of the four with eager argument validation: `requireNonNull` on `delegate`, `facade` and `entryNamePrefix`. Its `invoke` declares `throws Throwable` and uses the two-argument `call(...)` overload, so nothing is wrapped.

### Entry names → Resilience4j instances

The entry name is the *only* key that links a call to Resilience4j objects, and both builders are plain string concatenation after `entryNamePrefix` — **no separator is inserted by the library**.

| Decorated point | Name construction (from source) | Example with the default prefix `feign@` |
|-----------------|---------------------------------|------------------------------------------|
| `CLIENT` | `prefix + request.httpMethod().name() + ' ' + requestTemplate.feignTarget().url() + methodMetadata.template().path()` + `requestTemplate.queryLine()` when it is not blank | `feign@POST http://localhost:8080/user/{name}` |
| `INVOCATION_HANDLER_FACTORY` | `prefix + io.microsphere.reflect.MethodUtils.getSignature(method)` | `feign@<signature of UserService#getUser(String)>` |

Two consequences you should design for:

1. The `CLIENT` path mixes a **contract-parsed, unresolved** path template (`methodMetadata.template().path()`, so `{name}` stays literal) with a **per-request** `queryLine()`. Query parameters therefore become part of the entry name: `feign@GET http://localhost:8080/user?name=test-user` and `...?name=other-user` are two different circuit breakers. With free-form query strings (page numbers, timestamps, sort keys) this creates unbounded registry entries.
2. The `INVOCATION_HANDLER_FACTORY` path produces one stable name per interface method, independent of arguments — usually what you want for per-endpoint circuit breakers.

Each name is resolved by commons as `registry.find(name)` → else `createEntry(name)`, i.e. `registry.<module>(name, getConfiguration(name))`. Because `getConfiguration(name)` looks up a **named configuration whose key equals the entry name**, you can pre-register a `CircuitBreakerConfig` (or rate-limiter/retry/… config) under exactly the generated entry name to control one endpoint without touching the others. Everything else falls back to the registry's default configuration.

## Complete runnable example

The example mirrors the module's own test suite: a `UserService` Feign interface whose "server" is a local `SimpleUserService`, executed by a test `Client` so no HTTP server is needed. Add `feign-core`, `feign-gson` and the Resilience4j modules you use.

```java
import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
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

public class FeignResilienceDemo {

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
        // 1. the registries decide which modules decorate the call
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        // 2. pick the weaving point: CLIENT (default) or INVOCATION_HANDLER_FACTORY
        Resilience4jCapability capability =
                new Resilience4jCapability(facade, Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);

        Encoder encoder = new GsonEncoder();
        Decoder decoder = new GsonDecoder();

        // 3. plain Feign — in production replace the client with feign.Client.Default or your own
        Feign feign = Feign.builder()
                .client(new feign.Client.Default())
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)   // repeat .addCapability(...) for other capabilities
                .build();

        UserService userService = feign.newInstance(new Target.HardCodedTarget<>(UserService.class, "http://localhost:8080"));

        // every call is now decorated under entry names starting with "feign@"
        System.out.println(userService.createUser("test-user").getName());
    }
}
```

Running the plugin's own tests instead (they need no network):

```bash
./mvnw -pl microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign -am test
```

`AbstractResilience4jFeignTest` builds the client exactly as above, except that it installs `new DelegatingClient<>(encoder, new SimpleUserService())` — a `feign.Client` whose `execute(Request, Request.Options)` reads the `Method` from `request.requestTemplate().methodMetadata().method()`, invokes it reflectively on the in-memory `SimpleUserService`, and encodes the return value into a `Response` with status `200`. The three concrete tests only differ in how they build the capability:

| Test class | capability built in `createResilience4jCapability(Resilience4jFacade)` | Effect |
|------------|-----------------------------------------------------------------------|--------|
| `Resilience4jCapabilityTest` | `new Resilience4jCapability(facade)` | defaults: prefix `feign@`, `CLIENT`; asserts `DEFAULT_ENTRY_NAME_PREFIX`/`DEFAULT_DECORATED_POINT` |
| `Resilience4jClientTest` | `new Resilience4jCapability(facade, this.getClass().getSimpleName())` | `CLIENT` with a custom prefix (`Resilience4jClientTest…`) |
| `Resilience4jInvocationHandlerFactoryTest` | `new Resilience4jCapability(facade, INVOCATION_HANDLER_FACTORY)` | handler-factory mode, default prefix |

`testProxy()` is the assertion to copy if you want to prove the weaving from your own tests: in `INVOCATION_HANDLER_FACTORY` mode the proxy handler is a `Resilience4jInvocationHandler`, its `getDelegate()` is the previously-added capability's handler and `getFacade()` returns your facade instance; in `CLIENT` mode the proxy handler is untouched (`Resilience4jCapability#isDecoratedClient()` is `true`, so the assertion block is skipped). `testUserService()` then round-trips `createUser("test-user")` / `getUser("test-user")` against `SimpleUserService`.

## Registering in a Spring Cloud OpenFeign application

This module ships no Spring integration. In a Spring Cloud OpenFeign application the natural step is to expose the capability as a bean in the Feign client context:

```java
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.feign.Resilience4jCapability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignResilienceConfiguration {

    @Bean
    public Resilience4jCapability resilience4jCapability(Resilience4jFacade facade) {
        return new Resilience4jCapability(facade, Resilience4jCapability.DEFAULT_ENTRY_NAME_PREFIX,
                Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);
    }
}
```

> Unverified in this snapshot: whether your Spring Cloud OpenFeign version collects `Capability` beans from the client context, and whether a `Resilience4jFacade` bean exists — both are behaviour of Spring Cloud OpenFeign / of the `microsphere-resilience4j-spring*` modules, not of this plugin. The only wiring proven by this repository's sources is `Feign.Builder#addCapability(Capability)`. If you need Spring-managed registries, see [microsphere-resilience4j-spring.md](microsphere-resilience4j-spring.md).

## Behavioral notes

| Topic | Behaviour (from source) |
|-------|--------------------------|
| State / thread-safety | All four classes keep only `final` fields (`delegate`, `facade`, `entryNamePrefix`, `decoratedPoint`) and no mutable state; one capability instance can be shared by many `Feign` instances and many threads. |
| Name computation cost | Neither decorator caches entry names. `Resilience4jClient` rebuilds the name with a `StringBuilder` on **every** call; `Resilience4jInvocationHandler` calls `MethodUtils.getSignature(method)` per call. |
| Decoration is mutually exclusive | One `Resilience4jCapability` decorates either the `Client` or the `InvocationHandlerFactory`, never both. Adding two instances with different points would double-decorate the same logical call (two facades `call(...)` per invocation) — avoid it. |
| Facade is the single point of variability | The plugin resolves nothing from Resilience4j itself: registries, module set and ordering come from the `Resilience4jFacade` you pass. `ChainableResilience4jFacade(Registry...)` creates one template per registry and sorts them by priority (retry → circuit breaker → rate limiter → time limiter → bulkhead), and both decorators use the **one-time `call(...)` path**, so all modules you registered — including retry, time limiter and thread-pool bulkhead — participate (unlike the two-phase path used by the Spring Web plugin). |
| Wrapping order with several capabilities | `Feign.Builder` folds the capability list in insertion order, so the **last added capability is the outermost wrapper**. In the tests, `DelegatingCapability` is added before `Resilience4jCapability` and ends up inside it. |
| `Target` / `MethodHandler` inputs | `Resilience4jInvocationHandlerFactory#create` passes `target` and `dispatch` straight to the delegate factory; it does not read them, so the entry name never contains the target URL in this mode (only in `CLIENT` mode does `feignTarget().url()` appear). |
| Null handling | Only `Resilience4jInvocationHandler` validates its constructor arguments; `Resilience4jClient`, `Resilience4jInvocationHandlerFactory` and `Resilience4jCapability` accept `null` silently and fail later (NPE) — construct them properly. |
| Logging | No logging in the plugin itself; the commons facade logs `trace` messages per `call(...)`. |
