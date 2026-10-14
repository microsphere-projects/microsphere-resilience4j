# microsphere-resilience4j-dependencies — 用户指南（1.x / v0.1.x）

> 版本线 `0.1.x`（`release-1.x` 分支） · 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · 管理 **6** 个构件 · CI 实测的 JDK：8、11、17、21、25 · [← 用户指南索引](README.md)
> [English](../microsphere-resilience4j-dependencies.md)

`microsphere-resilience4j-dependencies` 是 1.x 版本线的**依赖管理清单（BOM）**。它是一个 `pom` 构件，你在 `<dependencyManagement>` 中导入它，从而让你的应用能够依赖 microsphere-resilience4j 的各个模块，**而不必为每一个都写出版本号**。

## 坐标

| 字段 | 值 | 来源 |
|-------|-------|--------|
| groupId | `io.github.microsphere-projects` | `microsphere-resilience4j-dependencies/pom.xml` |
| artifactId | `microsphere-resilience4j-dependencies` | 同上 |
| version | `${revision}` → 发布为 `0.1.0` … `0.1.4` | 同上 + git 标签 |
| packaging | `pom` | 同上 |
| name | `Microsphere :: Resilience4j :: Dependencies` | 同上 |
| `<parent>` | `microsphere-resilience4j-parent`（`${revision}`，`relativePath ../microsphere-resilience4j-parent/pom.xml`） | 同上 |

## BOM 所管理的构件（完整、已核验的清单）

阅读 `microsphere-resilience4j-dependencies/pom.xml` 的 `<dependencyManagement>` 块，它包含**恰好六个**条目，全部属于 `io.github.microsphere-projects`，全部固定为 `${revision}`（也就是说，一旦 BOM 发布出去，这些条目的版本就等于 BOM 自己的版本）：

| # | 受管理的 artifactId | BOM 中的版本 | 你能从中得到什么 |
|---|--------------------|--------------------|----------------------|
| 1 | `microsphere-resilience4j-commons` | `${revision}` | 与框架无关的模板、门面、`Resilience4jContext` |
| 2 | `microsphere-resilience4j-alibaba-druid` | `${revision}` | 基于 Druid `Filter` 的插件 |
| 3 | `microsphere-resilience4j-openfeign` | `${revision}` | OpenFeign 插件 |
| 4 | `microsphere-resilience4j-mybatis` | `${revision}` | MyBatis `Executor` 插件 |
| 5 | `microsphere-resilience4j-spring-web` | `${revision}` | Spring Web 处理器拦截器插件 |
| 6 | `microsphere-resilience4j-spring` | `${revision}` | Spring `@Enable*` 注解、事件桥接、插件 SPI |

上面的顺序就是文件中的顺序（`commons`、`alibaba-druid`、`openfeign`、`mybatis`、`spring-web`、`spring`）——没有第七个条目。

> **1.x 与 0.2.x 两份 BOM 管理的是同样六个构件。** 两份清单完全一致（通过比对两个文件核验），因此构件的*名称*在两条版本线之间是一份稳定的契约；两条线之间的差别在于你导入的版本（`0.1.4` 对 `0.2.4`）以及其背后的 Resilience4j / Spring 技术栈。

## BOM **不**管理的构件（已核验）

| 构件 | 在聚合反应堆（reactor）中构建？ | 在 BOM 中？ | 后果 |
|----------|-----------------------|-------------|-------------|
| `microsphere-resilience4j-hibernate-core` | 是（`microsphere-resilience4j-plugins/pom.xml`） | **否** | 你必须自己写出它的版本 |
| `microsphere-resilience4j-compatible` | 是（根 `<modules>`） | **否** | 只是一个构建期垫片；它自己的 POM 还把 `maven-deploy-plugin` 设为 `<skip>true</skip>`，因此不会被部署 |
| `microsphere-resilience4j-spring-boot` | 是 | **否** | 占位模块：`packaging` 是 `jar`，但该模块只包含一个 `pom.xml`（没有 `src/`） |
| `microsphere-resilience4j-spring-cloud` | 是 | **否** | 占位模块：`packaging` 是 `jar`，但该模块只包含一个 `pom.xml`（没有 `src/`） |
| `microsphere-resilience4j-plugins` | 是 | **否** | 聚合器（aggregator）`pom`，从来不作为依赖 |
| `microsphere-resilience4j`（根）、`microsphere-resilience4j-parent` | 是 | **否** | 聚合反应堆根 / 构建父工程——用作 `<parent>`，而不是被导入 |
| 第三方的 Resilience4j / Hibernate / MyBatis / Druid / Redis | — | 不在 BOM 本身之中 | **间接**管理：BOM 继承 `microsphere-resilience4j-parent`，后者的 `<dependencyManagement>` 以 `${resilience4j.version}` 列出了 24 个 `io.github.resilience4j:*` 坐标，并导入 `microsphere-spring-cloud-dependencies:0.1.26`、`-alibaba-druid-dependencies:0.1.20`、`-mybatis-dependencies:0.1.15`、`-hibernate-dependencies:0.1.9`、`-redis-dependencies:0.1.13` |

> **这些被继承的管理条目究竟有多少会到达*你的*项目，是你不应该去指望的。** 那六个第一方条目是声明在 BOM 文件本身里的；上面那些框架坐标住在 BOM 所*继承*的那个 POM 中，而一次 `<scope>import</scope>` 能带过来什么，取决于 Maven 对被导入模型的解析方式，以及那份已发布 POM 在部署时是如何被扁平化的。请把这份 BOM 仅仅当作六个第一方构件的版本来源，把你应用真正需要的框架版本自己固定住，并在你自己的项目里确认结果：
>
> ```bash
> ./mvnw -f your-project/pom.xml help:effective-pom | grep -A2 "resilience4j"
> ./mvnw -f your-project/pom.xml dependency:tree
> ```

