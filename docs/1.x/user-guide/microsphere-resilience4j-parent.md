# microsphere-resilience4j-parent — User Guide (1.x / v0.1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud Hoxton – 2021.0.x · JDK tested by CI: **8, 11, 17, 21, 25** · [← user guide index](README.md)

`microsphere-resilience4j-parent` is the shared **build parent** of the 1.x line. It is a `pom` artifact: you inherit it (or read it) rather than depend on it. This page tells you what it pins, and how a maintainer selects which Resilience4j 1.x version the whole reactor builds against.

## Coordinates

| Field | Value | Source |
|-------|-------|--------|
| groupId | `io.github.microsphere-projects` | `microsphere-resilience4j-parent/pom.xml` |
| artifactId | `microsphere-resilience4j-parent` | same |
| version | `${revision}` → `0.1.5-SNAPSHOT` on this branch snapshot; `0.1.4` was the last release | root `pom.xml` `<revision>`, git tag `0.1.4` |
| packaging | `pom` | module POM |
| name | `Microsphere :: Resilience4j :: Parent` | module POM |
| `<parent>` | `microsphere-resilience4j` (`${revision}`, `relativePath ../pom.xml`) | module POM |

## Inheritance chain

```
microsphere-resilience4j-parent        (this module, packaging pom)
   └── parent: microsphere-resilience4j        (reactor root POM, ${revision} = 0.1.5-SNAPSHOT)
            └── parent: microsphere-spring-cloud-parent : 0.1.26   (external grandparent)
```

The grandparent `0.1.26` is the Spring-Cloud-side parent for this line: it carries the compiler/build-plugin configuration and the `spring-cloud-hoxton` / `spring-cloud-2020` / `spring-cloud-2021` profiles that CI activates. It is **not** part of this repository snapshot, so its contents are referenced here only through the coordinates the POMs declare.

## What the parent declares

Reading `microsphere-resilience4j-parent/pom.xml` top to bottom, the file contains exactly three sections — `<properties>`, `<dependencyManagement>` and `<profiles>` — and nothing else (no `<dependencies>`, no `<build>`).

### 1. Managed microsphere integration versions

| Property | Value | Used to import |
|----------|-------|----------------|
| `microsphere-spring-cloud.version` | `0.1.26` | `microsphere-spring-cloud-dependencies` (`type=pom`, `scope=import`) |
| `microsphere-alibaba-druid.version` | `0.1.20` | `microsphere-alibaba-druid-dependencies` |
| `microsphere-mybatis.version` | `0.1.15` | `microsphere-mybatis-dependencies` |
| `microsphere-hibernate.version` | `0.1.9` | `microsphere-hibernate-dependencies` |
| `microsphere-redis.version` | `0.1.13` | `microsphere-redis-dependencies` |

These five imports are what let the module POMs declare `microsphere-hibernate-core`, `microsphere-mybatis-*`, Druid and Redis coordinates **without a version**. The `0.1.4` release is the commit that raised `microsphere-spring-cloud` to `0.1.26` (see `release-notes.md`: “Upgrade microsphere-spring-cloud to 0.1.26”).

### 2. Explicit Resilience4j version management (24 coordinates)

Unlike the `0.2.x` line, the 1.x parent does **not** import `io.github.resilience4j:resilience4j-bom`. It lists each artifact itself, all at `${resilience4j.version}`:

`resilience4j-all`, `resilience4j-bulkhead`, `resilience4j-cache`, `resilience4j-circuitbreaker`, `resilience4j-circularbuffer`, `resilience4j-consumer`, `resilience4j-feign`, `resilience4j-framework-common`, `resilience4j-kotlin`, `resilience4j-metrics`, `resilience4j-micrometer`, `resilience4j-prometheus`, `resilience4j-ratelimiter`, `resilience4j-ratpack`, `resilience4j-reactor`, `resilience4j-retrofit`, `resilience4j-retry`, `resilience4j-rxjava2`, `resilience4j-rxjava3`, `resilience4j-spring`, `resilience4j-spring-boot2`, `resilience4j-spring-cloud2`, `resilience4j-timelimiter`, `resilience4j-vertx`.

