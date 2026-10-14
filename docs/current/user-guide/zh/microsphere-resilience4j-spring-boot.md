# microsphere-resilience4j-spring-boot — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · **状态：空占位模块（未交付任何代码）** · JDK **17+** · [← 指南索引](README.md)
> [English](../microsphere-resilience4j-spring-boot.md)

## 摘要 —— 本模块尚未交付任何功能

`microsphere-resilience4j-spring-boot` 是一个**占位模块**。它的目录**仅**包含一个 `pom.xml`；不存在 `src/` 目录树（通过对 `microsphere-resilience4j-spring-boot/` 的目录列举已核实）。对本模块执行 `mvn package` 会产出一个不含任何类和资源的 jar。

> **不要为了期待某种行为而在项目中添加此依赖。** 如今它什么也不贡献。要集成 Spring，请改用 [`microsphere-resilience4j-spring`](microsphere-resilience4j-spring.md)。

## 坐标

| 字段 | 值 |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-spring-boot` |
| version | `${revision}` |
| packaging | `jar` |
| name | `Microsphere :: Resilience4j :: Spring Boot` |
| parent | `microsphere-resilience4j-parent` |
| `<properties>` | （空块 —— 不声明任何东西） |
| `<dependencies>` | **本 POM 中未声明任何依赖** |

## 该模块如何（以及不）被接入

该模块是构建聚合反应堆的真实成员，因此会与项目其余部分一起被构建和发布，但它被刻意隔离：

| 它出现在哪里 | 引用 |
|------------------|-----------|
| 根聚合反应堆 `<modules>` | 是 —— 列出了 `microsphere-resilience4j-spring-boot` |
| 它自身的 `pom.xml` | 是 |
| `microsphere-resilience4j-dependencies`（BOM） | **否** —— 不受 BOM 版本管理 |
| 任何其他模块的 `<dependencies>` | **否** —— 没有任何东西依赖它 |
| `README.md` 模块表 | **否** —— 未列出 |
| `release-notes.md` | **否** —— 没有任何功能条目提及它 |

对整个仓库检索该 artifactId，返回的命中仅出现在根 `pom.xml` 的 `<modules>` 条目以及该模块自身的 `pom.xml` 中，这印证了它在别处没有被引用。

## 占位模块为何存在

因为该坐标已定义、继承了构建父 POM（Resilience4j profile + 框架 BOM），并打包为 `jar`，它的存在**保留了 Maven 坐标 `io.github.microsphere-projects:microsphere-resilience4j-spring-boot`**，留待未来的 Spring Boot 自动配置模块使用 —— 它是 `microsphere-resilience4j-spring` 的约定配套件，将提供 `@EnableAutoConfiguration` 式的配置。

> 准确性说明：**没有任何源文件、README 条目或发布说明记录这一理由。** 上面的陈述是从该模块的打包/坐标以及命名约定作出的推断。`release` 快照中没有任何东西证明自动配置已被排期，因此请把它当作“已保留，而非已承诺”来看待。

## 实现它需要什么（面向使用者的概述）

如果且当本模块变为真实存在时，Spring Boot 使用者大致会期待：

1. `src/main/java` 自动配置类（例如 `Resilience4jAutoConfiguration`）加上一个 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册文件。
2. 对 `org.springframework.boot:spring-boot-autoconfigure` 的依赖（版本由父 POM 所继承的 `microsphere-spring-cloud-dependencies` / Spring Boot BOM 线提供）。
3. 到 `microsphere-resilience4j-spring` / `-commons` 的 compile 作用域装配，以便通过 Boot 配置属性提供 `@EnableResilience4j` 行为。
4. 在 `microsphere-resilience4j-dependencies` BOM 中注册该构件，以便使用者可以省略其版本。

在某个发布版本真正实现这些之前，请继续使用 `microsphere-resilience4j-spring` 并直接使用 `@Enable*` 注解。关于聚合反应堆位置和完整实现清单，参见 [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-spring-boot.md)。

## 如何判断未来的某个发布版本是真实存在的

因为本模块已经在 `0.2.x` 线上发布了一个（空的）jar，“构件存在于 Maven Central 上”并**不**证明它做了什么。在依赖它之前，请先核实该发布版本确实添加了内容：

1. **检查 BOM。** 一旦 `microsphere-resilience4j-spring-boot` 出现在 `microsphere-resilience4j-dependencies` 中，它就从占位模块被提升为受支持的坐标。
2. **检查 jar。** 一个已实现的模块会在 `io/microsphere/resilience4j/spring/boot/...` 下交付类，并带有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 条目。当前 `0.2.4` 的 jar 这些都不包含。
3. **检查发布说明 / README。** `release` 快照的 README 模块表和 `release-notes.md` 都没有提及本模块 —— 一个真实的实现通常会添加相应条目。

## 常见问题

- **“我添加了依赖，却什么也没发生。”** 这是预期 —— jar 是空的。今天请使用 `microsphere-resilience4j-spring` 和 `@EnableResilience4j`。
- **“它会被移除吗？”** 它保留在聚合反应堆中，因此会被构建和发布；没有任何已记录的计划要移除它。
- **“既然它什么都不做，为什么还要构建它？”** 它是一个聚合反应堆 `<module>`，因此每次构建都会将其打包；那个空 jar 是无害的副产品。

## 本版本线上的版本事实

| 事实 | 值 |
|------|-------|
| 版本线 | `0.2.x`（`release` 分支） |
| 聚合反应堆 POM 中的 `${revision}` | `0.2.5-SNAPSHOT` |
| 最新发布版本 | `0.2.4` —— 占位 jar 在本线上存在，但为空 |
| JDK | `17+`（从聚合反应堆 POM / CI 继承） |
