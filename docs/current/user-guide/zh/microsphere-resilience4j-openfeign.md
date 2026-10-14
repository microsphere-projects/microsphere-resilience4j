# microsphere-resilience4j-openfeign — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`） · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 用户指南索引](README.md)
> [English](../microsphere-resilience4j-openfeign.md)

## 本插件的作用

`microsphere-resilience4j-openfeign` 为**原生 OpenFeign 客户端**（即 `io.github.openfeign:feign-core` 编程模型 —— `Feign.builder()` + `@RequestLine` 接口）添加装饰，使其获得由 `Resilience4jFacade` 所持有的 Resilience4j 装饰能力。它以单个 `feign.Capability` 的形式交付，因此不涉及代理 bean、注解，也不依赖 Spring 容器。

| 类 | 角色 |
|-------|------|
| `io.microsphere.resilience4j.feign.Resilience4jCapability` | 入口点：一个 `feign.Capability`，依据其 `DecoratedPoint` 包装某一个 Feign 扩展点 |
| `io.microsphere.resilience4j.feign.Resilience4jClient` | `feign.Client` 装饰器 —— 仅保护 **HTTP 传输调用**（默认装饰点） |
| `io.microsphere.resilience4j.feign.Resilience4jInvocationHandlerFactory` | `feign.InvocationHandlerFactory` 装饰器 —— 构建具备弹性感知能力的 JDK 代理处理器 |
| `io.microsphere.resilience4j.feign.Resilience4jInvocationHandler` | `java.lang.reflect.InvocationHandler` 装饰器 —— 保护**整个 Feign 方法调用**（参数模板化、请求执行、解码） |

### 它在 Feign 中的挂载位置

`Feign.Builder.addCapability(Capability)` 是 Feign 自身的扩展 SPI。在 `Feign.Builder#build()` 期间，每一个已注册的能力扩展都会被要求对它所了解的组件执行 `enrich(...)`。`Resilience4jCapability` 恰好实现了其中两个钩子：

- `Client enrich(Client client)` → 当选择 `DecoratedPoint.CLIENT` 时，返回一个包装了原始对象的 `Resilience4jClient`（否则原样返回入参）。
- `InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` → 当选择 `DecoratedPoint.INVOCATION_HANDLER_FACTORY` 时，返回一个 `Resilience4jInvocationHandlerFactory`（否则原样返回入参）。

由于 `Capability` 是在构建时被应用到对象图上的，一次注册即可保护由该 `Feign` 实例创建出来的每一个接口的每一个方法。该装饰位于 Feign 自身下游所有行为的**外部**：在 `CLIENT` 路径上，它介于 `SynchronousMethodHandler` 与传输层之间；在 `INVOCATION_HANDLER_FACTORY` 路径上，它介于你的服务接口与 Feign 生成的代理之间。

这是五个插件中唯一一个**与框架无关**的插件 —— 它依赖 `microsphere-resilience4j-commons`（而非 `microsphere-resilience4j-spring`），并且不携带任何 `META-INF/spring.factories`。

## Maven 坐标与依赖装配

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-openfeign</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

阅读 `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/pom.xml` 可知，恰好只有一个 compile 作用域的依赖是非可选的：

