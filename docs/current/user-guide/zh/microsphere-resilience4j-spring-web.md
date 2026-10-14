# microsphere-resilience4j-spring-web — 用户指南

> 简体中文 | [English](../microsphere-resilience4j-spring-web.md)

> 版本线 `0.2.x`（`release` 分支）· 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`）· Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 用户指南索引](README.md)

## 本插件的作用

`microsphere-resilience4j-spring-web` 借助 `microsphere-spring-web` 的处理器方法拦截扩展点，为你的 **Spring MVC / WebFlux 处理器方法**（即你的 `@RequestMapping` 控制器方法）加上 Resilience4j 装饰。

| 类 | 角色 |
|-------|------|
| `io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin` | 名为 `spring-web` 的 `Resilience4jPlugin`；当该插件被列在某个 `@Enable*` 注解中时，注册拦截器的 bean definition |
| `io.microsphere.resilience4j.spring.web.Resilience4jHandlerMethodInterceptor` | `io.microsphere.spring.web.method.support.HandlerMethodInterceptor` + `ApplicationListener<WebEndpointMappingsReadyEvent>` + `Ordered`；在处理器方法执行的外围开启/关闭 Resilience4j 装饰 |

钩挂位置：`microsphere-spring-web(mvc)` 用 `InterceptingHandlerMethodProcessor` 替换了 Spring MVC 的参数解析步骤和返回值处理步骤，由它通知所有 `HandlerMethodAdvice` bean。而 `DelegatingHandlerMethodAdvice` bean 又把这些通知扇出给所有排序后的 `HandlerMethodInterceptor` bean——本插件的 `resilience4jHandlerMethodInterceptor` 就是其中之一。`beforeExecute` 在**最后一个处理器方法参数解析完成之后**运行（也就是紧挨着你的控制器方法被调用之前），`afterExecute` 在**返回值处理完毕之后**运行（或者当派发失败时由 `afterCompletion` 触发）。

这是唯一使用**两阶段** `Resilience4jFacade.begin(...)` / `end(context)` 操作的插件，因为被保护的调用起始与结束于两个不同的框架回调。

## Maven 坐标与依赖装配

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-spring-web</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

从 `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/pom.xml` 看，`microsphere-resilience4j-spring` 是唯一的非 optional 依赖：

| 依赖 | 在插件 POM 中的作用域 | 说明 |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile | 带入 `microsphere-resilience4j-commons` 和 `Resilience4jPlugin` SPI |
| `io.github.microsphere-projects:microsphere-spring-web` | **optional** | 提供 `HandlerMethodInterceptor`、`WebEndpointMapping`、`WebEndpointMappingsReadyEvent`、`WebScope`——**运行期必需** |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | 提供插件使用的 `BeanRegistrar.registerBeanDefinition(...)` |
| `org.springframework:spring-web` | **optional** | `HandlerMethod`、`NativeWebRequest` |
| `org.springframework:spring-webmvc` | **optional（compile 级别）** | 仅 servlet 栈需要；WebFlux 用户需要改用 `spring-webflux` |
| `io.github.resilience4j:resilience4j-spring` | **optional** | 提供 `@Enable*` 注解所绑定的 `*Configuration` / `*Registry` 以及 `*ConfigurationProperties` 类型 |
| `microsphere-spring-webmvc`、`spring-test`、`spring-aop`、`microsphere-spring-test`、`jakarta.servlet-api`、`jackson-databind`、`aspectjweaver`、`jsonassert`、`hamcrest`、`junit-jupiter`、`logback-classic` | **test** | `SpringWebResilience4jPluginTest` 使用 |

因此你必须自行添加（至少）`microsphere-spring-web`、`microsphere-spring-webmvc`（或 `microsphere-spring-webflux`）和 `resilience4j-spring`；在本 reactor 内构建时版本来自导入的 `microsphere-spring-cloud-dependencies` / `microsphere-spring-dependencies` BOM，否则请显式锁定版本。

## 版本与兼容性

| 条目 | 要求 | 说明 |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x`（`release`） | 构件 `microsphere-resilience4j-spring-web` |
| Resilience4j | 2.0.0 – 2.4.0 | 默认 profile `resilience4j-2.4` → `2.4.0` |
| Spring Framework / Boot | 由应用提供 | 本仓库使用的 `microsphere-spring-cloud-dependencies` 版本线覆盖 Spring Cloud 2022.0.x（Boot 3.0.x）直到 2025.0.x（Boot 3.5.x），因此假定技术栈为 `jakarta.servlet.*`（Boot 3+） |
| microsphere-spring-web(-mvc/-webflux) | 必需 | `@EnableWebMvcExtension` / `@EnableWebFluxExtension` 以及拦截处理器都在其中 |
| JDK | 17+ | 根 POM `java.version = 17` |

