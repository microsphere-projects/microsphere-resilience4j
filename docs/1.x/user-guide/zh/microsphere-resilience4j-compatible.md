# microsphere-resilience4j-compatible — 用户指南（1.x）

> 版本线：`0.1.x`（源分支 `release-1.x`） · 最新发布版本 `0.1.4` · 分支 revision `0.1.5-SNAPSHOT`
> 兼容 **Resilience4j 1.3.0 – 1.6.0**（这是唯一一个为支持 1.7 之前的 profile 而存在的模块）
> 一个构建期的**兼容性垫片**——把派生（fork）自 Resilience4j 的事件类放回原有的 `io.github.resilience4j` 包名下。
> ← [指南索引](README.md) · [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-compatible.md)

---

## 1. 这个模块是什么

`microsphere-resilience4j-compatible` 是一个小型的支持型 jar，只为 `release-1.x`（0.1.x）版本线而存在。
它以原有的 `io.github.resilience4j.*.event` 包名重新发布了少量 Resilience4j **事件**类。按照该模块自身的
`package-info.java` 所述，这些类
*"forked from the Resilience4j 1.7.x (latest), which will be used to be compatible with the lower versions of
Resilience4j."*（派生自 Resilience4j 1.7.x（最新版），用于与更低版本的 Resilience4j 保持兼容。）

`microsphere-resilience4j-commons` 需要它们的原因是：它的断路器与限流器模板暴露了以事件类为类型的
事件订阅方法（`CircuitBreakerOnFailureRateExceededEvent`、
`CircuitBreakerOnSlowCallRateExceededEvent`、`RateLimiterOnDrainedEvent`），
以及那些直到较晚的 1.x 版本才新增相应常量的事件类型枚举。当 commons 针对较旧的 Resilience4j 编译时，这些类型
在上游 jar 中是缺失的；垫片提供了它们，从而**使同一份 commons 源码能够针对 1.3 到 1.7 的每一个发布版本编译**。

package-info 引言，按原意逐字保留：

> The classes in this package or sub-packages are forked from the Resilience4j 1.7.x(latest), which will be used
> to be compatible with the lower versions of Resilience4j.

---

## 2. 它何时被自动加入

你几乎不需要自己添加它。`microsphere-resilience4j-commons/pom.xml` 在四个构建 profile 中把它声明为一项依赖：

| 构建 profile（`-P`） | Resilience4j 版本 | 是否挂载 compatible 垫片 |
|----------------------|----------------------|---------------------------|
| `resilience4j-1.3` | 1.3.0 | 是 |
| `resilience4j-1.4` | 1.4.0 | 是 |
| `resilience4j-1.5` | 1.5.0 | 是 |
| `resilience4j-1.6` | 1.6.0 | 是 |
| `resilience4j-1.7` *（activeByDefault）* | 1.7.0 | **否** —— 1.7.0 已经自带这些事件类型 |

因此：

- 使用**默认** profile（`resilience4j-1.7`）构建 commons **不会**引入垫片。
- 使用 `-P resilience4j-1.3 … 1.6` 构建 commons **会**自动引入垫片——它由该 profile 作为 commons 的一项
  `<dependency>` 注入，版本为 `${revision}`。

```xml
<!-- present in microsphere-resilience4j-commons/pom.xml, inside each of resilience4j-1.3 … resilience4j-1.6 -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-compatible</artifactId>
    <version>${revision}</version>
</dependency>
```

该垫片是 reactor 内的同级模块（`microsphere-resilience4j-compatible`），并列在根
`pom.xml` 的 `<modules>` 中；它**不**由 `microsphere-resilience4j-dependencies` BOM 管理，也**不
会被发布**（其自身 `pom.xml` 中把 `maven-deploy-plugin` 配置为 `<skip>true</skip>`）。它只是一个构建期
构件，仅此而已。

---

## 3. 我的应用应该直接依赖它吗

**不应该——通常永远不必。** 该模块不带任何属于自己的公开 API；它只是回移 Resilience4j 的类型。
你不应该添加一条指向 `microsphere-resilience4j-compatible` 的 `<dependency>`。它只会通过 commons
传递地进入你的 classpath，而且仅当你刻意让 commons 针对 Resilience4j 1.3–1.6 构建时才会出现。对于正常的
应用——无论是以 Resilience4j 1.7（默认）为目标，还是消费已发布的 commons jar——这个垫片根本不存在。

---

## 4. 逐类清单

模块中的每一个类都位于一个真实的 Resilience4j 包名下（正是这种有意为之的包名重合，使该垫片成为"影子兼容"）。共有
**五个事件类**以及 `package-info.java`：

