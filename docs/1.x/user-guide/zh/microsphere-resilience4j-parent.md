# microsphere-resilience4j-parent — 用户指南（1.x / v0.1.x）

> 版本线 `0.1.x`（`release-1.x` 分支） · 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0**（默认 `1.7.0`） · Spring Cloud Hoxton – 2021.0.x · CI 实测的 JDK：**8、11、17、21、25** · [← 用户指南索引](README.md)

`microsphere-resilience4j-parent` 是 1.x 版本线共享的**构建父工程**。它是一个 `pom` 构件：你继承它（或阅读它），而不是依赖它。本页说明它锁定了哪些内容，以及维护者如何选择让整个 reactor 针对哪一个 Resilience4j 1.x 版本进行构建。

## 坐标

| 字段 | 值 | 来源 |
|-------|-------|--------|
| groupId | `io.github.microsphere-projects` | `microsphere-resilience4j-parent/pom.xml` |
| artifactId | `microsphere-resilience4j-parent` | 同上 |
| version | `${revision}` → 本分支快照为 `0.1.5-SNAPSHOT`；`0.1.4` 是最后一次发布 | 根 `pom.xml` 的 `<revision>`、git 标签 `0.1.4` |
| packaging | `pom` | 模块 POM |
| name | `Microsphere :: Resilience4j :: Parent` | 模块 POM |
| `<parent>` | `microsphere-resilience4j`（`${revision}`，`relativePath ../pom.xml`） | 模块 POM |

## 继承链

```
microsphere-resilience4j-parent        (this module, packaging pom)
   └── parent: microsphere-resilience4j        (reactor root POM, ${revision} = 0.1.5-SNAPSHOT)
            └── parent: microsphere-spring-cloud-parent : 0.1.26   (external grandparent)
```

上级（grandparent）`0.1.26` 是本版本线上 Spring Cloud 那一侧的父工程：它承载了编译器/构建插件配置，以及 CI 所激活的 `spring-cloud-hoxton` / `spring-cloud-2020` / `spring-cloud-2021` profile。它**不**属于本仓库快照，因此这里只通过各 POM 所声明的坐标来指代其内容。

## 父工程声明了什么

自上而下阅读 `microsphere-resilience4j-parent/pom.xml`，该文件恰好只包含三个部分——`<properties>`、`<dependencyManagement>` 和 `<profiles>`——别无其他（没有 `<dependencies>`，也没有 `<build>`）。

### 1. 受管理的 microsphere 集成版本

| 属性 | 值 | 用于导入 |
|----------|-------|----------------|
| `microsphere-spring-cloud.version` | `0.1.26` | `microsphere-spring-cloud-dependencies`（`type=pom`、`scope=import`） |
| `microsphere-alibaba-druid.version` | `0.1.20` | `microsphere-alibaba-druid-dependencies` |
| `microsphere-mybatis.version` | `0.1.15` | `microsphere-mybatis-dependencies` |
| `microsphere-hibernate.version` | `0.1.9` | `microsphere-hibernate-dependencies` |
| `microsphere-redis.version` | `0.1.13` | `microsphere-redis-dependencies` |

正是这五项导入，使得各模块 POM 可以**不带版本号**地声明 `microsphere-hibernate-core`、`microsphere-mybatis-*`、Druid 与 Redis 坐标。`0.1.4` 这次发布把 `microsphere-spring-cloud` 提升到了 `0.1.26`（见 `release-notes.md`："Upgrade microsphere-spring-cloud to 0.1.26"）。

### 2. 显式的 Resilience4j 版本管理（24 个坐标）

与 `0.2.x` 版本线不同，1.x 父工程**不**导入 `io.github.resilience4j:resilience4j-bom`，而是自己逐个列出构件，全部使用 `${resilience4j.version}`：

`resilience4j-all`、`resilience4j-bulkhead`、`resilience4j-cache`、`resilience4j-circuitbreaker`、`resilience4j-circularbuffer`、`resilience4j-consumer`、`resilience4j-feign`、`resilience4j-framework-common`、`resilience4j-kotlin`、`resilience4j-metrics`、`resilience4j-micrometer`、`resilience4j-prometheus`、`resilience4j-ratelimiter`、`resilience4j-ratpack`、`resilience4j-reactor`、`resilience4j-retrofit`、`resilience4j-retry`、`resilience4j-rxjava2`、`resilience4j-rxjava3`、`resilience4j-spring`、`resilience4j-spring-boot2`、`resilience4j-spring-cloud2`、`resilience4j-timelimiter`、`resilience4j-vertx`。

