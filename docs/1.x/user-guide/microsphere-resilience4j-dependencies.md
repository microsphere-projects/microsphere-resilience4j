# microsphere-resilience4j-dependencies — User Guide (1.x / v0.1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · manages **6** artifacts · JDK tested by CI: 8, 11, 17, 21, 25 · [← user guide index](README.md)

`microsphere-resilience4j-dependencies` is the **BOM** of the 1.x line. It is a `pom` artifact you import in `<dependencyManagement>` so that your application can depend on the microsphere-resilience4j modules **without writing a version for each one**.

## Coordinates

| Field | Value | Source |
|-------|-------|--------|
| groupId | `io.github.microsphere-projects` | `microsphere-resilience4j-dependencies/pom.xml` |
| artifactId | `microsphere-resilience4j-dependencies` | same |
| version | `${revision}` → published as `0.1.0` … `0.1.4` | same + git tags |
| packaging | `pom` | same |
| name | `Microsphere :: Resilience4j :: Dependencies` | same |
| `<parent>` | `microsphere-resilience4j-parent` (`${revision}`, `relativePath ../microsphere-resilience4j-parent/pom.xml`) | same |

## Artifacts managed by the BOM (complete, verified list)

Reading the `<dependencyManagement>` block of `microsphere-resilience4j-dependencies/pom.xml`, it contains **exactly six** entries, all `io.github.microsphere-projects` and all pinned to `${revision}` (i.e. the same version as the BOM itself once it is published):

| # | Managed artifactId | Version in the BOM | What you get from it |
|---|--------------------|--------------------|----------------------|
| 1 | `microsphere-resilience4j-commons` | `${revision}` | framework-neutral templates, facade, `Resilience4jContext` |
| 2 | `microsphere-resilience4j-alibaba-druid` | `${revision}` | Druid `Filter`-based plugin |
| 3 | `microsphere-resilience4j-openfeign` | `${revision}` | OpenFeign plugin |
| 4 | `microsphere-resilience4j-mybatis` | `${revision}` | MyBatis `Executor` plugin |
| 5 | `microsphere-resilience4j-spring-web` | `${revision}` | Spring Web handler-interceptor plugin |
| 6 | `microsphere-resilience4j-spring` | `${revision}` | Spring `@Enable*` annotations, event bridge, plugin SPI |

The order above is the order in the file (`commons`, `alibaba-druid`, `openfeign`, `mybatis`, `spring-web`, `spring`) — there is no seventh entry.

> **The 1.x and 0.2.x BOMs manage the same six artifacts.** The lists are identical (verified by comparing both files), so the artifact *names* are a stable contract across the two version lines; what differs between the lines is the version you import (`0.1.4` vs `0.2.4`) and the Resilience4j / Spring stack behind it.

## What the BOM does NOT manage (verified)

| Artifact | Built in the reactor? | In the BOM? | Consequence |
|----------|-----------------------|-------------|-------------|
| `microsphere-resilience4j-hibernate-core` | yes (`microsphere-resilience4j-plugins/pom.xml`) | **no** | you must state its version yourself |
| `microsphere-resilience4j-compatible` | yes (root `<modules>`) | **no** | build-time shim only; its own POM also sets `maven-deploy-plugin <skip>true</skip>`, so it is not deployed |
| `microsphere-resilience4j-spring-boot` | yes | **no** | placeholder: `packaging` is `jar` but the module contains only a `pom.xml` (no `src/`) |
| `microsphere-resilience4j-spring-cloud` | yes | **no** | placeholder: `packaging` is `jar` but the module contains only a `pom.xml` (no `src/`) |
| `microsphere-resilience4j-plugins` | yes | **no** | aggregator `pom`, never a dependency |
| `microsphere-resilience4j` (root), `microsphere-resilience4j-parent` | yes | **no** | reactor root / build parent — used as `<parent>`, not imported |
| third-party Resilience4j / Hibernate / MyBatis / Druid / Redis | — | not in the BOM itself | managed **indirectly**: the BOM inherits `microsphere-resilience4j-parent`, whose `<dependencyManagement>` lists 24 `io.github.resilience4j:*` coordinates at `${resilience4j.version}` and imports `microsphere-spring-cloud-dependencies:0.1.26`, `-alibaba-druid-dependencies:0.1.20`, `-mybatis-dependencies:0.1.15`, `-hibernate-dependencies:0.1.9`, `-redis-dependencies:0.1.13` |

> **How much of that inherited management reaches *your* project is not something you should rely on.** The six first-party entries are declared in the BOM file itself; the framework coordinates above live in the POM the BOM *inherits*, and what an `<scope>import</scope>` brings across depends on Maven's resolution of the imported model and on how the released POM was flattened at deploy time. Treat this BOM as the version source for the six first-party artifacts only, pin the framework versions your application actually needs, and confirm the result in your own project:
>
> ```bash
> ./mvnw -f your-project/pom.xml help:effective-pom | grep -A2 "resilience4j"
> ./mvnw -f your-project/pom.xml dependency:tree
> ```

