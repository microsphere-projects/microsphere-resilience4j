# microsphere-resilience4j-spring-web — 用户指南（1.x）

> 版本线 `0.1.x`（`release-1.x` 分支） · 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0**（默认 `1.7.0`） · Spring Cloud **Hoxton – 2021.0.x**（父工程 `microsphere-spring-cloud-parent` `0.1.26`） · JDK **8** 基线 · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-spring-web.md)

仓库中的模块路径：`microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web`。
Java 包：`io.microsphere.resilience4j.spring.web`。

## 这个插件做什么

它让每一个 Spring MVC **处理器方法**都成为一个受 resilience4j 保护的条目，既不需要在控制器上加注解，也不需要编写装饰代码。插件被激活后，会注册一个 `Resilience4jHandlerMethodInterceptor` bean；它在处理器方法运行之前开启一个 Resilience4j 上下文，并在之后关闭它，于是控制器的执行会被你的 `Resilience4jFacade` 中配置的那些模块所度量和守护（断路器、舱壁隔离、限流器——见*哪些模块会生效*）。

## 它挂载在哪里

| 层 | 本插件所使用的钩子 | 具体类型 |
|-------|--------------------------|---------------|
| 插件发现 | `io.microsphere.resilience4j.spring.common.Resilience4jPlugin`（SPI，通过 `META-INF/spring.factories` 发布） | `SpringWebResilience4jPlugin` |
| bean 注册 | `Resilience4jPlugin#plugin(...)` 内部的 `BeanDefinitionRegistry` | 注册 bean 名称 `resilience4jHandlerMethodInterceptor` |
| 请求拦截 | `io.microsphere.spring.web.method.support.HandlerMethodInterceptor` — `beforeExecute(HandlerMethod, Object[], NativeWebRequest)` / `afterExecute(HandlerMethod, Object[], Object, Throwable, NativeWebRequest)`（即 `microsphere-spring-web` 的处理器方法扩展链，由 `@EnableWebMvcExtension` 激活） | `Resilience4jHandlerMethodInterceptor` |
| 端点元数据 | `ApplicationListener<io.microsphere.spring.web.event.WebEndpointMappingsReadyEvent>` | 同一个类——预先计算条目名称并预热 commons 的条目缓存 |
| 顺序 | `org.springframework.core.Ordered` | 同一个类（`getOrder()` / `setOrder(int)`，默认 `0`） |

拦截的边界是**处理器方法**，而不是 servlet 请求：这一对回调是在控制器方法（一个 Spring 的 `HandlerMethod`）被选定之后触发的，此时它的实参已经解析完毕，返回值 / 错误也都可以取到。因此它比一个 `Filter` 更窄，又比 `HandlerInterceptor#preHandle` 更丰富——而且它永远看不到那些没有映射到处理器方法的请求。

## 兼容性

| 项目 | 值 | 核验来源 |
|------|-------|---------------|
| microsphere-resilience4j-spring-web | `0.1.0` – `0.1.4`；`${revision}` = `0.1.5-SNAPSHOT` | 模块 `pom.xml`、根 `pom.xml` |
| Resilience4j | `1.3.0`、`1.4.0`、`1.5.0`、`1.6.0`、`1.7.0`（默认 `1.7.0`，profile `resilience4j-1.7` 是 `activeByDefault`） | `microsphere-resilience4j-parent/pom.xml` |
| Spring profiles → 技术栈 | `spring-cloud-hoxton` → Spring Cloud `Hoxton.SR12` / Spring Boot `2.3.12.RELEASE`；`spring-cloud-2020` → `2020.0.5` / `2.4.13`；`spring-cloud-2021`（默认） → `2021.0.9` / `2.6.15` | 所继承的 `microsphere-spring-cloud-parent` `0.1.26` POM 的 profiles |
| Servlet API | `javax.servlet`（JSR-340 那一代，本模块的 test 作用域中使用 `javax.servlet-api`） | 模块 `pom.xml`、`WebEndpointMappingsReadyEvent`/`NativeWebRequest` 的用法 |
| JDK 基线 | **8** — 1.x reactor POM 不声明 `java.version`；所继承的上级（grandparent）`microsphere-build` `0.3.14` 设置了 `<java.version>8</java.version>`（`maven.compiler.source/target = ${java.version}`），而 `.github/workflows/maven-build.yml` 从 `java: '8'` 起开始测试 | 这些 POM 以及该 workflow 文件 |
| `@since` | `1.0.0` | 源码头部注释 |

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
        <artifactId>microsphere-resilience4j-spring-web</artifactId>
    </dependency>
