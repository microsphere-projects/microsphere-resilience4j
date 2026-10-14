# microsphere-resilience4j-spring-web — User Guide (1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud **Hoxton – 2021.0.x** (parent `microsphere-spring-cloud-parent` `0.1.26`) · JDK **8** baseline · [← guide index](README.md)

Module path in the repository: `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web`.
Java package: `io.microsphere.resilience4j.spring.web`.

## What this plugin does

It makes every Spring MVC **handler method** a resilience4j-protected entry, without annotations on the controllers and without writing decoration code. When the plugin is activated, a `Resilience4jHandlerMethodInterceptor` bean is registered; it opens a Resilience4j context before the handler method runs and closes it after, so the controller execution is measured and guarded by the modules configured in your `Resilience4jFacade` (circuit breaker, bulkhead, rate limiter — see *Which modules apply*).

## Where it hooks in

| Layer | Hook used by this plugin | Concrete type |
|-------|--------------------------|---------------|
| plugin discovery | `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` (SPI, published through `META-INF/spring.factories`) | `SpringWebResilience4jPlugin` |
| bean registration | `BeanDefinitionRegistry` inside `Resilience4jPlugin#plugin(...)` | registers bean name `resilience4jHandlerMethodInterceptor` |
| request interception | `io.microsphere.spring.web.method.support.HandlerMethodInterceptor` — `beforeExecute(HandlerMethod, Object[], NativeWebRequest)` / `afterExecute(HandlerMethod, Object[], Object, Throwable, NativeWebRequest)` (the `microsphere-spring-web` handler-method extension chain, activated by `@EnableWebMvcExtension`) | `Resilience4jHandlerMethodInterceptor` |
| endpoint metadata | `ApplicationListener<io.microsphere.spring.web.event.WebEndpointMappingsReadyEvent>` | same class — pre-computes entry names and warms the commons entry caches |
| ordering | `org.springframework.core.Ordered` | same class (`getOrder()` / `setOrder(int)`, default `0`) |

The interception boundary is **the handler method**, not the servlet request: the pair of callbacks fires once the controller method (a Spring `HandlerMethod`) is selected, with its arguments resolved and the return value / error available. It is therefore narrower than a `Filter` and richer than `HandlerInterceptor#preHandle` — and it never sees requests that do not map to a handler method.

## Compatibility

| Item | Value | Verified from |
|------|-------|---------------|
| microsphere-resilience4j-spring-web | `0.1.0` – `0.1.4`; `${revision}` = `0.1.5-SNAPSHOT` | module `pom.xml`, root `pom.xml` |
| Resilience4j | `1.3.0`, `1.4.0`, `1.5.0`, `1.6.0`, `1.7.0` (default `1.7.0`, profile `resilience4j-1.7` is `activeByDefault`) | `microsphere-resilience4j-parent/pom.xml` |
| Spring profiles → stacks | `spring-cloud-hoxton` → Spring Cloud `Hoxton.SR12` / Spring Boot `2.3.12.RELEASE`; `spring-cloud-2020` → `2020.0.5` / `2.4.13`; `spring-cloud-2021` (default) → `2021.0.9` / `2.6.15` | profiles of the inherited `microsphere-spring-cloud-parent` `0.1.26` POM |
| Servlet API | `javax.servlet` (JSR-340 generation, `javax.servlet-api` in the test scope of this module) | module `pom.xml`, `WebEndpointMappingsReadyEvent`/`NativeWebRequest` usage |
| JDK baseline | **8** — the 1.x reactor POM declares no `java.version`; the inherited grandparent `microsphere-build` `0.3.14` sets `<java.version>8</java.version>` (`maven.compiler.source/target = ${java.version}`), and `.github/workflows/maven-build.yml` tests from `java: '8'` | those POMs and the workflow file |
| `@since` | `1.0.0` | source headers |

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
        <artifactId>microsphere-resilience4j-spring-web</artifactId>
    </dependency>
