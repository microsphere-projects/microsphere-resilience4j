# microsphere-resilience4j-openfeign — 用户指南（1.x）

> 版本线 `0.1.x`（`release-1.x` 分支） · 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0**（默认 `1.7.0`） · Spring Cloud **Hoxton – 2021.0.x**（父工程 `microsphere-spring-cloud-parent` `0.1.26`） · JDK **8** 基线 · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-openfeign.md)

仓库中的模块路径：`microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign`。
Java 包：`io.microsphere.resilience4j.feign`。

## 这个插件做什么

它包装一个 OpenFeign 客户端，使得**每一次 Feign 调用都经过 microsphere-resilience4j 门面**（来自 `microsphere-resilience4j-commons` 的 `Resilience4jFacade`），而不需要你手工一个模块一个模块地去装饰。这个门面由你所提供的 Resilience4j 注册中心支撑，因此同一个条目名称会被整条链——舱壁隔离（bulkhead）、限流器（rate limiter）、重试（retry）、超时限制器（time limiter）与断路器（circuit breaker）——以固定的、带优先级的顺序装饰。

该插件是**纯框架**的：它依赖 Feign 和 commons 模块，而不依赖 Spring 家族里的任何东西。它没有 `@Enable*` 注解，没有 `META-INF/spring.factories`，也没有 `Resilience4jPlugin` SPI 实现；你要通过 Feign 自己的扩展机制来注册它。

## 它挂载在哪里

Feign 暴露了 `feign.Capability`，那是一个由若干 `enrich(...)` 钩子组成的接口，`Feign.Builder` 会把它正在组装的各个组件依次在这些钩子上折叠（fold）。本插件恰好实现了其中的两个，而且**每个能力扩展实例只有其中一个生效**，具体由 `DecoratedPoint` 构造参数决定：

| `DecoratedPoint` | 所使用的 `feign.Capability` 钩子 | 生成的包装类 | 被拦截的是什么 |
|------------------|------------------------------|------------------------|---------------------|
| `CLIENT`（默认） | `Client enrich(Client client)` | `Resilience4jClient` | HTTP/`Client` 执行步骤，即 `Client#execute(Request, Request.Options)` |
| `INVOCATION_HANDLER_FACTORY` | `InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` | `Resilience4jInvocationHandlerFactory` → `Resilience4jInvocationHandler` | JDK 代理的方法调用，即 `InvocationHandler#invoke(Object, Method, Object[])` |

由此得到的包装链如下（下面出现的类名，要么是模块自身的测试所断言的，要么是插件源码所 import 的；中间那个 Feign 处理器属于 Feign 自身，而不是本插件的）：

```text
CLIENT mode
  proxy -> feign.ReflectiveFeign$FeignInvocationHandler -> ... -> Resilience4jClient -> your feign.Client

INVOCATION_HANDLER_FACTORY mode
  proxy -> Resilience4jInvocationHandler -> <handler returned by the delegate InvocationHandlerFactory>
```

这两个挂载点把韧性边界放在了不同的高度：`CLIENT` 装饰的是**传输调用**（`Client#execute`），因此被度量和被保护的是 Feign 一次请求/响应往返的执行；`INVOCATION_HANDLER_FACTORY` 装饰的是**代理方法调用**，于是 Feign 自己在处理器之下所做的一切（模板构建、编码/解码、它自己的重试与错误解码策略）都被包含在装饰之内，一次逻辑方法调用等于一次韧性处理。

## 兼容性

| 项目 | 值 | 核验来源 |
|------|-------|---------------|
| microsphere-resilience4j-openfeign | `0.1.0` – `0.1.4` 已发布；`${revision}` = `0.1.5-SNAPSHOT` | 模块 `pom.xml`、根 `pom.xml` |
| Resilience4j | `1.3.0`、`1.4.0`、`1.5.0`、`1.6.0`、`1.7.0`（默认 `1.7.0`） | `microsphere-resilience4j-parent/pom.xml` 中的 profiles `resilience4j-1.3` … `resilience4j-1.7` |
| Spring Cloud（仅当你把它嵌入 Spring 应用时） | Hoxton – 2021.0.x | 父工程 `microsphere-spring-cloud-parent` `0.1.26` 的 profiles |
| JDK 基线 | **8** | 1.x reactor POM 不声明 `java.version`；继承来的上级（grandparent）`microsphere-build` `0.3.14` 设置了 `<java.version>8</java.version>` 并且 `maven.compiler.source/target = ${java.version}`，而 CI 矩阵（`.github/workflows/maven-build.yml`）从 `java: '8'` 起步 |
| Feign（`feign-core`） | 由当前激活 profile 对应的 Spring Cloud BOM 管理；模块自身不锁定版本 | 模块 `pom.xml`（`<optional>true</optional>`，无 `<version>`） |
| 起始版本 | `1.0.0`（Javadoc `@since`），在 `0.1.1` 从 `feign` 更名为 `openfeign` | 源码头部注释、`release-notes.md` |

