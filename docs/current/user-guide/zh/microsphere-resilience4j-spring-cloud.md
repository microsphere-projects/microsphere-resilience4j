# microsphere-resilience4j-spring-cloud — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · **状态：空占位模块（未交付任何代码）** · JDK **17+** · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-spring-cloud.md)

## 摘要 —— 本模块尚未交付任何功能

`microsphere-resilience4j-spring-cloud` 是一个**占位模块**。它的目录**仅**包含一个 `pom.xml`；不存在 `src/` 目录树（通过对 `microsphere-resilience4j-spring-cloud/` 的目录列举已核实）。打包方式为 `jar`，因此构建会产出一个不含任何类和资源的空 jar。

> **不要指望它带来 Spring Cloud 集成而添加此依赖。** 如今它什么也不做。在 Spring 上下文中的 Resilience4j 行为来自 [`microsphere-resilience4j-spring`](microsphere-resilience4j-spring.md)。

## 坐标

| 字段 | 值 |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-spring-cloud` |
| version | `${revision}` |
| packaging | `jar` |
| name | `Microsphere :: Resilience4j :: Spring Cloud` |
| parent | `microsphere-resilience4j-parent` |
| `<properties>` | （空块 —— 不声明任何东西） |
| `<dependencies>` | **本 POM 中未声明任何依赖** |

## 该模块如何（以及不）被接入

它是构建聚合反应堆的成员（因此会随项目一起被构建/发布），但除此之外完全被隔离：

| 它出现在哪里 | 引用 |
|------------------|-----------|
| 根聚合反应堆 `<modules>` | 是 —— 列出了 `microsphere-resilience4j-spring-cloud` |
| 它自身的 `pom.xml` | 是 |
| `microsphere-resilience4j-dependencies`（BOM） | **否** —— 不受 BOM 版本管理 |
| 任何其他模块的 `<dependencies>` | **否** —— 没有任何东西依赖它 |
| `README.md` 模块表 | **否** —— 未列出 |
| `release-notes.md` | **否** —— 没有任何功能条目提及它 |

对整个仓库检索该 artifactId，返回的命中仅出现在根 `pom.xml` 的 `<modules>` 条目以及该模块自身的 `pom.xml` 中，这印证了它在别处没有被引用。

## 占位模块为何存在

该坐标、`jar` 打包，以及从 `microsphere-resilience4j-parent`（它会带入 Resilience4j 版本 profile 和 Spring Cloud / 框架 BOM）的继承，表明本模块**保留了 `io.github.microsphere-projects:microsphere-resilience4j-spring-cloud`**，留待未来的 Spring Cloud 层使用 —— 那是一个天然的家，用于放置诸如从 Spring Cloud Config 外部化 `Resilience4jRegistry` 配置、熔断器的 actuator/health 绑定，或 Spring Cloud 专属装饰器之类的东西。

> 准确性说明：**没有任何源文件、README 条目或发布说明记录这一理由。** 该陈述仅从打包、坐标和命名作出推断。`release` 快照中没有任何东西保证这样的模块已被排期 —— 请把它读作“已保留，而非已承诺”。

## 实现它需要什么（面向使用者的概述）

如果它变为真实存在，Spring Cloud 使用者可能会期待：

1. 一个由 Spring Cloud 激活的 `src/main/java` 配置层（`org.springframework.cloud:*` 依赖，版本由所继承的 `microsphere-spring-cloud-dependencies:0.2.26` 提供）。
2. `spring.factories` / `AutoConfiguration.imports` 装配，使得注册表和装饰器可从 Spring Cloud 属性配置。
3. 对 `microsphere-resilience4j-spring`（以及传递性地对 `-commons`）的依赖。
4. 在 `microsphere-resilience4j-dependencies` BOM 中注册该构件，以便可以省略其版本。

在某个真实发布版本交付这些之前，请依赖 `microsphere-resilience4j-spring`。[开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-spring-cloud.md) 详细记录了聚合反应堆位置和实现清单。

## 如何判断未来的某个发布版本是真实存在的

本模块已经在 `0.2.x` 线上发布了一个（空的）jar，因此“它存在于 Maven Central 上”并不意味着它可用。在依赖它之前：

1. **检查 BOM。** 出现在 `microsphere-resilience4j-dependencies` 中，标志着从占位模块到受支持坐标的转变。
2. **检查 jar 内容。** 一个真实的 Spring Cloud 层会在 `io/microsphere/resilience4j/spring/cloud/...` 下交付类和配置；`0.2.4` 的 jar 什么都没有。
3. **检查文档。** README 的模块表和 `release-notes.md` 如今都没有提及本模块。

## 常见问题

- **“它会从 Spring Cloud Config 读取 Resilience4j 配置吗？”** 不会 —— jar 是空的；请通过 `microsphere-resilience4j-spring` 自行装配注册表。
- **“为什么在构建中保留一个空模块？”** 它保留了该坐标并留在聚合反应堆 `<modules>` 中，因此会随每个版本自动被构建和发布。
- **“它需要自己的 Spring Cloud 版本 profile 吗？”** 不需要 —— `spring-cloud-2022…2025` profile 从 `microsphere-spring-cloud-parent:0.2.26` 继承而来，已经在父 POM 的类路径上。

## 本版本线上的版本事实

| 事实 | 值 |
|------|-------|
| 版本线 | `0.2.x`（`release` 分支） |
| 聚合反应堆 POM 中的 `${revision}` | `0.2.5-SNAPSHOT` |
| 最新发布版本 | `0.2.4` —— 占位 jar 在本线上存在，但为空 |
| Spring Cloud 范围（经由父 POM） | `2022.0.x` – `2025.0.x`（`microsphere-spring-cloud-parent` `0.2.26`） |
| JDK | `17+` |
