# microsphere-resilience4j-parent — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← guide index](README.md)

This page is for consumers who **build the library from source** (or build their own modules on top of it) and need to understand how the shared **build parent** selects a Resilience4j version. It is a build-time POM — you inherit it, you do not add it as a runtime dependency.

## Coordinates

| Field | Value |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-parent` |
| version | `${revision}` (resolves to the reactor version, e.g. `0.2.4`) |
| packaging | `pom` |
| name | `Microsphere :: Resilience4j :: Parent` |

Every code module in the reactor (`commons`, `spring`, the `plugins/*` leaves and the `plugins` aggregator, `dependencies`, `spring-boot`, `spring-cloud`) declares `microsphere-resilience4j-parent` as its `<parent>`, so they all pick up the same dependency management and the same version profiles.

## What the parent configures

The parent module POM declares **only three things** (verified in `microsphere-resilience4j-parent/pom.xml`): a set of version properties, a `<dependencyManagement>` block of imported BOMs, and the Resilience4j version `<profiles>`. All build plugins, `pluginManagement`, compiler settings, and the flatten/CI-friendly-version machinery are **inherited from the grandparent** — see the inheritance chain below — and are not re-declared here.

### Inheritance chain

```
microsphere-resilience4j-parent        (this module, packaging pom)
   └── parent: microsphere-resilience4j          (the reactor root pom)
                   └── parent: microsphere-spring-cloud-parent : 0.2.26   (external grandparent)
```

The parent module's `<parent>` is the root reactor POM `microsphere-resilience4j`; that root POM in turn inherits from `io.github.microsphere-projects:microsphere-spring-cloud-parent:0.2.26`, which is where the JDK / Spring Cloud build configuration comes from.

### Managed microsphere dependency versions (properties)

| Property | Version | Used to import |
|----------|---------|----------------|
| `microsphere-spring-cloud.version` | `0.2.26` | `microsphere-spring-cloud-dependencies` |
| `microsphere-alibaba-druid.version` | `0.2.20` | `microsphere-alibaba-druid-dependencies` |
| `microsphere-mybatis.version` | `0.2.16` | `microsphere-mybatis-dependencies` |
| `microsphere-hibernate.version` | `0.2.10` | `microsphere-hibernate-dependencies` |
| `microsphere-redis.version` | `0.2.14` | `microsphere-redis-dependencies` |

These five BOMs, plus `io.github.resilience4j:resilience4j-bom` and `io.github.resilience4j:resilience4j-framework-common`, are pulled into `<dependencyManagement>` with `<scope>import</scope>` (resilience4j BOM) or a managed version (`resilience4j-framework-common` = `${resilience4j.version}`). They pin the versions of every framework the plugins integrate with; they do **not** add runtime dependencies to a module that merely inherits this parent.

> The annotation processor artifact (`microsphere-annotation-processor`) used by the generated-spring-metadata modules is declared per-module in the leaf POMs and version-managed by the imported `microsphere-*` BOMs — it is not declared inside this parent POM.

## Resilience4j version profiles

Each profile only sets `resilience4j.version`; the source is written against the stable 2.x API surface so it compiles under any of them.

| Profile id | `resilience4j.version` | Activation |
|------------|------------------------|------------|
| `resilience4j-2.0` | `2.0.0` | explicit `-P` only |
| `resilience4j-2.1` | `2.1.0` | explicit `-P` only |
| `resilience4j-2.2` | `2.2.0` | explicit `-P` only |
| `resilience4j-2.3` | `2.3.0` | explicit `-P` only |
| `resilience4j-2.4` | `2.4.0` | `activeByDefault = true` |

## Picking a Resilience4j version when building from source

The root POM (`${revision}`, `java.version=17`) is the reactor aggregator; you build from the repository root.

```bash
# Default — no profile passed: the activeByDefault 'resilience4j-2.4' profile wins
./mvnw clean install

# Pin a specific Resilience4j line
./mvnw clean install -P resilience4j-2.0   # or -2.1 / -2.2 / -2.3
./mvnw clean install -P resilience4j-2.4   # same result as the default
```

Two practical caveats:

1. **`activeByDefault` is switched off as soon as any other profile in the same POM is activated.** If you activate a profile that also declares its own `resilience4j.version`, the default `2.4.0` will *not* apply. To be unambiguous, always pass an explicit `resilience4j-2.x` profile.
2. **Override without a profile:** you can bypass the profiles entirely by setting the property directly, e.g. `./mvnw clean install -Dresilience4j.version=2.2.0`. This is useful for a version that has no dedicated profile.

See the [developer guide page](../developer-guide/microsphere-resilience4j-parent.md) for the profile-resolution flowchart and how the GitHub Actions CI matrix drives these profiles.

## Version facts on this line

| Fact | Value | Source |
|------|-------|--------|
| Version line | `0.2.x` | branch `release` |
| Latest release | `0.2.4` | reactor / published artifacts |
| Current `${revision}` | `0.2.5-SNAPSHOT` | root `pom.xml` `<properties>` |
| Resilience4j range | `2.0.0` – `2.4.0` (default `2.4.0`) | parent `<profiles>` |
| Spring Cloud range | `2022.0.x` – `2025.0.x` | via `microsphere-spring-cloud-parent` `0.2.26` |
| JDK | `17` baseline (`java.version=17`; CI builds on 17/21/25) | root `pom.xml`, `.github/workflows` |

> Note: the JDK baseline for this `0.2.x` line is **17**, taken from `<java.version>17</java.version>` in the reactor POM and the CI matrix (17/21/25). The `0.1.x` / `1.x` line is the one that targets older JDKs.