</dependencies>
```

### 插件自身 POM 中声明的作用域

| 依赖 | 作用域 / 可选性 | 通常由谁来提供 |
|------------|---------------------|--------------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile，**非 optional**（`${revision}`） | 传递性带来——`Resilience4jPlugin` SPI、`@Enable*` 注解以及门面 bean 都出自这里 |
| `io.github.microsphere-projects:microsphere-spring-context` | `optional` | 在一个 microsphere-spring 应用中通常已经存在 |
| `io.github.microsphere-projects:microsphere-spring-web` | `optional` | **你必须自己添加**——它提供 `HandlerMethodInterceptor`、`WebEndpointMapping`、`WebEndpointMappingsReadyEvent`、`WebScope` |
| `io.github.microsphere-projects:microsphere-spring-webmvc` | 在*测试*代码块中以 **`test` 作用域**声明 | 运行时你必须自己添加它——它承载 `@EnableWebMvcExtension` |
| `org.springframework:spring-web` | `optional` | 你的 Spring Boot/Cloud BOM |
| `org.springframework:spring-webmvc` | `optional`（**不是** test 作用域，尽管它就坐在 “Testing” 注释之下） | 你的 Spring Boot/Cloud BOM |
| `io.github.resilience4j:resilience4j-spring` | `optional` | 需要——`@Enable*` 注解会绑定 Resilience4j 1.x 的配置属性 |
| `io.github.microsphere-projects:microsphere-annotation-processor` | `optional`（构建期） | 否 |
| 拦截器源码所使用的 `io.microsphere.*` 工具（`logging.Logger`/`LoggerFactory`、`util.Assert`、`lang.Wrapper`、`collection.MapUtils`、`annotation.Nonnull`） | **这个 POM 里根本没有声明** | 你的应用已经携带的 Microsphere core/logging 构件；`microsphere-java-core` 在 `microsphere-resilience4j-commons` 中是 `optional` 的，所以它*不是*传递性的。缺少提供方会表现为某个 `io.microsphere.*` 类的 `NoClassDefFoundError`——请显式添加它。究竟是哪一个构件，这些 POM 并没有把它锁定（未核验）。 |
| `junit-jupiter`、`spring-test`、`spring-aop`、`microsphere-spring-test`、`microsphere-spring-webmvc`、`javax.servlet-api`、`jackson-databind`、`aspectjweaver`、`jsonassert`、`hamcrest`、`logback-classic` | `test` | 否 |

这个 POM 中没有任何依赖使用 `provided` 作用域；插件所依赖的机制是 `optional`（非传递性），再加上 `microsphere-resilience4j-spring` 这条 compile 依赖。

## 注册 / 配置步骤

1. 添加上面的依赖，再加上 `microsphere-spring-web` 和 `microsphere-spring-webmvc`（二者在上游都被标记为 optional）。
2. 启用 Web MVC 扩展，处理器方法拦截才会存在：`@EnableWebMvcExtension`（放在一个 `@Configuration` 类上，或者像模块测试那样直接放在你的测试类上）。
3. 至少启用一个 Resilience4j 模块，**并在其 `plugins` 属性中按名称列出本插件**。`plugins` 是 `EnableResilience4j.plugins` 的一个 `@AliasFor`，它与 `Resilience4jPlugin#getName()` 相匹配，而这里该值就是常量 `SpringWebResilience4jPlugin.PLUGIN_NAME = "spring-web"`。如果列表中没有这个名字，`SpringWebResilience4jPlugin#plugin(...)` 就永远不会被调用，也就不会有拦截器 bean 存在。
4. （可选）如果你需要在其他处理器方法拦截器之前 / 之后运行，就对 `resilience4jHandlerMethodInterceptor` bean 调用 `setOrder(int)`。

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

## 完整可运行示例（模块自身的测试）

`SpringWebResilience4jPluginTest` 就是那份参照配置——一个注解驱动、非 Boot 的 Spring MVC 测试，它通过 `MockMvc` 来驱动本插件：