> **模块所用到的构件中有两个不在此列表中：** `resilience4j-annotations` 和 `resilience4j-core` 由 commons、compatible 及各插件 POM 声明（为 `optional`）且**不带 `<version>`**，而本父工程从不管理它们。因此它们的版本来自更上层的某个被导入 BOM（`microsphere-spring-cloud-dependencies:0.1.26` 这条链），而那在本仓库之外。如果你需要知道某次构建实际解析出的确切数字，请运行 `./mvnw -P resilience4j-1.7 help:effective-pom` 并据此查询这些坐标——不要假定它就等于 `${resilience4j.version}`。

### 3. Resilience4j 版本 profile

| profile id | `resilience4j.version` | 激活方式 | 对 reactor 的额外影响 |
|------------|------------------------|------------|-----------------------------|
| `resilience4j-1.3` | `1.3.0` | 仅显式 `-P` | `microsphere-resilience4j-commons` 同时依赖 `microsphere-resilience4j-compatible` |
| `resilience4j-1.4` | `1.4.0` | 仅显式 `-P` | 同上 |
| `resilience4j-1.5` | `1.5.0` | 仅显式 `-P` | 同上 |
| `resilience4j-1.6` | `1.6.0` | 仅显式 `-P` | 同上 |
| `resilience4j-1.7` | `1.7.0` | **`activeByDefault = true`** | 无——1.7.0 已具备垫片所提供的那些事件类 |