## 配置

插件需要三样东西：一个已启用的 Resilience4j 模块、该注解中的插件名，以及开启处理器方法拦截。

```java
@Configuration
@EnableWebMvc                      // plain Spring MVC
@EnableWebMvcExtension             // microsphere-spring-webmvc: interception + endpoint mappings
@EnableBulkhead(plugins = SpringWebResilience4jPlugin.PLUGIN_NAME)  // "spring-web"
@EnableCircuitBreaker
class WebResilience4jConfig {
}
```

逐步发生了什么：

1. `@EnableBulkhead`（与 `microsphere-resilience4j-spring` 中的每个 `@Enable*` 一样）触发 `EnableResilience4jRegistrar`，它调用 `registerPlugins(...)`：`Resilience4jPlugin` 实现从 `META-INF/spring.factories` 加载，对其中 `getName()` 出现在注解 `plugins` 属性里的每一个，执行 `plugin(beanFactory, registry, environment)`。
2. `SpringWebResilience4jPlugin.plugin(...)` 为拦截器类注册一个名为 `resilience4jHandlerMethodInterceptor`（`Resilience4jHandlerMethodInterceptor.BEAN_NAME`）的 bean definition——没有声明任何构造参数，因此 Spring 会用容器的 `Resilience4jFacade` bean 来满足唯一的 `Resilience4jFacade` 构造器。
3. 同一个 registrar 随后注册懒门面本身：`lazyResilience4jFacade`（一个 `LazyResilience4jFacade`，标记为 **primary**），其 `modules` 属性会为每个被启用的 `@Enable*` 模块累加一个条目。它的被委托对象 `ChainableResilience4jFacade` 在 `ContextRefreshedEvent` 时由上下文中的 `*Registry` bean 装配而成（`context.getBean(registryClass)`）。
4. `@EnableWebMvcExtension` 注册 `InterceptingHandlerMethodProcessor` / `DelegatingHandlerMethodAdvice`，发布 `WebEndpointMappingsReadyEvent`，并默认设置 `interceptHandlerMethods = true`。

> 插件名是与 `Resilience4jPlugin#getName()` 比对的（`EnableResilience4jRegistrar#registerPlugins`），尽管注解的 Javadoc 把该属性称为“the Spring Bean names of the plugins”（插件的 Spring Bean 名）。

### 手工注册（不使用插件机制）

```java
@Bean
public Resilience4jHandlerMethodInterceptor resilience4jHandlerMethodInterceptor(Resilience4jFacade facade) {
    Resilience4jHandlerMethodInterceptor interceptor = new Resilience4jHandlerMethodInterceptor(facade);
    interceptor.setOrder(0);
    return interceptor;
}
```

任何 `Resilience4jFacade` 都可以，只要在第一个请求到达前它已就绪；在 Spring 之外它是 `ChainableResilience4jFacade`。

## 公共 API

### `SpringWebResilience4jPlugin`

| 成员 | 行为（源自代码） |
|--------|--------------------------|
| `public static final String PLUGIN_NAME = "spring-web"` | `getName()` 也返回该值 |
| `plugin(ConfigurableListableBeanFactory, BeanDefinitionRegistry, ConfigurableEnvironment)` | 忽略 bean factory 和 environment，调用 `registerBeanDefinition(registry, BEAN_NAME, Resilience4jHandlerMethodInterceptor.class)` |
| `getName()` | `PLUGIN_NAME` |

### `Resilience4jHandlerMethodInterceptor`

