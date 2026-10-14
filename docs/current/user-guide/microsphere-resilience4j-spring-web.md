# microsphere-resilience4j-spring-web — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← user guide index](README.md)

## What this plugin does

`microsphere-resilience4j-spring-web` decorates **Spring MVC / WebFlux handler methods** (your `@RequestMapping` controller methods) with Resilience4j, using the handler-method interception extension point of `microsphere-spring-web`.

| Class | Role |
|-------|------|
| `io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin` | `Resilience4jPlugin` named `spring-web`; registers the interceptor bean definition when the plugin is listed in an `@Enable*` annotation |
| `io.microsphere.resilience4j.spring.web.Resilience4jHandlerMethodInterceptor` | `io.microsphere.spring.web.method.support.HandlerMethodInterceptor` + `ApplicationListener<WebEndpointMappingsReadyEvent>` + `Ordered`; opens/closes the Resilience4j decoration around a handler-method execution |

Where it hooks in: `microsphere-spring-web(mvc)` replaces the argument-resolution and return-value-handling steps of Spring MVC with `InterceptingHandlerMethodProcessor`, which notifies every `HandlerMethodAdvice` bean. The `DelegatingHandlerMethodAdvice` bean in turn fans those notifications out to all sorted `HandlerMethodInterceptor` beans — one of which is this plugin's `resilience4jHandlerMethodInterceptor`. `beforeExecute` runs **after the last handler-method argument has been resolved** (so immediately before your controller method is invoked) and `afterExecute` runs **after the return value has been handled** (or from `afterCompletion` when the dispatch failed).

This is the only plugin that uses the **two-phase** `Resilience4jFacade.begin(...)` / `end(context)` operations, because the protected call starts and ends in two different framework callbacks.

## Maven coordinates and dependency wiring

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-spring-web</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

From `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/pom.xml`, `microsphere-resilience4j-spring` is the only non-optional dependency:

| Dependency | Scope in the plugin POM | Notes |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile | brings `microsphere-resilience4j-commons` and the `Resilience4jPlugin` SPI |
| `io.github.microsphere-projects:microsphere-spring-web` | **optional** | supplies `HandlerMethodInterceptor`, `WebEndpointMapping`, `WebEndpointMappingsReadyEvent`, `WebScope` — **required at runtime** |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | supplies `BeanRegistrar.registerBeanDefinition(...)` used by the plugin |
| `org.springframework:spring-web` | **optional** | `HandlerMethod`, `NativeWebRequest` |
| `org.springframework:spring-webmvc` | **optional (compile-level)** | only needed for the servlet stack; WebFlux users need `spring-webflux` instead |
| `io.github.resilience4j:resilience4j-spring` | **optional** | provides `*Configuration` / `*Registry` and the `*ConfigurationProperties` types the `@Enable*` annotations bind |
| `microsphere-spring-webmvc`, `spring-test`, `spring-aop`, `microsphere-spring-test`, `jakarta.servlet-api`, `jackson-databind`, `aspectjweaver`, `jsonassert`, `hamcrest`, `junit-jupiter`, `logback-classic` | **test** | used by `SpringWebResilience4jPluginTest` |

You must therefore add (at least) `microsphere-spring-web`, `microsphere-spring-webmvc` (or `microsphere-spring-webflux`) and `resilience4j-spring` yourself; the versions come from the imported `microsphere-spring-cloud-dependencies` / `microsphere-spring-dependencies` BOMs when you build inside this reactor, otherwise pin them explicitly.

## Version and compatibility