```java
@SpringJUnitConfig(classes = {
        SpringWebResilience4jPluginTest.class,   // 配置类自身
        TestController.class                     // 来自 microsphere-spring-test
})
@WebAppConfiguration
@EnableWebMvc
@EnableWebMvcExtension
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)   // 测试中是静态导入进来的 PLUGIN_NAME
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

这证明了：对 `/test/user` 的 POST 由 `TestController` 处理（它把 `User` 负载原样以 JSON 回显），舱壁隔离通过 `@EnableBulkhead` 以默认配置装配完成，而在本插件生效的情况下，韧性装饰被应用到了那个处理器方法上。运行方式：

```bash
./mvnw -pl microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web -am test
```

## API 参考

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

`beanFactory` 与 `environment` 这两个参数被忽略：这里的注册只涉及一个 bean **定义**（`registerBeanDefinition`，来自 `io.microsphere.spring.beans.factory.support.BeanRegistrar`），既不创建实例，也不读取任何属性。

### `Resilience4jHandlerMethodInterceptor`

声明为：

```java
public class Resilience4jHandlerMethodInterceptor implements HandlerMethodInterceptor,
        ApplicationListener<WebEndpointMappingsReadyEvent>, Ordered
```

| 成员 | 签名 / 行为 |
|--------|---------------------|
| 常量 | `public static final String BEAN_NAME = "resilience4jHandlerMethodInterceptor"` |
| 构造器 | `public Resilience4jHandlerMethodInterceptor(Resilience4jFacade resilience4jFacade)` — 通过静态导入的 `io.microsphere.util.Assert.assertNotNull` 断言非 null（那是 Microsphere 自己的 `Assert`，不是 Spring 的） |
| `@Override` | `public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request)` → `facade.begin(getEntryName(handlerMethod))`，结果作为一个名为 `Resilience4jContext.class.getName()` 的请求属性存放起来 |
| `@Override` | `public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request)` → 移除该属性；若为 `null` 就记录一条 `trace` 并返回；否则 `context.setResult(returnValue).setFailure(error)`，然后 `facade.end(context)` |
| `@Override` | `public void onApplicationEvent(WebEndpointMappingsReadyEvent event)` → `initEntryCache(event)` |
| `@Override` | `public int getOrder()` / `public void setOrder(int order)`（字段默认值 `0`） |
| 访问器 | `@Nonnull public final Resilience4jFacade getResilience4jFacade()` |
| 命名 | `public String getEntryName(HandlerMethod handlerMethod)` → `methodEntryNamesCache.computeIfAbsent(method, m -> PLUGIN_NAME + "@" + handlerMethod)` |
| 覆盖点 | `protected void initEntryCache(WebEndpointMappingsReadyEvent event)` |

Javadoc 把该类称作 “the abstract template class for Resilience4j's `HandlerMethodInterceptor`”，但它其实是**具体类**，由插件直接实例化；`initEntryCache` 是唯一那个 protected 扩展点。

### 条目名称以及它们如何绑定到 Resilience4j 实例

```text
spring-web@ + handlerMethod.toString()
```

`PLUGIN_NAME + "@" + handlerMethod` 就是源码中的拼接，所以尾部正是 Spring 的 `HandlerMethod#toString()` 针对那个控制器方法所渲染出来的内容（渲染本身属于 Spring，这里不予复现——把整个键当作一个不透明但确定的值来对待即可）。缓存的键是 `Method`，因此名称是按处理器方法划分的，跨请求稳定，且与实参无关。commons 的解析方式是 `registry.find(name)` → 否则 `registry.<module>(name, getConfiguration(name))`，而 `getConfiguration(name)` 查找的是**一个名称等于条目名称的具名配置**，所以你可以通过在那个精确的键下注册一份配置来锁定某一个端点；任何匹配不上的都使用注册中心的默认配置。`initEntryCache` 还会把所有预先计算好的名称推进每个模板的本地缓存（`Resilience4jTemplate#initLocalEntriesCache(names)`），这正是让那些由 `HashMap` 支撑的缓存在运行期保持安全的设计用法。

## 哪些模块会生效（本插件的一条硬性限制）

本插件使用**两阶段**的 `begin(...)` / `end(...)` 操作，而不是一次性的 `call(...)`。在 1.x commons 这条版本线上，有三个模板报告 `isBeginSupported() == false` 且 `isEndSupported() == false`，因而会被悄无声息地跳过：