## How to consume it

### Step 1 — import the BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.1.4</version>          <!-- latest release on the 0.1.x line -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 2 — declare the modules you need, without versions

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

### Step 3 — supply the versions the BOM does not cover

The plugin modules declare Resilience4j, Spring, Hibernate, MyBatis and Druid as `optional`, so **your application owns those versions**. The safest way to keep them consistent with the 1.x line is to import a Spring Boot / Spring Cloud BOM from the Hoxton – 2021.0.x range plus, if you use Resilience4j directly, the `io.github.resilience4j` coordinates you need:

```xml
<dependencyManagement>
    <dependencies>
        <!-- example: Boot 2.7.x pairs with Spring Cloud 2021.0.x; choose the Boot line that
             matches your own Spring Cloud train (Hoxton -> Boot 2.2/2.3, 2020 -> 2.4/2.5).
             Import order matters: your own entries first. -->
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
    <!-- Resilience4j modules: the 0.1.x line targets 1.3.0 - 1.7.0, default 1.7.0 -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>1.7.0</version>
    </dependency>
</dependencies>
```

Pin one Resilience4j 1.x version and stay inside `1.3.0`–`1.7.0`; the sources of this line are written against the 1.x `Registry` / `EventPublisher` APIs and will not compile against 2.x.

## Pinning the Hibernate plugin explicitly

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.1.4</version>   <!-- NOT managed by the BOM -->
</dependency>
```

`microsphere-resilience4j-hibernate-core` is a real module of the 1.x reactor but is missing from this BOM, so the BOM import will not fill in a version for it — an unversioned declaration fails with a `'dependencies.dependency.version' is missing` build error. Add either the `<version>` above or a matching `<dependencyManagement>` entry.

## Overriding a single managed version

Your own `<dependencyManagement>` entry wins if it appears **before** the BOM import; an explicit `<version>` on the dependency itself always wins:

```xml
<dependencyManagement>
    <dependencies>
        <!-- evaluated first, takes precedence -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-commons</artifactId>
            <version>0.1.5-SNAPSHOT</version>
        </dependency>
        <!-- BOM supplies the rest -->
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

Do not mix `0.1.x` and `0.2.x` artifacts in one application: the version lines target incompatible Resilience4j majors (1.x vs 2.x) and different Spring stacks (Hoxton–2021 vs 2022–2025).

## How a maintainer pairs the BOM with a version profile

The BOM is *published from* a reactor build, so the profile chosen at build time decides which Resilience4j line the six artifacts were compiled and tested against — and, for `1.3`–`1.6`, whether the `compatible` shim was on the compile classpath:

```bash
# release-style build: Resilience4j 1.7.0, no shim
./mvnw clean deploy -Drevision=0.1.4 -P resilience4j-1.7

# same BOM content, compiled against Resilience4j 1.5.0 (shim attached automatically)
./mvnw clean install -P resilience4j-1.5
```

`${revision}` is a single property in the reactor root (`0.1.5-SNAPSHOT` on this snapshot); `-Drevision=…` at deploy time stamps both the BOM and the six managed entries with the same concrete version. The published workflow enforces the format (`^[0-9]+\.[0-9]+\.[0-9]+$`) and then bumps the root `<revision>` to the next patch snapshot, which is why this branch reads `0.1.5-SNAPSHOT` after the `0.1.4` release.

## Version facts on this line

| Fact | Value |
|------|-------|
| BOM version to import | `0.1.4` (latest release; `0.1.5-SNAPSHOT` is unreleased) |
| Managed first-party artifacts | 6 (identical list to the `0.2.x` BOM) |
| Not managed | `hibernate-core` plugin, `compatible`, `spring-boot`, `spring-cloud`, aggregators/parents |
| Resilience4j line the artifacts were built against | `1.3.0` – `1.7.0`, default `1.7.0` (Maven profiles) |
| Spring Cloud | Hoxton – 2021.0.x (CI profiles `spring-cloud-hoxton`, `-2020`, `-2021`) |
| JDK | no `java.version` property on this line; CI builds JDK 8/11/17/21/25, publishing runs on JDK 11 |

## Related pages

- [Developer guide — microsphere-resilience4j-dependencies (1.x)](../developer-guide/microsphere-resilience4j-dependencies.md)
- [Build parent — microsphere-resilience4j-parent (1.x)](microsphere-resilience4j-parent.md)
- [1.x user guide index](README.md)
- [Current (0.2.x) BOM page](../../current/user-guide/microsphere-resilience4j-dependencies.md)