| Item | Requirement | Notes |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x` (`release`) | artifact `microsphere-resilience4j-spring-web` |
| Resilience4j | 2.0.0 – 2.4.0 | default profile `resilience4j-2.4` → `2.4.0` |
| Spring Framework / Boot | application-supplied | the `microsphere-spring-cloud-dependencies` line used by this repo maps Spring Cloud 2022.0.x (Boot 3.0.x) up to 2025.0.x (Boot 3.5.x), so `jakarta.servlet.*` (Boot 3+) is the assumed stack |
| microsphere-spring-web(-mvc/-webflux) | required | `@EnableWebMvcExtension` / `@EnableWebFluxExtension` and the interception processor live there |
| JDK | 17+ | root POM `java.version = 17` |

## Setup

The plugin needs three things: an enabled Resilience4j module, the plugin name in that annotation, and handler-method interception switched on.

```java
@Configuration
@EnableWebMvc                      // plain Spring MVC
@EnableWebMvcExtension             // microsphere-spring-webmvc: interception + endpoint mappings
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)  // "spring-web"
@EnableCircuitBreaker
class WebResilience4jConfig {
}
```

What happens step by step:

1. `@EnableBulkhead` (like every `@Enable*` in `microsphere-resilience4j-spring`) triggers `EnableResilience4jRegistrar`, which calls `registerPlugins(...)`: `Resilience4jPlugin` implementations are loaded from `META-INF/spring.factories`, and for each whose `getName()` appears in the annotation's `plugins` attribute, `plugin(beanFactory, registry, environment)` runs.
2. `SpringWebResilience4jPlugin.plugin(...)` registers a bean definition named `resilience4jHandlerMethodInterceptor` (`Resilience4jHandlerMethodInterceptor.BEAN_NAME`) for the interceptor class — no constructor arguments are declared, so Spring satisfies the single `Resilience4jFacade` constructor with the container's `Resilience4jFacade` bean.
3. The same registrar then registers the lazy facade itself: `lazyResilience4jFacade` (a `LazyResilience4jFacade`, marked **primary**) whose `modules` property accumulates one entry per enabled `@Enable*` module. Its delegate `ChainableResilience4jFacade` is assembled on `ContextRefreshedEvent` from the `*Registry` beans in the context (`context.getBean(registryClass)`).
4. `@EnableWebMvcExtension` registers `InterceptingHandlerMethodProcessor` / `DelegatingHandlerMethodAdvice`, publishes `WebEndpointMappingsReadyEvent`, and by default sets `interceptHandlerMethods = true`.

> Plugin names are matched against `Resilience4jPlugin#getName()` (`EnableResilience4jRegistrar#registerPlugins`), even though the annotation Javadoc calls the attribute “the Spring Bean names of the plugins”.

### Manual registration (without the plugin mechanism)

```java
@Bean
public Resilience4jHandlerMethodInterceptor resilience4jHandlerMethodInterceptor(Resilience4jFacade facade) {
    Resilience4jHandlerMethodInterceptor interceptor = new Resilience4jHandlerMethodInterceptor(facade);
    interceptor.setOrder(0);
    return interceptor;
}
```

Any `Resilience4jFacade` works as long as it is ready before the first request; outside Spring it is `ChainableResilience4jFacade`.

## Public API

### `SpringWebResilience4jPlugin`

| Member | Behaviour (from source) |
|--------|--------------------------|
| `public static final String PLUGIN_NAME = "spring-web"` | also returned by `getName()` |
| `plugin(ConfigurableListableBeanFactory, BeanDefinitionRegistry, ConfigurableEnvironment)` | ignores the bean factory and environment, calls `registerBeanDefinition(registry, BEAN_NAME, Resilience4jHandlerMethodInterceptor.class)` |
| `getName()` | `PLUGIN_NAME` |

### `Resilience4jHandlerMethodInterceptor`

| Member | Behaviour (from source) |
|--------|--------------------------|
| `public static final String BEAN_NAME = "resilience4jHandlerMethodInterceptor"` | the bean name used by the plugin |
| `Resilience4jHandlerMethodInterceptor(Resilience4jFacade)` | `assertNotNull(resilience4jFacade, () -> "The 'resilience4jFacade' argument can't be null")` |
| `beforeExecute(HandlerMethod, Object[], NativeWebRequest)` | `facade.begin(getEntryName(handlerMethod))` and stores the returned `Resilience4jContext<Resilience4jContext[]>` in **request scope** under the attribute name `Resilience4jContext.class.getName()` (`WebScope.REQUEST`) |
| `afterExecute(HandlerMethod, Object[], Object returnValue, Throwable error, NativeWebRequest)` | `WebScope.REQUEST.removeAttribute(...)`; if nothing is found it logs `trace` *“The 'context' is null , please check whether the 'resilience4jFacade' is a 'ChainableResilience4jFacade'”* and returns; otherwise `context.setResult(returnValue).setFailure(error)` then `facade.end(context)` |
| `getEntryName(HandlerMethod)` | `methodEntryNamesCache.computeIfAbsent(method, m -> PLUGIN_NAME + "@" + handlerMethod)` — cached per `java.lang.reflect.Method` |
| `onApplicationEvent(WebEndpointMappingsReadyEvent)` → `protected initEntryCache(event)` | builds the name cache (`newFixedHashMap(mappings.size())`), pre-computes a name for every endpoint whose `getEndpoint()` is a `HandlerMethod`, then — if the facade unwraps to a `ChainableResilience4jFacade` (`Wrapper.tryUnwrap`) — calls `template.initLocalEntriesCache(names)` on every template |
| `getResilience4jFacade()` | `@Nonnull final` accessor |
| `getOrder()` / `setOrder(int)` | `Ordered`; the container sorts all `HandlerMethodInterceptor` beans with it |