## 安装

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

`microsphere-resilience4j-openfeign` 受这份依赖管理清单（BOM，`…/microsphere-resilience4j-dependencies/pom.xml`）管理。

### 插件自身 POM 中声明的作用域——以及你必须补充什么

插件编译时所依赖的几乎所有东西都是 `<optional>true</optional>`，这意味着**它们不具备传递性**：你的应用必须自己把这些 jar 带进来。唯一非 optional 的 compile 依赖就是 commons 模块。

| 依赖（模块 `pom.xml`） | 作用域 / 可选性 | 你是否必须自行提供？ |
|-------------------------------|---------------------|----------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-commons` | compile，**非 optional**（`${revision}`） | 会传递性地带来 |
| `io.github.openfeign:feign-core` | `optional` | 是（通常已经由 OpenFeign 带进来了） |
| `io.github.microsphere-projects:microsphere-java-core` | `optional` | 是——用于 `MethodUtils.getSignature`、`StringUtils.isNotBlank`、`SymbolConstants.SPACE_CHAR` |
| `io.github.resilience4j:resilience4j-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter`、`-annotations`、`-consumer`、`-framework-common`、`-feign` | `optional` | 是——凡是你交给门面的注册中心所对应的那些模块都需要 |
| `io.github.microsphere-projects:microsphere-annotation-processor` | `optional`（构建期） | 否 |
| `org.junit.jupiter:junit-jupiter`、`microsphere-java-test`、`ch.qos.logback:logback-classic`、`io.github.openfeign:feign-gson` | `test` | 否 |

这个 POM 中**没有 `provided` 作用域**；所使用的修饰符只有 `optional` 和 `test`。

另外请注意，`resilience4j-feign` 虽有声明，但插件中的任何类都**没有使用**它——对它唯一的引用是 `Resilience4jInvocationHandler` 中的一条 `@see io.github.resilience4j.feign.DecoratorInvocationHandler` Javadoc 链接。

## API 参考

### `Resilience4jCapability`（`implements feign.Capability`）

| 成员 | 签名 |
|--------|-----------|
| 常量 | `public static final String DEFAULT_ENTRY_NAME_PREFIX = "feign@"` |
| 常量 | `public static final DecoratedPoint DEFAULT_DECORATED_POINT = CLIENT` |
| 构造器 | `Resilience4jCapability(Resilience4jFacade facade)` |
| 构造器 | `Resilience4jCapability(Resilience4jFacade facade, DecoratedPoint decoratedPoint)` |
| 构造器 | `Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix)` |
| 构造器 | `Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix, DecoratedPoint decoratedPoint)` |
| 钩子 | `@Override public Client enrich(Client client)` |
| 钩子 | `@Override public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory)` |
| 访问器 | `@Nonnull public DecoratedPoint getDecoratedPoint()` |
| 谓词 | `public boolean isDecoratedClient()` |
| 谓词 | `public boolean isDecoratedInvocationHandlerFactory()` |
| 嵌套枚举 | `public static enum DecoratedPoint { CLIENT, INVOCATION_HANDLER_FACTORY }` |

两个 `enrich` 方法都是有条件的：只有当所配置的 `DecoratedPoint` 选中它们时才会返回新的包装对象，否则原样返回入参——所以以不同挂载点把同一个能力扩展添加两次是可行的，但单个实例只会装饰恰好一处。

### `Resilience4jClient`（`implements feign.Client`）

```java
public Resilience4jClient(Client delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public Response execute(Request request, Request.Options options) throws IOException {
    String entryName = buildEntryName(request);
    return this.facade.call(entryName, () -> this.delegate.execute(request, options), IOException.class);
}
```

`execute` 保留了 `throws IOException` 契约：门面调用使用的是 `<T, TR extends Throwable> T call(String, ThrowableSupplier<T>, Class<TR>)` 这个重载，它委托给普通的 `call(String, ThrowableSupplier<T>)`，然后把任何 `Throwable` 交给 `io.microsphere.util.ExceptionUtils.wrap(t, throwableClass)`，从而传输层的 `IOException` 不会被藏在一个非受检包装器之下。Resilience4j 自己发出的拒绝信号是非受检的（`CallNotPermittedException`、`RequestNotPermitted`、`TimeoutException`），因此无论如何它们都会以运行时异常的形式到达你的调用方——如果你想把它们转换成某个响应，请安装一个回退（`Targeter`/`ErrorDecoder`/断路器 fallback）。`wrap(...)` 的确切语义位于 `microsphere-java-core` 中，在本快照之外。

### `Resilience4jInvocationHandlerFactory`（`implements feign.InvocationHandlerFactory`）

```java
public Resilience4jInvocationHandlerFactory(InvocationHandlerFactory delegate, Resilience4jFacade facade, String entryNamePrefix)

@Override
public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
    InvocationHandler delegate = this.delegate.create(target, dispatch);
    return new Resilience4jInvocationHandler(delegate, facade, entryNamePrefix);
}
```

### `Resilience4jInvocationHandler`（`implements java.lang.reflect.InvocationHandler`）

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

在这四个类中，只有它做了急切的参数校验：对 `delegate`、`facade` 和 `entryNamePrefix` 都调用了 `requireNonNull`。它的 `invoke` 声明 `throws Throwable`，并使用两参数的 `call(...)` 重载，因此不做任何包装。

### 条目名称 → Resilience4j 实例

条目名称是把一次调用与 Resilience4j 对象关联起来的*唯一*键，而两个构建器都只是在 `entryNamePrefix` 之后做纯粹的字符串拼接——**库本身不会插入任何分隔符**。

| 挂载点 | 名称的构造方式（来自源码） | 使用默认前缀 `feign@` 时的示例 |
|-----------------|---------------------------------|------------------------------------------|
| `CLIENT` | `prefix + request.httpMethod().name() + ' ' + requestTemplate.feignTarget().url() + methodMetadata.template().path()`，当 `requestTemplate.queryLine()` 非空白时再拼上它 | `feign@POST http://localhost:8080/user/{name}` |
| `INVOCATION_HANDLER_FACTORY` | `prefix + io.microsphere.reflect.MethodUtils.getSignature(method)` | `feign@<UserService#getUser(String) 的签名>` |