按 profile 注入依赖的逻辑位于 `microsphere-resilience4j-commons/pom.xml`，而非父工程；父工程只设置版本属性。流程图见[开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-parent.md#profile-解析流程图)。

## 选择版本 profile（维护者工作流）

始终从仓库根目录（reactor 聚合器 `microsphere-resilience4j`）进行构建：

```bash
# 默认构建：不指定 profile -> 'resilience4j-1.7'（activeByDefault）-> Resilience4j 1.7.0
./mvnw clean install

# 针对较旧的 1.x 版本线构建并测试；compatible 垫片会被自动挂载
./mvnw clean install -P resilience4j-1.3     # 或 -1.4 / -1.5 / -1.6

# 与某个 Spring Cloud 版本 train profile（定义于上级 POM，不在本仓库）组合使用
./mvnw clean install -P resilience4j-1.5,spring-cloud-hoxton

# 对于没有对应 profile 的版本，完全绕过这些 profile
./mvnw clean install -Dresilience4j.version=1.6.1
```

实践中会咬人的几条规则：

1. **一旦同一有效模型中激活了任何其他 profile，`activeByDefault` 就会被抑制。** 若某次构建激活了 `spring-cloud-hoxton` 却**没有**指定任何 `resilience4j-1.x` profile，`resilience4j.version` 就可能处于未设置状态，于是每一个受管理的 `io.github.resilience4j:*` 坐标都不再有版本。这正是 `.github/workflows/maven-build.yml` 总会同时传入一个 `maven-profile-spring-cloud` 和一个 `maven-profile-resilience4j` 值的原因。
2. **通过属性覆盖是脆弱的。** `resilience4j.version` **仅**声明在上述这些 profile 内部（本版本线的任何 `<properties>` 块中都没有 `<resilience4j.version>`），所以 `-Dresilience4j.version=1.6.1` 这个思路本身是成立的——但在同一次构建中激活的任何 profile 也都会贡献该属性，而 profile 的值会遮蔽命令行的值。如果必须使用一个没有专属 profile 的版本，请显式停用默认的那个：
   ```bash
   ./mvnw clean install -P '!resilience4j-1.7' -Dresilience4j.version=1.6.1
   ```
   否则请优先使用这五个内置 profile；它们是 CI 真正校验过的组合。
3. **`-P resilience4j-1.3 … 1.6` 改变的是依赖图，而不仅仅是版本**——垫片 jar 会出现在 commons 下游一切的 classpath 上（它是作为普通的、非 optional 的 `compile` 依赖被添加的）。在同一次构建中激活两个 `resilience4j-*` profile 会合并出两个不同的 `resilience4j.version` 值，此时生效值取决于 profile 的激活顺序；始终只传入一个。
4. **垫片不会被发布。** `microsphere-resilience4j-compatible/pom.xml` 把 `maven-deploy-plugin` 配置为 `<skip>true</skip>`，因此一个从 Maven Central 解析 Resilience4j 1.3–1.6 的下游应用，在 Central 上是找不到 `microsphere-resilience4j-compatible` 的。它的存在是为了让*本* reactor 能够编译与测试；若要直接消费它，就得在本地构建它（`./mvnw -pl microsphere-resilience4j-compatible install -P resilience4j-1.3`）。

## CI 对 profile 的覆盖

`.github/workflows/maven-build.yml`（本分支）在推送到 `dev-1.x` 以及针对 `dev-1.x` / `release-1.x` 的 `pull_request` 上运行该矩阵：

| 矩阵维度 | 取值 |
|------------------|--------|
| `java` | `8`、`11`、`17`、`21`、`25` |
| `maven-profile-spring-cloud` | `spring-cloud-hoxton`、`spring-cloud-2020`、`spring-cloud-2021` |
| `maven-profile-resilience4j` | `resilience4j-1.3`、`resilience4j-1.4`、`resilience4j-1.5`、`resilience4j-1.6`、`resilience4j-1.7` |

命令为 `mvn --batch-mode --update-snapshots --file pom.xml -Drevision=0.0.1-SNAPSHOT test --activate-profiles test,coverage,<spring-cloud profile>,<resilience4j profile>`。

## 本版本线的 JDK 基线（已核验，附证据）

本版本线**没有** `<java.version>` 属性：在 `release-1.x` 目录树中搜索每一个 POM 都找不到，这与设置了 `17` 的 `0.2.x` 版本线 reactor POM 形成对照。基于源码可以陈述的是：

| 证据 | 值 |
|----------|-------|
| `.github/workflows/maven-build.yml` 矩阵 | JDK `8`、`11`、`17`、`21`、`25` 均会构建并测试该 reactor |
| `.github/workflows/maven-publish.yml` → `setup-java` | `deploy` 到 Maven Central 使用 JDK `11` |
| `release-notes.md`（v0.1.2，"Build and Workflow Enhancements"） | "Reverted to `javax.servlet` API and enforced Java 11 compatibility." |
| `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/pom.xml` | 依赖 `javax.servlet:javax.servlet-api`（Java EE 命名空间，而非 `jakarta.servlet`） |
| compiler/插件配置 | 继承自 `microsphere-spring-cloud-parent:0.1.26`，位于本仓库之外 |

实用解读：代码在 JDK 8 及以上完成编译与测试，发布构件在 JDK 11 上产出，而基于 `javax.servlet` 的技术栈意味着**仅支持 Spring Boot 2.x / Spring Cloud Hoxton – 2021.0.x**。若要得到确切的 `source`/`target` 取值，请在你自己的构建上运行 `./mvnw help:effective-pom`（该值来自上级 POM，而非本仓库）。

## 本版本线的版本事实

| 事实 | 值 | 来源 |
|------|-------|--------|
| 分支 | `release-1.x`（CI 构建 `dev-1.x`，发布 `release-1.x`） | workflow 触发器 |
| 版本线 | `0.1.x` | 根 `pom.xml` |
| 已发布版本 | `0.1.0`、`0.1.1`、`0.1.2`、`0.1.3`、`0.1.4` | git 标签 / `release-notes.md` |
| 当前 `${revision}` | `0.1.5-SNAPSHOT` | 根 `pom.xml` `<properties>` |
| Resilience4j 范围 | `1.3.0` – `1.7.0`，默认 `1.7.0` | 父工程 `<profiles>` |
| Spring Cloud 范围 | Hoxton – 2021.0.x | CI profile + 上级 `0.1.26` |
| 相对于 0.2.x 的额外 reactor 模块 | `microsphere-resilience4j-compatible` | 根 `pom.xml` `<modules>` |

## 相关页面

- [开发者指南 — microsphere-resilience4j-parent（1.x）](../../developer-guide/zh/microsphere-resilience4j-parent.md)
- [BOM：microsphere-resilience4j-dependencies（1.x）](microsphere-resilience4j-dependencies.md)
- [1.x 用户指南索引](README.md)
- [当前（0.2.x）父工程页面](../../../current/user-guide/zh/microsphere-resilience4j-parent.md)