| 成员 | 行为（源自代码） |
|--------|--------------------------|
| `public static final String BEAN_NAME = "resilience4jHandlerMethodInterceptor"` | 插件所使用的 bean 名 |
| `Resilience4jHandlerMethodInterceptor(Resilience4jFacade)` | `assertNotNull(resilience4jFacade, () -> "The 'resilience4jFacade' argument can't be null")` |
| `beforeExecute(HandlerMethod, Object[], NativeWebRequest)` | 调用 `facade.begin(getEntryName(handlerMethod))`，并把返回的 `Resilience4jContext<Resilience4jContext[]>` 以属性名 `Resilience4jContext.class.getName()` 存入**请求作用域**（`WebScope.REQUEST`） |
| `afterExecute(HandlerMethod, Object[], Object returnValue, Throwable error, NativeWebRequest)` | `WebScope.REQUEST.removeAttribute(...)`；若什么都没取到，则记录 `trace` 日志 *“The 'context' is null , please check whether the 'resilience4jFacade' is a 'ChainableResilience4jFacade'”* 并返回；否则 `context.setResult(returnValue).setFailure(error)`，然后 `facade.end(context)` |
| `getEntryName(HandlerMethod)` | `methodEntryNamesCache.computeIfAbsent(method, m -> PLUGIN_NAME + "@" + handlerMethod)` — 按 `java.lang.reflect.Method` 缓存 |
| `onApplicationEvent(WebEndpointMappingsReadyEvent)` → `protected initEntryCache(event)` | 构建名字缓存（`newFixedHashMap(mappings.size())`），为每个 `getEndpoint()` 是 `HandlerMethod` 的端点预先计算名字，然后——如果门面能解包为 `ChainableResilience4jFacade`（`Wrapper.tryUnwrap`）——对每个模板调用 `template.initLocalEntriesCache(names)` |
| `getResilience4jFacade()` | `@Nonnull final` 访问器 |
| `getOrder()` / `setOrder(int)` | `Ordered`；容器用它对所有 `HandlerMethodInterceptor` bean 排序 |

### Resilience4j 入口名

```java
PLUGIN_NAME + "@" + handlerMethod   // "spring-web@" + HandlerMethod#toString()
```

`HandlerMethod#toString()` 会渲染底层的 `java.lang.reflect.Method`，因此名字形如：

```
spring-web@public org.springframework.http.ResponseEntity com.acme.UserController.get(java.lang.Long)
```

由此带来两点后果：

- 名字中包含**空格、点和圆括号**。在 YAML/properties 的 key 中要给它加引号，例如 `"spring-web@public org.springframework.http.ResponseEntity com.acme.UserController.get(java.lang.Long)": …`。在编写配置之前，先在 `trace` 级别打印一个名字看看（`io.microsphere.resilience4j.spring.web.Resilience4jHandlerMethodInterceptor` 在启动期间会记录 `Create the entryName : '{}' for HandlerMethod : {}`）。
- 缓存的 key 是 `java.lang.reflect.Method`，而被缓存的值内嵌了 `HandlerMethod#toString()`。因此派发到同一个方法的两个 `@RequestMapping` 路径共享**同一个**入口；而由某个控制器 bean 声明的方法与另一个 bean 使用的继承副本，会得到两个不同的 `Method` key。

### 为单个入口选择 Resilience4j 配置

`Resilience4jTemplate` 用 `registry.find(name)` 解析入口，未命中时走 `createEntry(name)` → `registry.<module>(name, getConfiguration(name))`，其中 `getConfiguration(name)` 会向注册表请求**key 等于入口名的那个命名配置**，若没有则回退到 `registry.getDefaultConfig()`。有两种控制方式：

1. 通过 `@Enable*` 绑定的属性：每个注解都把 Resilience4j 自身的配置属性类型绑定到 `microsphere.resilience4j.<module>.` 前缀下——例如 `@EnableBulkhead` 把 `io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties` 绑定到 `microsphere.resilience4j.bulkhead.`，把 `CommonThreadPoolBulkheadConfigurationProperties` 绑定到 `microsphere.resilience4j.thread-pool-bulkhead.`（`@EnableCircuitBreaker` → `microsphere.resilience4j.circuit-breaker.`、`@EnableRateLimiter` → `microsphere.resilience4j.rate-limiter.`、`@EnableRetry` → `microsphere.resilience4j.retry.`、`@EnableTimeLimiter` → `microsphere.resilience4j.time-limiter.`）。每个前缀之下的 key 结构是 Resilience4j 自己的（`configs` / `instances` map），所以请对照你所使用的 Resilience4j 版本核对字段名：

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

2. 以编程方式，在注册表 bean 上：在该处理器方法的第一次请求**之前**调用 `registry.addConfiguration(entryName, config)`。

## 完整可运行示例

改写自 `SpringWebResilience4jPluginTest`，本模块唯一的测试。上游测试在一个 `@SpringJUnitConfig` + `@WebAppConfiguration` 上下文中使用同样的注解，并用 `MockMvc` 驱动控制器；控制器本身（`io.microsphere.spring.test.web.controller.TestController`，来自 `microsphere-spring-test`）是一个 test 作用域 bean，它对 `POST /test/user` 返回提交的 `User`。

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

你自己的控制器只需要是一个普通的 `@Controller`/`@RestController`：

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

每个映射到该方法的请求都在 `spring-web@public …UserController.createUser(…)` 之下被装饰；隔离舱限制并发执行数，熔断器则统计整个处理器方法执行（参数解析、方法调用、返回值处理）的成功/失败。