| 模块 | 是否参与处理器方法的韧性处理？ | 所执行的动作（1.x commons 源码） |
|--------|--------------------------------------------|----------------------------------------|
| `CircuitBreaker` | 是 | `begin`：先取 `nanoTime()`，然后 `circuitBreaker.acquirePermission()`，开始时间存储在上下文上；`end`：`onSuccess(duration, NANOSECONDS)` 或 `onError(duration, NANOSECONDS, failure)` |
| `Bulkhead` | 是 | `begin`：`bulkhead.acquirePermission()`；`end`：`bulkhead.onComplete()` |
| `RateLimiter` | 是 | `doBegin` **和** `doEnd` 都会调用 `RateLimiter.waitForPermission(rateLimiter)` |
| `Retry` | **否** | 跳过——重试需要整个调用都落在一个回调之内 |
| `TimeLimiter` | **否** | 跳过 |
| `ThreadPoolBulkhead` | **否** | 跳过 |

如果你需要对一个控制器做重试或超时限制，请在另一层去装饰这次调用（[openfeign](microsphere-resilience4j-openfeign.md) 插件的一次性 `call(...)` 路径，或者直接使用 `Resilience4jOperations#execute/call`）。

## 行为说明

| 主题 | 行为（来自源码） |
|-------|--------------------------|
| 拒绝以异常形式浮现 | `beforeExecute` 在你的控制器方法*之前*运行；`acquirePermission()` 会抛出 `CallNotPermittedException` / `RequestNotPermitted`（限流则是阻塞，等待许可）。本插件**没有 fallback 钩子**——异常会传播进 Spring MVC 的分发过程，并变成一个错误响应。 |
| 许可释放依赖 `afterExecute` | 舱壁隔离的许可只会由 `afterExecute` 内部的 `bulkhead.onComplete()` 释放。因为上下文是作为一个请求属性（`WebScope.REQUEST`）携带的，所以这一对回调必须针对同一个请求都执行到位。 |
| 只做了一半的 `begin` 不会被补偿 | `ChainableResilience4jFacade#begin` 一次一个模板地创建子上下文；如果靠后的某个模板拒绝了，就不会返回复合上下文，于是 `beforeExecute` 什么都不存，`afterExecute` 记录 “The 'context' is null …” 然后返回——较早那些模板的 `end` 阶段（包括 `onComplete()`）永远走不到。在这条路径上请保持模块集合小而可预期。 |
| 限流器每个请求消耗两个许可 | 已在 1.x commons 中核验：`RateLimiterTemplate.doBegin` 与 `doEnd` 都调用 `waitForPermission(rateLimiter)`。请据此设定你的 `limitForPeriod`（大约是每个处理器方法目标 QPS 的两倍）。 |
| 线程安全 | 该拦截器不保留任何按请求而变的状态（上下文住在请求属性里），而它的可变字段只有 `order`（一个普通 `int`，只在配置期间被写入）和 `methodEntryNamesCache`。该缓存是在 `WebEndpointMappingsReadyEvent` 期间创建并填充的；一个不在那些映射之内的处理器方法，会在请求时由 `computeIfAbsent` 懒式补入，而这是对那个 map 的并发改动。请尽量让所有端点都被该事件发现，并在上下文刷新结束之前设定好 `order`。 |
| 名称缓存必须初始化 | `getEntryName` 会直接解引用 `methodEntryNamesCache`；如果 `WebEndpointMappingsReadyEvent` 从未到达这个监听器（例如在你的配置中 Web 扩展并不发布事件），那么第一次处理器调用就会抛出 `NullPointerException`。测试中的 `@EnableWebMvcExtension` 正是让这个事件流转起来的东西；插件自身并不发布它。 |
| 装饰粒度仅限处理器方法 | 被 `@Override` 的回调收到的是一个 `HandlerMethod`；静态资源、`ResourceHttpRequestHandler` 以及非 `HandlerMethod` 的端点不会被装饰。 |
| 日志 | 只有 `trace` 级别，经由 `io.microsphere.logging`（`LoggerFactory.getLogger(...)`），出现在 null 上下文那条路径上以及每一个所创建的条目名称上。 |

## 另见

- 开发者侧的分析（SPI 接线、拦截器链内部、依赖图、时序图）：[../../developer-guide/zh/microsphere-resilience4j-spring-web.md](../../developer-guide/zh/microsphere-resilience4j-spring-web.md)
- 本插件所依赖的 `@Enable*` 注解与门面 bean：[microsphere-resilience4j-spring.md](microsphere-resilience4j-spring.md)
