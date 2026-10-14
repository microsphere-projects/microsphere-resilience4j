# microsphere-resilience4j-parent — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`） · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-parent.md)

本页面面向**从源码构建该库**（或在其之上构建自己的模块）并需要了解共享的**构建父 POM**如何选择 Resilience4j 版本的使用者。它是一个构建期的 POM —— 你继承它，而不是把它作为运行时依赖添加。

## 坐标

| 字段 | 值 |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-parent` |
| version | `${revision}`（解析为聚合反应堆版本，例如 `0.2.4`） |
| packaging | `pom` |
| name | `Microsphere :: Resilience4j :: Parent` |

聚合反应堆中的每个代码模块（`commons`、`spring`、`plugins/*` 各叶子节点以及 `plugins` 聚合器、`dependencies`、`spring-boot`、`spring-cloud`）都将 `microsphere-resilience4j-parent` 声明为其 `<parent>`，因此它们都采用相同的依赖管理和相同的版本 profile。

## 父 POM 配置了什么

父模块 POM **只声明三样东西**（已在 `microsphere-resilience4j-parent/pom.xml` 中核实）：一组版本属性、一个导入 BOM 的 `<dependencyManagement>` 块，以及 Resilience4j 版本的 `<profiles>`。所有构建插件、`pluginManagement`、编译器设置以及 flatten/CI 友好版本机制都**从祖辈 POM 继承**而来 —— 见下面的继承链 —— 并不在此处重新声明。

### 继承链

```
microsphere-resilience4j-parent        (this module, packaging pom)
   └── parent: microsphere-resilience4j          (the reactor root pom)
                   └── parent: microsphere-spring-cloud-parent : 0.2.26   (external grandparent)
```

父模块的 `<parent>` 是聚合反应堆根 POM `microsphere-resilience4j`；该根 POM 又继承自 `io.github.microsphere-projects:microsphere-spring-cloud-parent:0.2.26`，JDK / Spring Cloud 的构建配置正来源于此。

### 受管理的 microsphere 依赖版本（属性）

| 属性 | 版本 | 用于导入 |
|----------|---------|----------------|
| `microsphere-spring-cloud.version` | `0.2.26` | `microsphere-spring-cloud-dependencies` |
| `microsphere-alibaba-druid.version` | `0.2.20` | `microsphere-alibaba-druid-dependencies` |
| `microsphere-mybatis.version` | `0.2.16` | `microsphere-mybatis-dependencies` |
| `microsphere-hibernate.version` | `0.2.10` | `microsphere-hibernate-dependencies` |
| `microsphere-redis.version` | `0.2.14` | `microsphere-redis-dependencies` |

这五个 BOM，加上 `io.github.resilience4j:resilience4j-bom` 和 `io.github.resilience4j:resilience4j-framework-common`，被拉入 `<dependencyManagement>`，采用 `<scope>import</scope>`（resilience4j BOM）或受管理版本（`resilience4j-framework-common` = `${resilience4j.version}`）。它们固定了各插件所集成的每个框架的版本；它们**不会**为仅仅继承本父 POM 的模块添加任何运行时依赖。

> 注解处理器构件（`microsphere-annotation-processor`，供 generated-spring-metadata 各模块使用）在各叶子 POM 中按模块声明，版本由导入的 `microsphere-*` BOM 管理 —— 它并非在本父 POM 内部声明。

## Resilience4j 版本 profile

每个 profile 只设置 `resilience4j.version`；源码是依据稳定的 2.x API 表面编写的，因此在其中任意一个下都能编译。

| profile id | `resilience4j.version` | 激活方式 |
|------------|------------------------|------------|
| `resilience4j-2.0` | `2.0.0` | 仅显式 `-P` |
| `resilience4j-2.1` | `2.1.0` | 仅显式 `-P` |
| `resilience4j-2.2` | `2.2.0` | 仅显式 `-P` |
| `resilience4j-2.3` | `2.3.0` | 仅显式 `-P` |
| `resilience4j-2.4` | `2.4.0` | `activeByDefault = true` |

## 从源码构建时选择 Resilience4j 版本

根 POM（`${revision}`，`java.version=17`）是聚合反应堆的聚合器；你要从仓库根目录构建。

```bash
# Default — no profile passed: the activeByDefault 'resilience4j-2.4' profile wins
./mvnw clean install

# Pin a specific Resilience4j line
./mvnw clean install -P resilience4j-2.0   # or -2.1 / -2.2 / -2.3
./mvnw clean install -P resilience4j-2.4   # same result as the default
```

两个实用注意事项：

1. **一旦同一 POM 中的任何其他 profile 被激活，`activeByDefault` 就会失效。** 如果你激活的 profile 也声明了自己的 `resilience4j.version`，默认的 `2.4.0` *不会*生效。为清晰起见，始终传入一个显式的 `resilience4j-2.x` profile。
2. **不用 profile 的覆盖方式：** 你可以通过直接设置该属性来完全绕过 profile，例如 `./mvnw clean install -Dresilience4j.version=2.2.0`。这对没有专用 profile 的版本很有用。

关于 profile 解析流程图以及 GitHub Actions CI 矩阵如何驱动这些 profile，参见 [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-parent.md)。

## 本版本线上的版本事实

| 事实 | 值 | 来源 |
|------|-------|--------|
| 版本线 | `0.2.x` | 分支 `release` |
| 最新发布版本 | `0.2.4` | 聚合反应堆 / 已发布的构件 |
| 当前 `${revision}` | `0.2.5-SNAPSHOT` | 根 `pom.xml` `<properties>` |
| Resilience4j 范围 | `2.0.0` – `2.4.0`（默认 `2.4.0`） | 父 POM `<profiles>` |
| Spring Cloud 范围 | `2022.0.x` – `2025.0.x` | 经由 `microsphere-spring-cloud-parent` `0.2.26` |
| JDK | `17` 基线（`java.version=17`；CI 在 17/21/25 上构建） | 根 `pom.xml`、`.github/workflows` |

> 注：本 `0.2.x` 线的 JDK 基线是 **17**，取自聚合反应堆 POM 中的 `<java.version>17</java.version>` 以及 CI 矩阵（17/21/25）。针对较旧 JDK 的是 `0.1.x` / `1.x` 线。
