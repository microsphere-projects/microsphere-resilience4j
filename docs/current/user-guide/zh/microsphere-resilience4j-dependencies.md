# microsphere-resilience4j-dependencies — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · 管理 **6** 个构件 · JDK **17+** · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-dependencies.md)

`microsphere-resilience4j-dependencies` 是本项目的**依赖管理清单**（BOM，Bill of Materials）。它是一个 `pom` 构件，你在 `<dependencyManagement>` 中导入它，从而可以依赖各个 microsphere-resilience4j 模块而**无需为每一个都写版本号**。

## 坐标

| 字段 | 值 |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-dependencies` |
| version | `${revision}`（发布为例如 `0.2.4`） |
| packaging | `pom` |
| name | `Microsphere :: Resilience4j :: Dependencies` |
| parent | `microsphere-resilience4j-parent`（因此它继承 Resilience4j 版本 profile 以及导入的各框架 BOM） |

## BOM 管理的构件

下面每一项都是 `io.github.microsphere-projects`，并被固定到 `${revision}` —— 也就是与 BOM 本身相同的发布版本。这是来自 `microsphere-resilience4j-dependencies/pom.xml` 的**完整、已核实**列表（恰好六个构件）：

| # | 受管理的 artifactId | BOM 中的版本 | 用途 |
|---|--------------------|----------------|---------|
| 1 | `microsphere-resilience4j-commons` | `${revision}` | 与框架无关的模板 / 门面核心 |
| 2 | `microsphere-resilience4j-spring` | `${revision}` | Spring `@Enable*` 注解、事件桥接、插件 SPI |
| 3 | `microsphere-resilience4j-alibaba-druid` | `${revision}` | Druid SQL 连接池插件 |
| 4 | `microsphere-resilience4j-mybatis` | `${revision}` | MyBatis `Executor` 插件 |
| 5 | `microsphere-resilience4j-openfeign` | `${revision}` | OpenFeign 插件 |
| 6 | `microsphere-resilience4j-spring-web` | `${revision}` | Spring Web MVC 插件 |

> **BOM 不管理什么（已核实）：** `microsphere-resilience4j-hibernate-core`、`microsphere-resilience4j-plugins` 聚合器，以及占位模块 `microsphere-resilience4j-spring-boot` / `microsphere-resilience4j-spring-cloud` 都**缺席**于这个 `<dependencyManagement>`。最值得注意的是，`microsphere-resilience4j-hibernate-core` 在聚合反应堆中被构建，却不受 BOM 的版本管理 —— 见下方陷阱。

## 如何使用它（import 作用域）

在 `<dependencyManagement>` 中导入一次 BOM，然后列出你需要的各模块且**不写**版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.2.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-commons</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-spring</artifactId>
    </dependency>
</dependencies>
```

## 覆盖单个版本

BOM 条目只是一个*默认值*；如果你自己的 `<dependencyManagement>` 条目出现在 BOM 导入**之前**，它会胜出。若要将某个特定模块强制为不同版本（例如一个本地构建的快照），请在导入之前声明它：

```xml
<dependencyManagement>
    <dependencies>
        <!-- your override is evaluated first and takes precedence -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-commons</artifactId>
            <version>0.2.5-SNAPSHOT</version>
        </dependency>
        <!-- then import the BOM for everything else -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.2.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

你也可以直接在 `<dependencies>` 条目本身设置一个显式的 `<version>`；依赖上的直接版本总会覆盖受管理的默认值。

## 陷阱：Hibernate 插件不在 BOM 中

`microsphere-resilience4j-hibernate-core` 是一个真实的、已发布的模块（它出现在 `microsphere-resilience4j-plugins` 下的聚合反应堆中），但它**没有**列在这个 BOM 里。如果你想要 Hibernate 插件，就必须自行提供它的版本 —— BOM 导入不会填入它：

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.2.4</version>   <!-- NOT managed by the BOM -->
</dependency>
```

另一方面，Hibernate 的*第三方*技术栈（`org.hibernate` 各构件）是间接地受版本管理的：父 POM 导入了 `io.github.microsphere-projects:microsphere-hibernate-dependencies:0.2.10`。仅仅是 microsphere-resilience4j 的 Hibernate **插件**坐标在 BOM 中缺失。

## 本版本线上的版本事实

| 事实 | 值 |
|------|-------|
| 要导入的 BOM 版本 | `0.2.4`（`0.2.x` 线上的最新发布版本） |
| 受管理的 microsphere-resilience4j 构件 | 6（见上表） |
| Resilience4j / Spring 版本 | 经由 `microsphere-resilience4j-parent` 继承（`0.2.26` Spring Cloud 线） |
| JDK | `17+` |

关于 BOM 如何生成以及 `${revision}` 如何在发布时被 flatten（摊平），参见 [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-dependencies.md)。