### Resilience4j entry name

```java
PLUGIN_NAME + "@" + handlerMethod   // "spring-web@" + HandlerMethod#toString()
```

`HandlerMethod#toString()` renders the underlying `java.lang.reflect.Method`, so a name looks like:

```
spring-web@public org.springframework.http.ResponseEntity com.acme.UserController.get(java.lang.Long)
```

Two consequences:

- The name contains **spaces, dots and parentheses**. Quote it in YAML/properties keys, e.g. `"spring-web@public org.springframework.http.ResponseEntity com.acme.UserController.get(java.lang.Long)": …`. Print one name at `trace` level (`io.microsphere.resilience4j.spring.web.Resilience4jHandlerMethodInterceptor` logs `Create the entryName : '{}' for HandlerMethod : {}` during startup) before writing configuration.
- The cache key is the `java.lang.reflect.Method`, the cached value embeds `HandlerMethod#toString()`. Two `@RequestMapping` paths that dispatch to the same method therefore share **one** entry, while a method declared by a controller bean and an inherited copy used by another bean get separate `Method` keys.

### Selecting per-entry Resilience4j configuration

`Resilience4jTemplate` resolves an entry with `registry.find(name)` and, on a miss, `createEntry(name)` → `registry.<module>(name, getConfiguration(name))`, where `getConfiguration(name)` asks the registry for the **named configuration whose key equals the entry name** and falls back to `registry.getDefaultConfig()`. Two ways to steer it:

1. Through the properties bound by `@Enable*`: each annotation binds Resilience4j's own configuration-properties type under a `microsphere.resilience4j.<module>.` prefix — e.g. `@EnableBulkhead` binds `io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties` to `microsphere.resilience4j.bulkhead.` and `CommonThreadPoolBulkheadConfigurationProperties` to `microsphere.resilience4j.thread-pool-bulkhead.` (`@EnableCircuitBreaker` → `microsphere.resilience4j.circuit-breaker.`, `@EnableRateLimiter` → `microsphere.resilience4j.rate-limiter.`, `@EnableRetry` → `microsphere.resilience4j.retry.`, `@EnableTimeLimiter` → `microsphere.resilience4j.time-limiter.`). The key layout below each prefix is Resilience4j's own (`configs` / `instances` maps), so check the field names against the Resilience4j version you use:

   ```yaml
   # the <entry-name> keys are the quoted HandlerMethod names described above
   microsphere:
     resilience4j:
       bulkhead:
         configs:
           tight:
             maxConcurrentCalls: 2
         instances:
           "spring-web@public org.springframework.http.ResponseEntity com.acme.UserController.get(java.lang.Long)":
             maxConcurrentCalls: 8
   ```

2. Programmatically, on the registry bean: `registry.addConfiguration(entryName, config)` **before** the first request to that handler method.

## Complete runnable example

Adapted from `SpringWebResilience4jPluginTest`, the only test of this module. The upstream test runs the same annotations inside a `@SpringJUnitConfig` + `@WebAppConfiguration` context and drives the controller with `MockMvc`; the controller itself (`io.microsphere.spring.test.web.controller.TestController`, from `microsphere-spring-test`) is a test-scope bean that answers `POST /test/user` with the submitted `User`.

```java
@EnableWebMvc
@EnableWebMvcExtension
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)
@EnableCircuitBreaker
@SpringJUnitConfig(classes = { WebConfig.class, UserController.class })
@WebAppConfiguration
class UserControllerTest {

    @Autowired WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(this.context).build();
    }

    @Test
    void decoratedRequest() throws Exception {
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

Your own controller only has to be an ordinary `@Controller`/`@RestController`:

```java
@RestController
@RequestMapping("/test")
public class UserController {

