# microsphere-resilience4j-dependencies — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · manages **6** artifacts · JDK **17+** · [← guide index](README.md)

`microsphere-resilience4j-dependencies` is the project **BOM** (Bill of Materials). It is a `pom` artifact you import in `<dependencyManagement>` so that you can depend on the microsphere-resilience4j modules **without writing a version for each one**.

## Coordinates

| Field | Value |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-dependencies` |
| version | `${revision}` (published as e.g. `0.2.4`) |
| packaging | `pom` |
| name | `Microsphere :: Resilience4j :: Dependencies` |
| parent | `microsphere-resilience4j-parent` (so it inherits the Resilience4j version profiles and the imported framework BOMs) |

## Artifacts managed by the BOM

Every entry below is `io.github.microsphere-projects` and is pinned to `${revision}` — i.e. the same release version as the BOM itself. This is the **complete, verified** list from `microsphere-resilience4j-dependencies/pom.xml` (exactly six artifacts):

| # | Managed artifactId | Version in BOM | Purpose |
|---|--------------------|----------------|---------|
| 1 | `microsphere-resilience4j-commons` | `${revision}` | framework-neutral templates / facade core |
| 2 | `microsphere-resilience4j-spring` | `${revision}` | Spring `@Enable*` annotations, event bridge, plugin SPI |
| 3 | `microsphere-resilience4j-alibaba-druid` | `${revision}` | Druid SQL-pool plugin |
| 4 | `microsphere-resilience4j-mybatis` | `${revision}` | MyBatis `Executor` plugin |
| 5 | `microsphere-resilience4j-openfeign` | `${revision}` | OpenFeign plugin |
| 6 | `microsphere-resilience4j-spring-web` | `${revision}` | Spring Web MVC plugin |

> **What the BOM does NOT manage (verified):** `microsphere-resilience4j-hibernate-core`, the `microsphere-resilience4j-plugins` aggregator, and the placeholder modules `microsphere-resilience4j-spring-boot` / `microsphere-resilience4j-spring-cloud` are **absent** from this `<dependencyManagement>`. Most notably, `microsphere-resilience4j-hibernate-core` is built in the reactor but is not version-managed by the BOM — see the gotcha below.

## How to use it (import scope)

Add the BOM once in `<dependencyManagement>`, then list the modules you need **without** versions:

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

## Overriding a single version

A BOM entry is only a *default*; your own `<dependencyManagement>` entry wins if it appears **before** the BOM import. To force a specific module to a different version (for example a locally-built snapshot), declare it ahead of the import:

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

You can also just set an explicit `<version>` on the `<dependencies>` entry itself; a direct version on the dependency always overrides the managed default.

## Gotcha: the Hibernate plugin is not in the BOM

`microsphere-resilience4j-hibernate-core` is a real, published module (it appears in the reactor under `microsphere-resilience4j-plugins`), but it is **not** listed in this BOM. If you want the Hibernate plugin you must supply its version yourself — the BOM import will not fill it in:

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.2.4</version>   <!-- NOT managed by the BOM -->
</dependency>
```

The Hibernate *third-party* stack (`org.hibernate` artifacts), on the other hand, is version-managed indirectly: the parent POM imports `io.github.microsphere-projects:microsphere-hibernate-dependencies:0.2.10`. Only the microsphere-resilience4j Hibernate **plugin** coordinate is missing from the BOM.

## Version facts on this line

| Fact | Value |
|------|-------|
| BOM version to import | `0.2.4` (latest release on the `0.2.x` line) |
| Managed microsphere-resilience4j artifacts | 6 (see table above) |
| Resilience4j / Spring versions | inherited via `microsphere-resilience4j-parent` (`0.2.26` Spring Cloud line) |
| JDK | `17+` |

See the [developer guide page](../developer-guide/microsphere-resilience4j-dependencies.md) for how the BOM is generated and how `${revision}` is flattened at publish time.