| 依赖 | 在插件 POM 中的作用域 | 说明 |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-commons` | **compile（非可选）** | `Resilience4jFacade` / `ChainableResilience4jFacade` —— 你需要自行构建门面 |
| `io.github.openfeign:feign-core` | **optional** | 必须由你的应用提供；本仓库未固定其版本 |
| `io.github.resilience4j:resilience4j-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter`、`-annotations`、`-consumer`、`-framework-common` | **optional** | 只有 `-framework-common` 在本 POM 中带有显式的 `${resilience4j.version}` |
| `io.github.resilience4j:resilience4j-feign` | **optional** | 仅在 `Resilience4jInvocationHandler` 的 Javadoc 中被引用（`@see io.github.resilience4j.feign.DecoratorInvocationHandler`）；运行时不使用它的任何类 |
| `io.github.microsphere-projects:microsphere-java-core` | **optional** | 提供 `io.microsphere.reflect.MethodUtils`、`io.microsphere.util.StringUtils`、`io.microsphere.constants.SymbolConstants` —— 若你依赖它，请自行声明 |
| `io.github.microsphere-projects:microsphere-annotation-processor` | **optional** | 仅用于构建期 |
| `junit-jupiter`、`microsphere-java-test`、`feign-gson`、`logback-classic` | **test** | 供三个测试类使用 |

本模块**没有**对 `microsphere-resilience4j-spring`、Spring Framework 或 Spring Cloud 的任何依赖。

## 版本与兼容性

| 项目 | 要求 | 说明 |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x`（`release`） | 构件 `microsphere-resilience4j-openfeign` |
| Resilience4j | 2.0.0 – 2.4.0 | 由 `microsphere-resilience4j-parent` 的 Maven profile 选择；默认 profile `resilience4j-2.4` → `2.4.0` |
| OpenFeign | 由应用提供 | 需要 `Capability.enrich(Client)` / `enrich(InvocationHandlerFactory)`、`Request#requestTemplate()`、`RequestTemplate#feignTarget()`、`RequestTemplate#queryLine()`、`MethodMetadata#template()` —— 即 Spring Cloud 2022.0.x – 2025.0.x 所使用的 Feign 10.x+ API 线 |
| Spring Cloud OpenFeign | 可选 | 间接支持：你可以贡献一个 `feign.Capability` bean；本模块不会自动配置它 |
| JDK | 17+ | 根 POM `java.version = 17` |

## 配置

### 1. 构建一个门面

该插件需要一个 `Resilience4jFacade`。在 Spring 之外，`ChainableResilience4jFacade`（来自 commons）是常用选择 —— 这正是测试基类所做的：

```java
Resilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(),
        RetryRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults());
```

各个注册表以可变参数 `Registry...` 传入；门面会为每个注册表构建一个 `Resilience4jTemplate`，并按 `Resilience4jModule` 优先级排序。

### 2. 将能力扩展注册到你的 `Feign.Builder`

```java
Resilience4jCapability capability = new Resilience4jCapability(facade); // CLIENT point, "feign@" prefix

Feign feign = Feign.builder()
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .addCapability(capability)   // add it last if other capabilities must stay inside the decoration
        .build();
```

### 3. （可选）选择装饰点或入口名前缀

```java
// protect the whole method invocation instead of the transport call
new Resilience4jCapability(facade, Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);

// custom prefix (must be unique if several Feign client groups share a registry)
new Resilience4jCapability(facade, "billing-feign@");

// both
new Resilience4jCapability(facade, "billing-feign@", Resilience4jCapability.DecoratedPoint.CLIENT);
```

在 Spring 内部，你可以复用由 `microsphere-resilience4j-spring` 注册的门面（`LazyResilience4jFacade.BEAN_NAME = "lazyResilience4jFacade"`），并自行暴露该能力扩展：

```java
@Configuration
public class FeignResilience4jConfig {

    @Bean
    public Resilience4jCapability resilience4jCapability(Resilience4jFacade lazyResilience4jFacade) {
        return new Resilience4jCapability(lazyResilience4jFacade);
    }
}
```

> 本模块**不**注册任何 `Resilience4jPlugin`，也**不**提供 `@Enable*` 支持：与 Druid、MyBatis、Hibernate 和 Spring Web 插件不同，这里没有任何自动发生的事情。一个 `Capability` bean 是否会被 Spring Cloud OpenFeign 拾取，取决于该项目自身的按客户端配置机制 —— 请对照你所使用的 Spring Cloud 版本加以确认，或者像上面那样自行构建 `Feign` 实例。

## 公共 API

### `Resilience4jCapability`