| 类（全限定名） | 种类 | 用途 |
|-------------------------|------|---------|
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent` | 接口 | 断路器事件契约：`getCircuitBreakerName()`、`getEventType()`、`getCreationTime()`，外加 `Type` 枚举。派生副本声明了完整的 1.7.x `Type` 集合——包括 `FAILURE_RATE_EXCEEDED`、`SLOW_CALL_RATE_EXCEEDED`、`FORCED_OPEN`、`DISABLED`——并对 `STATE_TRANSITION` 与 `RESET` 带有 `forcePublish` 标志。 |
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnFailureRateExceededEvent` | 类 | 当断路器突破其失败率阈值时触发的事件；携带 `getFailureRate()` 并上报 `Type.FAILURE_RATE_EXCEEDED`。被 `CircuitBreakerTemplate.onFailureRateExceededEvent(...)` 引用。 |
| `io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSlowCallRateExceededEvent` | 类 | 当慢调用率阈值被突破时触发的事件；携带 `getSlowCallRate()` 并上报 `Type.SLOW_CALL_RATE_EXCEEDED`。被 `CircuitBreakerTemplate.onSlowCallRateExceededEvent(...)` 引用。 |
| `io.github.resilience4j.ratelimiter.event.RateLimiterEvent` | 接口 | 限流器事件契约：`getRateLimiterName()`、`getEventType()`、`getNumberOfPermits()`、`getCreationTime()`，外加 `Type { FAILED_ACQUIRE, SUCCESSFUL_ACQUIRE, DRAINED }`。派生副本新增了下面 drained 事件所使用的 `DRAINED` 常量。 |
| `io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent` | 类 | 当限流器的许可等待者被排空（drain）时触发的事件；上报 `Type.DRAINED` 且 `getNumberOfPermits() == 1`。被 `RateLimiterTemplate.onDrainedEvent(...)` 引用。 |

关于这些具体事件类有两点值得注意：

- `CircuitBreakerOnFailureRateExceededEvent` / `CircuitBreakerOnSlowCallRateExceededEvent` 继承自
  `AbstractCircuitBreakerEvent`，而 `RateLimiterOnDrainedEvent` 继承自 `AbstractRateLimiterEvent`。这两个抽象
  基类都没有在此重新派生——它们来自真实的 `resilience4j-circuitbreaker` / `resilience4j-ratelimiter` jar，
  而之所以可行，恰恰是因为垫片类共享了同一个包（参见开发者指南）。
- 本派生副本中的 `RateLimiterOnDrainedEvent` 暴露的 `getNumberOfPermits()` 固定返回 `1`，与其所回移的 1.7.x
  行为一致。

该模块把所需的 Resilience4j 构件声明为 `<optional>` 的 compile 依赖（`resilience4j-circuitbreaker`、
`resilience4j-ratelimiter`、`resilience4j-core`、`resilience4j-framework-common`，以及该系列的其余部分），从而
让抽象事件基类以及 `ZonedDateTime` 一类的类型在编译期能够解析，同时不向下游强制指定版本。

---

## 5. 构建命令示例与 classpath 效果

针对 Resilience4j **1.4.0**（一个缺少 drained/slow-call 事件类型的 1.7 之前版本）构建 commons：

```bash
./mvnw -pl microsphere-resilience4j-commons -am clean install -P resilience4j-1.4
```

对 classpath 的影响：

1. 父工程 profile `resilience4j-1.4` 把 `resilience4j.version` 设为 `1.4.0`，因此所有 `io.github.resilience4j:*`
   构件都解析为 `1.4.0`。
2. commons 的 profile `resilience4j-1.4` 被激活，并添加了对
   `io.github.microsphere-projects:microsphere-resilience4j-compatible:0.1.5-SNAPSHOT`（`${revision}`）的依赖。
3. 垫片 jar 进入 compile/runtime classpath，**早于** 1.4.0 的 Resilience4j 事件类成为问题之前，它以原有的
   `io.github.resilience4j.*.event` 名称提供了 `CircuitBreakerEvent`（带 `FAILURE_RATE_EXCEEDED` / `SLOW_CALL_RATE_EXCEEDED`）、
   `CircuitBreakerOnFailureRateExceededEvent`、`CircuitBreakerOnSlowCallRateExceededEvent`、`RateLimiterEvent`
   （带 `DRAINED`）以及 `RateLimiterOnDrainedEvent`。
4. 这一份唯一的源码树——`CircuitBreakerTemplate`、`RateLimiterTemplate`，以及引用这些类型的测试——无需改动即可
   编译并运行。

与默认情况对比：

```bash
./mvnw -pl microsphere-resilience4j-commons -am clean install          # profile resilience4j-1.7 (activeByDefault)
```

此时 `resilience4j.version` 为 `1.7.0`，commons 的垫片 profile **不**会激活，而真实的 1.7.0
Resilience4j jar 已经包含每一种事件类型——因此 `microsphere-resilience4j-compatible` 完全**不在**
classpath 上。

---

## 6. 与 0.2.x 版本线的差异

`compatible` 模块在 `0.2.x` 版本线上**并不存在**。那条版本线以 Resilience4j 2.x 为目标，其事件模型在
受支持范围内是稳定的，因此不需要任何回移垫片。如果你在维护一个 `0.2.x`
应用，本页与本模块都与你无关；请使用[当前指南](../../../current/user-guide/zh/README.md)。

---

所有页面的中文译文位于 `zh/` 目录下，文件名与英文版本完全一致。