> **Two artifacts the modules use are NOT in this list:** `resilience4j-annotations` and `resilience4j-core` are declared (as `optional`) by the commons, compatible and plugin POMs **without a `<version>`**, and this parent never manages them. Their version therefore comes from one of the imported BOMs higher up (the `microsphere-spring-cloud-dependencies:0.1.26` chain), which is outside this repository. If you need the exact number that a given build resolved, run `./mvnw -P resilience4j-1.7 help:effective-pom` and look those coordinates up — do not assume it equals `${resilience4j.version}`.

### 3. Resilience4j version profiles

| Profile id | `resilience4j.version` | Activation | Extra effect on the reactor |
|------------|------------------------|------------|-----------------------------|
| `resilience4j-1.3` | `1.3.0` | explicit `-P` only | `microsphere-resilience4j-commons` also depends on `microsphere-resilience4j-compatible` |
| `resilience4j-1.4` | `1.4.0` | explicit `-P` only | same |
| `resilience4j-1.5` | `1.5.0` | explicit `-P` only | same |
| `resilience4j-1.6` | `1.6.0` | explicit `-P` only | same |
| `resilience4j-1.7` | `1.7.0` | **`activeByDefault = true`** | none — 1.7.0 already has the event classes the shim provides |

The per-profile dependency injection lives in `microsphere-resilience4j-commons/pom.xml`, not in the parent; the parent only sets the version property. See the [developer guide page](../developer-guide/microsphere-resilience4j-parent.md#profile-resolution-flowchart) for the flowchart.

## Picking a version profile (maintainer workflow)

You always build from the repository root (the reactor aggregator `microsphere-resilience4j`):

```bash
# Default build: no profile given -> 'resilience4j-1.7' (activeByDefault) -> Resilience4j 1.7.0
./mvnw clean install

# Build and test against an older 1.x line; the compatible shim is attached automatically
./mvnw clean install -P resilience4j-1.3     # or -1.4 / -1.5 / -1.6

# Combine with a Spring Cloud train profile (defined in the grandparent, not in this repo)
./mvnw clean install -P resilience4j-1.5,spring-cloud-hoxton

# Bypass the profiles entirely for a version that has no profile
./mvnw clean install -Dresilience4j.version=1.6.1
```

Rules that bite in practice:

1. **`activeByDefault` is suppressed once any other profile of the same effective model is activated.** A build that activates `spring-cloud-hoxton` **without** a `resilience4j-1.x` profile can leave `resilience4j.version` unset, and every managed `io.github.resilience4j:*` coordinate then has no version. This is exactly why `.github/workflows/maven-build.yml` always passes both a `maven-profile-spring-cloud` and a `maven-profile-resilience4j` value.
2. **Overriding by property is fragile.** `resilience4j.version` is declared **only** inside these profiles (there is no `<resilience4j.version>` in any `<properties>` block of this line), so `-Dresilience4j.version=1.6.1` is a valid idea — but any profile that is active in the same build also contributes the property, and a profile value shadows the command line. If you must use a version with no dedicated profile, deactivate the default one explicitly:
   ```bash
   ./mvnw clean install -P '!resilience4j-1.7' -Dresilience4j.version=1.6.1
   ```
   Otherwise prefer the five built-in profiles; they are the combinations CI actually verifies.
3. **`-P resilience4j-1.3 … 1.6` changes the dependency graph, not just the version** — the shim jar appears on the classpath of everything downstream of commons (it is added as a plain, non-optional `compile` dependency). Activating two `resilience4j-*` profiles in the same build merges two different values for `resilience4j.version`, and the effective value then depends on profile activation order; always pass exactly one.
4. **The shim is not published.** `microsphere-resilience4j-compatible/pom.xml` configures `maven-deploy-plugin` with `<skip>true</skip>`, so a downstream application that resolves Resilience4j 1.3–1.6 from Maven Central will not find `microsphere-resilience4j-compatible` there. It exists to make *this* reactor compile and test; consuming it directly means building it locally (`./mvnw -pl microsphere-resilience4j-compatible install -P resilience4j-1.3`).

## CI coverage of the profiles

`.github/workflows/maven-build.yml` (this branch) runs the matrix on `push` to `dev-1.x` and `pull_request` to `dev-1.x` / `release-1.x`:

| Matrix dimension | Values |
|------------------|--------|
| `java` | `8`, `11`, `17`, `21`, `25` |
| `maven-profile-spring-cloud` | `spring-cloud-hoxton`, `spring-cloud-2020`, `spring-cloud-2021` |
| `maven-profile-resilience4j` | `resilience4j-1.3`, `resilience4j-1.4`, `resilience4j-1.5`, `resilience4j-1.6`, `resilience4j-1.7` |

The command is `mvn --batch-mode --update-snapshots --file pom.xml -Drevision=0.0.1-SNAPSHOT test --activate-profiles test,coverage,<spring-cloud profile>,<resilience4j profile>`.

## JDK baseline on this line (verified, with evidence)

This line has **no** `<java.version>` property: searching every POM in the `release-1.x` tree returns none, in contrast to the `0.2.x` line whose reactor POM sets `17`. What can be stated from source:

| Evidence | Value |
|----------|-------|
| `.github/workflows/maven-build.yml` matrix | JDK `8`, `11`, `17`, `21`, `25` all build and test the reactor |
| `.github/workflows/maven-publish.yml` → `setup-java` | JDK `11` is used for the `deploy` to Maven Central |
| `release-notes.md` (v0.1.2, “Build and Workflow Enhancements”) | “Reverted to `javax.servlet` API and enforced Java 11 compatibility.” |
| `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/pom.xml` | depends on `javax.servlet:javax.servlet-api` (Java EE namespace, not `jakarta.servlet`) |
| compiler/plugin configuration | inherited from `microsphere-spring-cloud-parent:0.1.26`, outside this repository |

Practical reading: the code is compiled and tested on JDK 8 and up, the release artifact is produced on JDK 11, and the `javax.servlet`-based stack means **Spring Boot 2.x / Spring Cloud Hoxton – 2021.0.x only**. For a definitive `source`/`target` pair, run `./mvnw help:effective-pom` on your own build (the value comes from the grandparent POM, not from this repository).

## Version facts on this line

| Fact | Value | Source |
|------|-------|--------|
| Branch | `release-1.x` (CI builds `dev-1.x`, publishes `release-1.x`) | workflow triggers |
| Version line | `0.1.x` | root `pom.xml` |
| Released versions | `0.1.0`, `0.1.1`, `0.1.2`, `0.1.3`, `0.1.4` | git tags / `release-notes.md` |
| Current `${revision}` | `0.1.5-SNAPSHOT` | root `pom.xml` `<properties>` |
| Resilience4j range | `1.3.0` – `1.7.0`, default `1.7.0` | parent `<profiles>` |
| Spring Cloud range | Hoxton – 2021.0.x | CI profiles + grandparent `0.1.26` |
| Extra reactor module vs 0.2.x | `microsphere-resilience4j-compatible` | root `pom.xml` `<modules>` |

## Related pages

- [Developer guide — microsphere-resilience4j-parent (1.x)](../developer-guide/microsphere-resilience4j-parent.md)
- [BOM: microsphere-resilience4j-dependencies (1.x)](microsphere-resilience4j-dependencies.md)
- [1.x user guide index](README.md)
- [Current (0.2.x) parent page](../../current/user-guide/microsphere-resilience4j-parent.md)