有两个后果需要你在设计时加以考虑：

1. `CLIENT` 路径把一份**由契约解析而来、尚未求值的**路径模板（`methodMetadata.template().path()`，所以 `{name}` 保持字面量）与一个**按请求而变**的 `queryLine()` 混在了一起。因此查询参数会成为条目名称的一部分：`feign@GET http://localhost:8080/user?name=test-user` 和 `...?name=other-user` 是两个不同的断路器。若查询串是自由形式的（页码、时间戳、排序键），这会制造出无界的注册中心条目。
2. `INVOCATION_HANDLER_FACTORY` 路径为每个接口方法产出一个稳定的、与实参无关的名称——对于按端点划分的断路器而言，这通常正是你想要的。

commons 对每个名称的解析方式是 `registry.find(name)` → 否则 `createEntry(name)`，也就是 `registry.<module>(name, getConfiguration(name))`。因为 `getConfiguration(name)` 查找的是**一个键等于条目名称的具名配置**，你可以预先以正好生成的那个条目名称注册一个 `CircuitBreakerConfig`（或限流器/重试/… 配置），从而只控制某一个端点而不触碰其他端点。其余情况都回落到该注册中心的默认配置。

## 完整可运行示例

这个示例与模块自身的测试套件保持一致：一个 `UserService` Feign 接口，它的“服务端”是一个本地的 `SimpleUserService`，由一个测试用的 `Client` 来执行，因此不需要 HTTP 服务器。请添加 `feign-core`、`feign-gson` 以及你所使用的各个 Resilience4j 模块。

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
        // 1. 注册中心决定哪些模块来装饰这次调用
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        // 2. 选择挂载点：CLIENT（默认）或 INVOCATION_HANDLER_FACTORY
        Resilience4jCapability capability =
                new Resilience4jCapability(facade, Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY);

        Encoder encoder = new GsonEncoder();
        Decoder decoder = new GsonDecoder();

        // 3. 纯粹的 Feign —— 在生产环境中请换成 feign.Client.Default 或你自己的实现
        Feign feign = Feign.builder()
                .client(new feign.Client.Default())
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)   // 需要其他能力扩展时重复调用 .addCapability(...)
                .build();

        UserService userService = feign.newInstance(new Target.HardCodedTarget<>(UserService.class, "http://localhost:8080"));

        // 现在每次调用都会被装饰在以 "feign@" 开头的条目名称之下
        System.out.println(userService.createUser("test-user").getName());
    }
}
```

或者直接运行插件自身的测试（它们不需要网络）：

```bash
./mvnw -pl microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign -am test
```

`AbstractResilience4jFeignTest` 构建客户端的方式与上面完全一致，只是它安装的是 `new DelegatingClient<>(encoder, new SimpleUserService())`——一个 `feign.Client`，它的 `execute(Request, Request.Options)` 从 `request.requestTemplate().methodMetadata().method()` 中读出 `Method`，在内存中的 `SimpleUserService` 上反射地调用它，并把返回值编码成一个状态为 `200` 的 `Response`。三个具体测试类的差别仅在于它们如何构建能力扩展：

| 测试类 | 在 `createResilience4jCapability(Resilience4jFacade)` 中构建的能力扩展 | 效果 |
|------------|-----------------------------------------------------------------------|--------|
| `Resilience4jCapabilityTest` | `new Resilience4jCapability(facade)` | 默认值：前缀 `feign@`、`CLIENT`；断言 `DEFAULT_ENTRY_NAME_PREFIX`/`DEFAULT_DECORATED_POINT` |
| `Resilience4jClientTest` | `new Resilience4jCapability(facade, this.getClass().getSimpleName())` | 带自定义前缀（`Resilience4jClientTest…`）的 `CLIENT` |
| `Resilience4jInvocationHandlerFactoryTest` | `new Resilience4jCapability(facade, INVOCATION_HANDLER_FACTORY)` | 处理器工厂模式，默认前缀 |

如果你想在自己的测试里证明这处挂载，`testProxy()` 就是应当抄用的那条断言：在 `INVOCATION_HANDLER_FACTORY` 模式下，代理处理器是一个 `Resilience4jInvocationHandler`，它的 `getDelegate()` 是先前添加的那个能力扩展的处理器，而 `getFacade()` 返回你的门面实例；在 `CLIENT` 模式下代理处理器不受改动（`Resilience4jCapability#isDecoratedClient()` 为 `true`，因此那段断言被跳过）。随后 `testUserService()` 会针对 `SimpleUserService` 往返调用 `createUser("test-user")` / `getUser("test-user")`。