</dependencies>
```

### Scopes declared in the plugin's own POM

| Dependency | Scope / optionality | Who normally provides it |
|------------|---------------------|--------------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile, **non-optional** (`${revision}`) | transitive — this is where the `Resilience4jPlugin` SPI, `@Enable*` annotations and the facade beans come from |
| `io.github.microsphere-projects:microsphere-spring-context` | `optional` | usually present already in a microsphere-spring app |
| `io.github.microsphere-projects:microsphere-spring-web` | `optional` | **you must add it** — it provides `HandlerMethodInterceptor`, `WebEndpointMapping`, `WebEndpointMappingsReadyEvent`, `WebScope` |
| `io.github.microsphere-projects:microsphere-spring-webmvc` | declared in the *testing* block with **`test` scope** | you must add it yourself at runtime — it carries `@EnableWebMvcExtension` |
| `org.springframework:spring-web` | `optional` | your Spring Boot/Cloud BOM |
| `org.springframework:spring-webmvc` | `optional` (**not** test scope, despite sitting under the “Testing” comment) | your Spring Boot/Cloud BOM |
| `io.github.resilience4j:resilience4j-spring` | `optional` | needed — the `@Enable*` annotations bind Resilience4j 1.x configuration properties |
| `io.github.microsphere-projects:microsphere-annotation-processor` | `optional` (build-time) | no |
| `io.microsphere.*` utilities used by the interceptor source (`logging.Logger`/`LoggerFactory`, `util.Assert`, `lang.Wrapper`, `collection.MapUtils`, `annotation.Nonnull`) | **not declared in this POM at all** | the Microsphere core/logging artifacts your app already carries; `microsphere-java-core` is `optional` in `microsphere-resilience4j-commons`, so it is *not* transitive. A missing provider shows up as `NoClassDefFoundError` for an `io.microsphere.*` class — add it explicitly. Which artifact exactly is not pinned down by these POMs (unverified). |
| `junit-jupiter`, `spring-test`, `spring-aop`, `microsphere-spring-test`, `microsphere-spring-webmvc`, `javax.servlet-api`, `jackson-databind`, `aspectjweaver`, `jsonassert`, `hamcrest`, `logback-classic` | `test` | no |

No dependency in this POM uses `provided` scope; the mechanism the plugin relies on is `optional` (non-transitive) plus the `microsphere-resilience4j-spring` compile dependency.

## Registration / setup steps

1. Add the dependency above, plus `microsphere-spring-web` and `microsphere-spring-webmvc` (both are marked optional upstream).
2. Enable the Web MVC extension so handler-method interception exists: `@EnableWebMvcExtension` (on a `@Configuration` class or directly on your test class, as the module test does).
3. Enable at least one Resilience4j module **and list this plugin by name** in its `plugins` attribute. `plugins` is an `@AliasFor` of `EnableResilience4j.plugins`, matched against `Resilience4jPlugin#getName()`, whose value here is the constant `SpringWebResilience4jPlugin.PLUGIN_NAME = "spring-web"`. Without that name in the list, `SpringWebResilience4jPlugin#plugin(...)` is never invoked and no interceptor bean exists.
4. (Optional) `setOrder(int)` on the `resilience4jHandlerMethodInterceptor` bean if you need to run before/after other handler-method interceptors.

```java
import io.microsphere.resilience4j.spring.bulkhead.annotation.EnableBulkhead;
import io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin;
import io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@EnableWebMvcExtension
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)
public class ResilienceWebConfiguration {
}
```

## Complete runnable example (the module's own test)

`SpringWebResilience4jPluginTest` is the reference setup — an annotation-driven, non-Boot Spring MVC test that drives the plugin through `MockMvc`:

```java
@SpringJUnitConfig(classes = {
        SpringWebResilience4jPluginTest.class,   // the configuration class itself
        TestController.class                     // from microsphere-spring-test
})
@WebAppConfiguration
@EnableWebMvc
@EnableWebMvcExtension
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)   // static-imported PLUGIN_NAME in the test
class SpringWebResilience4jPluginTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(this.context).build();
    }

    @Test
    void test() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = new User();
        user.setName("Mercy");
        user.setAge(18);
        String json = objectMapper.writeValueAsString(user);
        this.mockMvc.perform(post("/test/user")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(json));
    }
}
```