## 如何使用它

### 第 1 步——导入 BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.1.4</version>          <!-- 0.1.x 这条版本线上的最新发布版本 -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 第 2 步——声明你需要的模块，不写版本

```xml
<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-commons</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-spring</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-mybatis</artifactId>
    </dependency>
</dependencies>
```

### 第 3 步——补上 BOM 未覆盖的那些版本

各插件模块都把 Resilience4j、Spring、Hibernate、MyBatis 和 Druid 声明为 `optional`，所以**这些版本归你的应用管**。让它们与 1.x 版本线保持一致的最稳妥做法，是导入一个落在 Hoxton – 2021.0.x 区间内的 Spring Boot / Spring Cloud BOM，另外如果你直接使用 Resilience4j，再加上你所需要的 `io.github.resilience4j` 坐标：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 示例：Boot 2.7.x 与 Spring Cloud 2021.0.x 配对；请选择与你自己的
             Spring Cloud 版本 train 相匹配的那条 Boot 线（Hoxton -> Boot 2.2/2.3，2020 -> 2.4/2.5）。
             导入顺序很重要：你自己的条目放在最前面。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.18</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
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
    <!-- Resilience4j 各模块：0.1.x 这条版本线面向 1.3.0 - 1.7.0，默认 1.7.0 -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>1.7.0</version>
    </dependency>
</dependencies>
```

请固定一个 Resilience4j 1.x 版本并留在 `1.3.0`–`1.7.0` 之内；这条版本线的源码是照着 1.x 的 `Registry` / `EventPublisher` API 写的，无法针对 2.x 编译。

## 显式地固定 Hibernate 插件的版本

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.1.4</version>   <!-- 不受 BOM 管理 -->
</dependency>
```

`microsphere-resilience4j-hibernate-core` 是 1.x 聚合反应堆的一个真实模块，但它在这份 BOM 中缺席，所以导入 BOM 不会为它填上版本——一条不带版本的声明会以 `'dependencies.dependency.version' is missing` 的构建错误失败。请加上上面那个 `<version>`，或者加一条对应的 `<dependencyManagement>` 条目。

## 覆盖单个受管理的版本

你自己的 `<dependencyManagement>` 条目只要出现在 BOM 导入**之前**就会胜出；而在依赖本身上写显式的 `<version>` 则永远胜出：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 先被求值，因此优先级更高 -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-commons</artifactId>
            <version>0.1.5-SNAPSHOT</version>
        </dependency>
        <!-- 其余部分由 BOM 提供 -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.1.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

不要在一个应用里混用 `0.1.x` 和 `0.2.x` 的构件：两条版本线面向互不兼容的 Resilience4j 大版本（1.x 对 2.x）以及不同的 Spring 技术栈（Hoxton–2021 对 2022–2025）。

## 维护者如何把 BOM 与某个版本 profile 配对

BOM 是*从一次聚合反应堆构建中发布出来的*，所以构建时所选的 profile 决定了这六个构件是针对哪一条 Resilience4j 版本线编译和测试的——并且，对于 `1.3`–`1.6`，还决定了 `compatible` 垫片是否出现在编译 classpath 上：

```bash
# 发布式的构建：Resilience4j 1.7.0，不挂垫片
./mvnw clean deploy -Drevision=0.1.4 -P resilience4j-1.7

# BOM 内容相同，但针对 Resilience4j 1.5.0 编译（垫片会被自动挂上）
./mvnw clean install -P resilience4j-1.5
```

`${revision}` 是聚合反应堆根中的一个单一属性（在本快照上是 `0.1.5-SNAPSHOT`）；部署时的 `-Drevision=…` 会给 BOM 和那六个受管理条目盖上同一个具体版本。发布用的 workflow 会强制校验版本格式（`^[0-9]+\.[0-9]+\.[0-9]+$`），随后把根 `<revision>` 提升到下一个补丁快照，这就是为什么在 `0.1.4` 发布之后这个分支读到的是 `0.1.5-SNAPSHOT`。

## 本版本线上的版本事实

| 事实 | 值 |
|------|-------|
| 要导入的 BOM 版本 | `0.1.4`（最新发布版本；`0.1.5-SNAPSHOT` 尚未发布） |
| 受管理的第一方构件 | 6 个（与 `0.2.x` BOM 的清单完全一致） |
| 不受管理 | `hibernate-core` 插件、`compatible`、`spring-boot`、`spring-cloud`、各聚合器/父工程 |
| 构件编译时所针对的 Resilience4j 版本线 | `1.3.0` – `1.7.0`，默认 `1.7.0`（Maven profile） |
| Spring Cloud | Hoxton – 2021.0.x（CI profiles `spring-cloud-hoxton`、`-2020`、`-2021`） |
| JDK | 本版本线上没有 `java.version` 属性；CI 构建 JDK 8/11/17/21/25，发布在 JDK 11 上运行 |

## 相关页面

- [开发者指南 — microsphere-resilience4j-dependencies（1.x）](../../developer-guide/zh/microsphere-resilience4j-dependencies.md)
- [构建父工程 — microsphere-resilience4j-parent（1.x）](microsphere-resilience4j-parent.md)
- [1.x 用户指南索引](README.md)
- [当前（0.2.x）BOM 页面](../../../current/user-guide/zh/microsphere-resilience4j-dependencies.md)