## 在 Spring Cloud OpenFeign 应用中注册

本模块不提供任何 Spring 集成。在一个 Spring Cloud OpenFeign 应用中，自然的做法是把能力扩展作为一个 bean 暴露到 Feign 客户端上下文里：

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

> 本快照中未经核验：你所用的 Spring Cloud OpenFeign 版本是否会从客户端上下文中收集 `Capability` bean，以及是否存在一个 `Resilience4jFacade` bean——这两者都是 Spring Cloud OpenFeign / `microsphere-resilience4j-spring*` 模块的行为，而不是本插件的行为。本仓库源码所能证明的唯一接线方式只有 `Feign.Builder#addCapability(Capability)`。如果你需要由 Spring 管理的注册中心，请参见 [microsphere-resilience4j-spring.md](microsphere-resilience4j-spring.md)。

## 行为说明

| 主题 | 行为（来自源码） |
|-------|--------------------------|
| 状态 / 线程安全 | 四个类都只保留 `final` 字段（`delegate`、`facade`、`entryNamePrefix`、`decoratedPoint`），没有任何可变状态；同一个能力扩展实例可以被许多 `Feign` 实例和许多线程共享。 |
| 名称计算开销 | 两个装饰器都不缓存条目名称。`Resilience4jClient` 在**每一次**调用上都用 `StringBuilder` 重建名称；`Resilience4jInvocationHandler` 每次调用都执行 `MethodUtils.getSignature(method)`。 |
| 装饰是互斥的 | 一个 `Resilience4jCapability` 要么装饰 `Client`，要么装饰 `InvocationHandlerFactory`，绝不会两者都装饰。用不同挂载点添加两个实例会对同一次逻辑调用双重装饰（每次调用产生两次门面 `call(...)`）——请避免这样做。 |
| 门面是唯一的可变点 | 插件不从 Resilience4j 自身解析任何东西：注册中心、模块集合与顺序都来自你传入的 `Resilience4jFacade`。`ChainableResilience4jFacade(Registry...)` 为每个注册中心创建一个模板并按优先级排序（重试 → 断路器 → 限流器 → 超时限制器 → 舱壁隔离），而两个装饰器都使用**一次性 `call(...)` 路径**，因此你注册的所有模块——包括重试、超时限制器和线程池舱壁隔离——都会参与（这与 Spring Web 插件所使用的两阶段路径不同）。 |
| 多个能力扩展时的包装顺序 | `Feign.Builder` 按插入顺序折叠能力扩展列表，因此**最后添加的能力扩展是最外层的包装**。在测试中，`DelegatingCapability` 在 `Resilience4jCapability` 之前添加，于是落在它的内侧。 |
| `Target` / `MethodHandler` 入参 | `Resilience4jInvocationHandlerFactory#create` 把 `target` 和 `dispatch` 原样传给被委托的工厂；它并不读取它们，所以在该模式下条目名称中永远不会出现目标 URL（只有在 `CLIENT` 模式下 `feignTarget().url()` 才会出现）。 |
| 空值处理 | 只有 `Resilience4jInvocationHandler` 校验自己的构造参数；`Resilience4jClient`、`Resilience4jInvocationHandlerFactory` 和 `Resilience4jCapability` 会静默接受 `null` 并在之后失败（NPE）——请正确地构造它们。 |
| 日志 | 插件自身没有任何日志；commons 的门面会在每次 `call(...)` 时记录 `trace` 级别消息。 |