What this proves: the POST to `/test/user` is handled by `TestController` (which echoes the `User` payload back as JSON), the bulkhead is configured with defaults through `@EnableBulkhead`, and the resilience decoration is applied to that handler method with the plugin active. Run it with:

```bash
./mvnw -pl microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web -am test
```

## API reference

### `SpringWebResilience4jPlugin implements Resilience4jPlugin`

```java
public static final String PLUGIN_NAME = "spring-web";

@Override
public void plugin(ConfigurableListableBeanFactory beanFactory,
                   BeanDefinitionRegistry registry,
                   ConfigurableEnvironment environment) {
    registerResilience4jHandlerMethodInterceptor(registry);
}

@Override
public String getName() {
    return PLUGIN_NAME;
}

private void registerResilience4jHandlerMethodInterceptor(BeanDefinitionRegistry registry) {
    registerBeanDefinition(registry, BEAN_NAME, Resilience4jHandlerMethodInterceptor.class);
}
```

The `beanFactory` and `environment` parameters are ignored: registration is a bean **definition** only (`registerBeanDefinition` from `io.microsphere.spring.beans.factory.support.BeanRegistrar`), no instance is created and no property is read.

### `Resilience4jHandlerMethodInterceptor`

Declared as:

```java
public class Resilience4jHandlerMethodInterceptor implements HandlerMethodInterceptor,
        ApplicationListener<WebEndpointMappingsReadyEvent>, Ordered
```

| Member | Signature / behaviour |
|--------|---------------------|
| constant | `public static final String BEAN_NAME = "resilience4jHandlerMethodInterceptor"` |
| constructor | `public Resilience4jHandlerMethodInterceptor(Resilience4jFacade resilience4jFacade)` — asserts non-null via the statically imported `io.microsphere.util.Assert.assertNotNull` (Microsphere's own `Assert`, not Spring's) |
| `@Override` | `public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request)` → `facade.begin(getEntryName(handlerMethod))`, result stored as a request attribute named `Resilience4jContext.class.getName()` |
| `@Override` | `public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request)` → removes the attribute; if `null` it logs a `trace` and returns; otherwise `context.setResult(returnValue).setFailure(error)` then `facade.end(context)` |
| `@Override` | `public void onApplicationEvent(WebEndpointMappingsReadyEvent event)` → `initEntryCache(event)` |
| `@Override` | `public int getOrder()` / `public void setOrder(int order)` (field default `0`) |
| accessor | `@Nonnull public final Resilience4jFacade getResilience4jFacade()` |
| naming | `public String getEntryName(HandlerMethod handlerMethod)` → `methodEntryNamesCache.computeIfAbsent(method, m -> PLUGIN_NAME + "@" + handlerMethod)` |
| override point | `protected void initEntryCache(WebEndpointMappingsReadyEvent event)` |

The Javadoc calls the class “the abstract template class for Resilience4j's `HandlerMethodInterceptor`”, but it is **concrete** and is instantiated directly by the plugin; `initEntryCache` is the only protected extension point.

### Entry names and how they bind to Resilience4j instances

```text
spring-web@ + handlerMethod.toString()
```

`PLUGIN_NAME + "@" + handlerMethod` is the concatenation in source, so the tail is exactly what Spring's `HandlerMethod#toString()` renders for that controller method (the rendering itself is Spring's and is not reproduced here — treat the whole key as opaque but deterministic). The cache key is the `Method`, so the name is per handler method, stable across requests and independent of arguments. Commons resolves it as `registry.find(name)` → otherwise `registry.<module>(name, getConfiguration(name))`, and `getConfiguration(name)` looks up a **named configuration whose name equals the entry name**, so you can target one endpoint by registering a configuration under that exact key; anything unmatched uses the registry default. `initEntryCache` also pushes all pre-computed names into each template's local cache (`Resilience4jTemplate#initLocalEntriesCache(names)`), which is the designed way to keep the `HashMap`-backed caches safe at runtime.

## Which modules apply (a hard limitation of this plugin)