    @PostMapping(value = "/user", produces = APPLICATION_JSON_VALUE)
    public User createUser(@RequestBody User user) {   // one entry name for this Method
        return user;
    }
}
```

Every request mapped to that method is decorated under `spring-web@public …UserController.createUser(…)`; the bulkhead limits concurrent executions and the circuit breaker counts successes/failures of the whole handler-method execution (arguments resolved, method invoked, return value handled).

## Behavioural notes and caveats

- **Only the two-phase-capable modules are active here.** `begin(name)` / `end(context)` ask each template `isBeginSupported()` / `isEndSupported()`; `RetryTemplate`, `TimeLimiterTemplate` and `ThreadPoolBulkheadTemplate` answer `false` and are **silently skipped**. `CircuitBreakerTemplate` (acquire permission, then `onSuccess`/`onResult`/`onError`), `BulkheadTemplate` and `RateLimiterTemplate` do support the pair. Use the [OpenFeign](microsphere-resilience4j-openfeign.md), [MyBatis](microsphere-resilience4j-mybatis.md) or [Druid](microsphere-resilience4j-alibaba-druid.md) plugins if you need retry or timeout decoration.
- **A blocked request is not swallowed.** The three begin-capable templates each reject in `begin(...)`: `CircuitBreakerTemplate` calls `CircuitBreaker#acquirePermission()` → `CallNotPermittedException`; `BulkheadTemplate` calls `Bulkhead#acquirePermission()` → `RequestNotPermitted`; `RateLimiterTemplate` calls the static `RateLimiter#waitForPermission(...)` helper, which first **blocks** for the limiter's `timeoutDuration` and then throws `RequestNotPermitted` (or `io.github.resilience4j.core.exception.AcquirePermissionCancelledException` when the thread is interrupted). The exception leaves `beforeExecute`, so Spring MVC turns it into a 500 unless you map it (`@ExceptionHandler`, `HandlerExceptionResolver`, or Spring Boot's `ErrorController`).
- **`end(...)` never runs without a context.** If `beforeExecute` did not store its request attribute — because the request never went through `InterceptingHandlerMethodProcessor`, or the attribute was cleared — `afterExecute` only logs at `trace` and returns. The consequence is per module: `BulkheadTemplate.doEnd` (`bulkhead.onComplete()`) is skipped, so the acquired **concurrency permit leaks** for that `Bulkhead` instance; `CircuitBreakerTemplate.doEnd` and `RateLimiterTemplate.doEnd` are skipped too, so the call is never recorded as success or failure (and `onError` never trips the breaker). Async requests are the usual cause; make sure interception is enabled and the request is dispatched through the handler method processor in both the initial and the async dispatch.
- **The entry-name cache is populated by an event.** `methodEntryNamesCache` is assigned only inside `initEntryCache(WebEndpointMappingsReadyEvent)`; `getEntryName(...)` calls `computeIfAbsent` on it. If that event is never published (the `@EnableWebMvcExtension` / `@EnableWebFluxExtension` machinery is absent), `beforeExecute` fails with a `NullPointerException` on the first request rather than degrading gracefully.
- **`@Order` matters.** The interceptor is one of possibly many `HandlerMethodInterceptor` beans, and the advice loop runs them in sorted order; place it early (`Ordered.HIGHEST_PRECEDENCE`) if other interceptors may throw before your decoration ends.
- **Coverage is per handler method, not per URL.** The same method reached through different mappings (multiple `@RequestMapping` paths, `@RequestMapping` on class + method) yields **one** entry; conversely `@RequestMapping` wildcard patterns cost nothing extra. Feign-style per-URL isolation is therefore not available on this plugin.
- **No `@EnableResilience4j` on the plugin itself.** This module ships only the plugin descriptor and the interceptor: nothing is activated unless you (a) list `spring-web` in an `@Enable*` annotation **and** (b) enable the Web extension. Forgetting (b) is the most common silent failure — the bean exists but nobody calls it.
- **WebFlux is supported by the same classes.** The interceptor's Javadoc points at `EnableWebMvcExtension` and `EnableWebFluxExtension`; both publish `WebEndpointMappingsReadyEvent` and dispatch `HandlerMethodInterceptor` beans. Only the test in this repository covers the MVC path.

## Related pages

- [Developer guide — microsphere-resilience4j-spring-web](../developer-guide/microsphere-resilience4j-spring-web.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