## 行为说明与注意事项

- **这里只有支持两阶段的模块会生效。** `begin(name)` / `end(context)` 会逐个询问模板的 `isBeginSupported()` / `isEndSupported()`；`RetryTemplate`、`TimeLimiterTemplate` 和 `ThreadPoolBulkheadTemplate` 回答 `false`，因而被**静默跳过**。`CircuitBreakerTemplate`（获取许可，然后 `onSuccess`/`onResult`/`onError`）、`BulkheadTemplate` 和 `RateLimiterTemplate` 支持这一对操作。如果你需要重试或超时装饰，请使用 [OpenFeign](microsphere-resilience4j-openfeign.md)、[MyBatis](microsphere-resilience4j-mybatis.md) 或 [Druid](microsphere-resilience4j-alibaba-druid.md) 插件。
- **被拒绝的请求不会被吞掉。** 三个支持 begin 的模板各自在 `begin(...)` 中拒绝：`CircuitBreakerTemplate` 调用 `CircuitBreaker#acquirePermission()` → `CallNotPermittedException`；`BulkheadTemplate` 调用 `Bulkhead#acquirePermission()` → `RequestNotPermitted`；`RateLimiterTemplate` 调用静态辅助方法 `RateLimiter#waitForPermission(...)`，它会先**阻塞**等待限流器的 `timeoutDuration`，然后抛出 `RequestNotPermitted`（线程被中断时抛出 `io.github.resilience4j.core.exception.AcquirePermissionCancelledException`）。异常从 `beforeExecute` 抛出，因此 Spring MVC 会把它变成 500，除非你做了映射（`@ExceptionHandler`、`HandlerExceptionResolver`，或 Spring Boot 的 `ErrorController`）。
- **没有 context 就不会执行 `end(...)`。** 如果 `beforeExecute` 没有存入它的请求属性——因为请求没有经过 `InterceptingHandlerMethodProcessor`，或者属性已被清除——`afterExecute` 只会记一条 `trace` 日志然后返回。后果按模块各不相同：`BulkheadTemplate.doEnd`（`bulkhead.onComplete()`）被跳过，于是该 `Bulkhead` 实例上已获取的**并发许可会泄漏**；`CircuitBreakerTemplate.doEnd` 和 `RateLimiterTemplate.doEnd` 同样被跳过，于是这次调用永远不会被记为成功或失败（`onError` 也就无法让熔断器跳闸）。异步请求是最常见的原因；请确认拦截已启用，并且请求在初始派发和异步派发中都经过处理器方法处理器。
- **入口名缓存由事件填充。** `methodEntryNamesCache` 只在 `initEntryCache(WebEndpointMappingsReadyEvent)` 中被赋值；`getEntryName(...)` 在其上调用 `computeIfAbsent`。如果该事件从未发布（缺少 `@EnableWebMvcExtension` / `@EnableWebFluxExtension` 机制），`beforeExecute` 会在第一个请求时因 `NullPointerException` 失败，而不是优雅降级。
- **`@Order` 有影响。** 该拦截器只是可能存在多个 `HandlerMethodInterceptor` bean 中的一个，而 advice 循环按排序后的顺序执行它们；如果其他拦截器可能在你装饰结束之前抛异常，请把它放得靠前一些（`Ordered.HIGHEST_PRECEDENCE`）。
- **覆盖范围是按处理器方法而非按 URL。** 通过不同映射到达的同一个方法（多个 `@RequestMapping` 路径、类上 + 方法上的 `@RequestMapping`）只产生**一个**入口；反过来，`@RequestMapping` 通配模式不会带来额外成本。因此该插件无法提供 Feign 那种按 URL 隔离的能力。
- **插件自身没有 `@EnableResilience4j`。** 本模块只交付插件描述符和拦截器：除非你 (a) 在某个 `@Enable*` 注解中列出 `spring-web`，**并且** (b) 启用 Web 扩展，否则什么都不会被激活。忘记 (b) 是最常见的静默失败——bean 存在，但没人调用它。
- **WebFlux 由同样的类支持。** 拦截器的 Javadoc 指向 `EnableWebMvcExtension` 和 `EnableWebFluxExtension`；二者都会发布 `WebEndpointMappingsReadyEvent` 并派发 `HandlerMethodInterceptor` bean。本仓库中的测试只覆盖了 MVC 路径。

## 相关页面

- [开发者指南 — microsphere-resilience4j-spring-web](../../developer-guide/zh/microsphere-resilience4j-spring-web.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