This plugin uses the **two-phase** `begin(...)` / `end(...)` operations, not the one-time `call(...)`. In the 1.x commons line three templates report `isBeginSupported() == false` and `isEndSupported() == false`, and are therefore silently skipped:

| Module | Participates in handler-method resilience? | Action performed (1.x commons sources) |
|--------|--------------------------------------------|----------------------------------------|
| `CircuitBreaker` | yes | `begin`: `nanoTime()` then `circuitBreaker.acquirePermission()`, start time stored on the context; `end`: `onSuccess(duration, NANOSECONDS)` or `onError(duration, NANOSECONDS, failure)` |
| `Bulkhead` | yes | `begin`: `bulkhead.acquirePermission()`; `end`: `bulkhead.onComplete()` |
| `RateLimiter` | yes | both `doBegin` **and** `doEnd` call `RateLimiter.waitForPermission(rateLimiter)` |
| `Retry` | **no** | skipped — retry needs the whole call in one callback |
| `TimeLimiter` | **no** | skipped |
| `ThreadPoolBulkhead` | **no** | skipped |

If you need retry or time limiting on a controller, decorate the call at another layer (the [openfeign](microsphere-resilience4j-openfeign.md) plugin's one-time `call(...)` path, or `Resilience4jOperations#execute/call` directly).

## Behavioral notes

| Topic | Behaviour (from source) |
|-------|--------------------------|
| Rejection surfaces as an exception | `beforeExecute` runs *before* your controller method; `acquirePermission()` throws `CallNotPermittedException` / `RequestNotPermitted` (rate limiting blocks instead, waiting for a permit). This plugin has **no fallback hook** — the exception propagates into the Spring MVC dispatch and becomes an error response. |
| Permit release depends on `afterExecute` | The bulkhead permit is only released by `bulkhead.onComplete()` inside `afterExecute`. Because the context is carried in a request attribute (`WebScope.REQUEST`), the pair must both run for the same request. |
| Partial `begin` is not compensated | `ChainableResilience4jFacade#begin` creates the sub-contexts one template at a time; if a later template rejects, no composite context is returned, so `beforeExecute` stores nothing and `afterExecute` logs “The 'context' is null …” and returns — the earlier templates' `end` phase (including `onComplete()`) is never reached. Keep the module set small and predictable on this path. |
| Rate limiter consumes two permits per request | Verified in 1.x commons: `RateLimiterTemplate.doBegin` and `doEnd` both call `waitForPermission(rateLimiter)`. Size your `limitForPeriod` accordingly (roughly double the intended QPS per handler method). |
| Thread-safety | The interceptor keeps no per-request state (the context lives in the request attribute), and its mutable fields are `order` (a plain `int`, written only during configuration) and `methodEntryNamesCache`. The cache is created and filled during `WebEndpointMappingsReadyEvent`; a handler method outside those mappings is added lazily by `computeIfAbsent` at request time, which mutates that map concurrently. Prefer having all endpoints discovered by the event, and set `order` before the context refresh finishes. |
| Name cache must be initialised | `getEntryName` dereferences `methodEntryNamesCache` directly; if `WebEndpointMappingsReadyEvent` never reached this listener (e.g. the Web extension does not publish events in your configuration), the first handler call throws `NullPointerException`. `@EnableWebMvcExtension` in the test is what makes the event flow; the plugin itself does not publish it. |
| Wrapping is per handler method only | `@Override`d callbacks receive a `HandlerMethod`; static resources, `ResourceHttpRequestHandler` and non-`HandlerMethod` endpoints are not decorated. |
| Logging | `trace` level only, through `io.microsphere.logging` (`LoggerFactory.getLogger(...)`), on the null-context path and on each created entry name. |

## See also

- Developer-side analysis (SPI wiring, interceptor chain internals, dependency graph, sequence diagram): [../developer-guide/microsphere-resilience4j-spring-web.md](../developer-guide/microsphere-resilience4j-spring-web.md)
- The `@Enable*` annotations and facade beans this plugin depends on: [microsphere-resilience4j-spring.md](microsphere-resilience4j-spring.md)