| 成员 | 行为（源自代码） |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "feign@"` | 默认前缀 |
| `public static final DecoratedPoint DEFAULT_DECORATED_POINT = CLIENT` | 默认装饰点 |
| `Resilience4jCapability(Resilience4jFacade)` | 前缀 = `feign@`，装饰点 = `CLIENT` |
| `Resilience4jCapability(Resilience4jFacade, DecoratedPoint)` | 前缀 = `feign@`，自定义装饰点 |
| `Resilience4jCapability(Resilience4jFacade, String entryNamePrefix)` | 自定义前缀，装饰点 = `CLIENT` |
| `Resilience4jCapability(Resilience4jFacade, String, DecoratedPoint)` | 唯一进行字段赋值的构造函数；无 null 校验 |
| `Client enrich(Client)` | 仅当 `isDecoratedClient()` 时才装饰 |
| `InvocationHandlerFactory enrich(InvocationHandlerFactory)` | 仅当 `isDecoratedInvocationHandlerFactory()` 时才装饰 |
| `getDecoratedPoint()`、`isDecoratedClient()`、`isDecoratedInvocationHandlerFactory()` | 针对唯一被选中的 `DecoratedPoint` 的访问器 / 判定方法 |
| `enum DecoratedPoint { CLIENT, INVOCATION_HANDLER_FACTORY }` | 每个能力扩展只能装饰恰好一个装饰点 |

### `Resilience4jClient`（传输边界）

```java
public Resilience4jClient(Client delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public Response execute(Request request, Request.Options options) throws IOException {
    String entryName = buildEntryName(request);
    return this.facade.call(entryName, () -> this.delegate.execute(request, options), IOException.class);
}
```

入口名由私有的 `buildEntryName(Request)` 拼装而成：

```
<entryNamePrefix> + httpMethod.name() + " " + requestTemplate.feignTarget().url()
                  + requestTemplate.methodMetadata().template().path()
                  + requestTemplate.queryLine()   // appended only when StringUtils.isNotBlank(queryLine)
```

| Feign 声明 | 生成的入口名 |
|-------------------|----------------------|
| 针对 `http://localhost:8080` 的 `@RequestLine("POST /user/{name}")` | `feign@POST http://localhost:8080/user/{name}` |
| 带查询参数 `name=test-user` 的 `@RequestLine("GET /user")` | `feign@GET http://localhost:8080/user?name=test-user` |

路径来自**方法元数据**的模板（因此 `{name}` 这类变量保留其声明形式），而查询行来自**按请求**的模板（因此查询值通常已被展开）。由于两半来自不同的模板，在编写按入口配置之前，请先在你的环境中打印一个入口名确认。

### `Resilience4jInvocationHandlerFactory` / `Resilience4jInvocationHandler`（方法边界）

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

`getDelegate()` 与 `getFacade()` 是带 `@Nonnull` 的访问器。入口名是**每个接口方法**一个稳定资源（声明类型 + 方法名 + 参数类型，由 `io.microsphere.reflect.MethodUtils#getSignature(Method)` 渲染而成），例如 `feign@<UserService.getUser(String) 的签名>`。

### 按入口的 Resilience4j 配置

`AdvancedResilience4jOperations#getEntry(name)` 会在注册表中查找该名称，未命中时以 `getConfiguration(name)` 创建该入口 —— 也就是说，采用**键等于入口名的注册表命名配置**，若不存在则回退到 `registry.getDefaultConfig()`。因此，对于一个普通的 Resilience4j 注册表：

```java
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
circuitBreakerRegistry.addConfiguration(
        "feign@POST http://localhost:8080/user/{name}",                  // the entry name
        CircuitBreakerConfig.custom().failureRateThreshold(20).build()); // your config
```

`microsphere.resilience4j.circuit-breaker.` / `microsphere.resilience4j.bulkhead.` … 属性前缀（Spring 侧）在 [spring 模块页面](microsphere-resilience4j-spring.md) 上有文档说明。

## 完整可运行示例

### 1. 默认配置（`CLIENT` 点）—— 改写成自单元测试

上游测试用一个进程内的本地 `Client` 替换了真实的 HTTP 传输，使整个往返无需服务器即可运行；生产应用则会使用 `Feign.builder()… .target(UserService.class, "http://localhost:8080")`（或 `hardCodedTarget`）来代替。

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

`AbstractResilience4jFeignTest#testUserService` 断言的正是这一往返（`assertEquals(userName, user.getName())`、`assertEquals(user, foundUser)`），而 `Resilience4jCapabilityTest#testConstants` 断言 `DEFAULT_ENTRY_NAME_PREFIX == "feign@"` 且 `DEFAULT_DECORATED_POINT == CLIENT`。

### 2. 方法级装饰（`INVOCATION_HANDLER_FACTORY` 点）

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

`AbstractResilience4jFeignTest#testProxy` 精确地走查了那条链：它断言 `handler instanceof Resilience4jInvocationHandler`、`getDelegate() instanceof` 测试自身的包装器、最内层委托的类名等于 `feign.ReflectiveFeign$FeignInvocationHandler`，并 `assertSame(facade, resilience4jInvocationHandler.getFacade())`。而在 `CLIENT` 点下，同一测试会断言该处理器**没有**被装饰（只有 `Client` 被装饰）。

## 行为说明与注意事项

- **异常形态取决于装饰点。**
  - `CLIENT`：`facade.call(name, supplier, IOException.class)` 声明 `throws IOException`；任何失败 —— 包括 Resilience4j 的 `CallNotPermittedException`、`MaximumWaitTimeExceededException` 或 `TimeoutException` —— 都会以 **`IOException`** 呈现，这正是 Feign 调用方通常所期望的（也是 `Retryer` / Spring Cloud 错误处理本就能应对的）。原始原因由包装步骤保留。
  - `INVOCATION_HANDLER_FACTORY`：`facade.call(name, supplier)` 以 **`Throwable`** 重新抛出，因此调用方看到的是**原始**异常类型，包括当熔断器打开时的 `io.github.resilience4j.CallNotPermittedException`。你的接口签名无法声明它，所以它会以非受检异常的形式传播，或在代理边界处通过 `UndeclaredThrowableException` 传递 —— 请显式捕获它，或改用 `CLIENT` 点。
- **Feign 调用期间熔断打开意味着没有 HTTP 流量。** 装饰在进入传输层之前就已生效，因此被阻止的调用从不触及网络，也从不占用连接池中的一个连接。
- **两个装饰点都会应用全部模块。** 两条路径都使用一次性的 `call(...)` 操作，因此 `RetryTemplate`、`TimeLimiterTemplate` 和 `ThreadPoolBulkheadTemplate` 都会参与 —— 这与 [Spring Web](microsphere-resilience4j-spring-web.md) 和 [Hibernate Core](microsphere-resilience4j-hibernate-core.md) 插件所使用、会跳过这三个模块的两阶段 `begin()/end()` 路径不同。
- **`TimeLimiter` 与 `ThreadPoolBulkhead` 会把调用移到另一个线程。** 在 `CLIENT` 点上，Feign 请求已完全构建好，所以只有传输在别处运行。在 `INVOCATION_HANDLER_FACTORY` 点上，整个代理调用都在别处运行，这会破坏任何绑定到调用线程的东西（请求上下文、`ThreadLocal` 传播、安全持有者）。
- **资源基数。** 在 `CLIENT` 点上，查询行是入口名的一部分，因此高基数的查询参数（id、时间戳）会使 Resilience4j 实例数量成倍增长 —— 每个不同的名称对应一个 `CircuitBreaker`/`RateLimiter`/`Bulkhead`，各自持有自己的环形缓冲区。若想要固定、按方法的资源集，请使用 `INVOCATION_HANDLER_FACTORY`。
- **`Retryer` 与 Resilience4j 的 `Retry` 是两回事。** Feign 自身的重试机制在 `ErrorDecoder` 层重试 `RetryableException`；在 `CLIENT` 点上，被包装成 `IOException` 的失败对自定义 `ErrorDecoder` 而言可能不再呈现 `RetryableException` 的形态。如果你同时使用两者，请只计算一次重试。
- **装饰顺序即构建顺序。** 每个能力扩展的 `enrich(...)` 都会包装上一个的返回值，因此最后添加的能力扩展成为最外层。请在那些你希望其工作被计入弹性边界内部的能力扩展**之后**再添加 `Resilience4jCapability`（测试正是出于这一原因先添加了 `DelegatingCapability`）。
- **同时使用两个装饰点需要两个能力扩展。** `DecoratedPoint` 是单值的；添加两个装饰点不同的 `Resilience4jCapability` 实例会使每次调用被限流器和隔层重复计数。建议只用一个装饰点。
- **门面就绪状态。** 如果你注入的是 Spring 的 `lazyResilience4jFacade`，请注意其委托链是在 `ContextRefreshedEvent` 时组装的；因此在上下文启动期间发起 Feign 调用，其行为可能不同于稳定运行状态。

## 相关页面

- [开发者指南 — microsphere-resilience4j-openfeign](../../developer-guide/zh/microsphere-resilience4j-openfeign.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-spring-web](microsphere-resilience4j-spring-web.md)
