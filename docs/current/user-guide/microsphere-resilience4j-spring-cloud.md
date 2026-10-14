# microsphere-resilience4j-spring-cloud — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · **status: empty placeholder (no code shipped)** · JDK **17+** · [← guide index](README.md)

## TL;DR — this module ships no functionality yet

`microsphere-resilience4j-spring-cloud` is a **placeholder**. Its directory contains **only** a `pom.xml`; there is no `src/` tree (verified by directory listing of `microsphere-resilience4j-spring-cloud/`). Packaging is `jar`, so a build produces an empty jar with no classes and no resources.

> **Do not add this dependency expecting Spring Cloud integration.** Today it does nothing. Resilience4j behavior in a Spring context comes from [`microsphere-resilience4j-spring`](microsphere-resilience4j-spring.md).

## Coordinates

| Field | Value |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-spring-cloud` |
| version | `${revision}` |
| packaging | `jar` |
| name | `Microsphere :: Resilience4j :: Spring Cloud` |
| parent | `microsphere-resilience4j-parent` |
| `<properties>` | (empty block — declares nothing) |
| `<dependencies>` | **none declared in this POM** |

## How the module is (and is not) wired in

It is a member of the build reactor (so it is built/released with the project) but is otherwise fully isolated:

| Where it appears | Reference |
|------------------|-----------|
| Root reactor `<modules>` | yes — `microsphere-resilience4j-spring-cloud` is listed |
| Its own `pom.xml` | yes |
| `microsphere-resilience4j-dependencies` (BOM) | **no** — not version-managed by the BOM |
| Any other module's `<dependencies>` | **no** — nothing depends on it |
| `README.md` module table | **no** — not listed |
| `release-notes.md` | **no** — no feature entry mentions it |

A repository-wide search for the artifactId returns hits only in the root `pom.xml` `<modules>` entry and in the module's own `pom.xml`, confirming it is referenced nowhere else.

## Why the placeholder exists

The coordinate, the `jar` packaging, and the inheritance from `microsphere-resilience4j-parent` (which brings the Resilience4j version profiles and the Spring Cloud / framework BOMs) indicate that this module **reserves `io.github.microsphere-projects:microsphere-resilience4j-spring-cloud`** for a future Spring Cloud layer — the natural home for things like externalized `Resilience4jRegistry` configuration from Spring Cloud Config, circuit-breaker actuator/health bindings, or Spring Cloud-specific decorators.

> Accuracy note: **no source file, README entry, or release note documents this rationale.** The statement is an inference from packaging, coordinates and naming only. Nothing in the `release` snapshot guarantees such a module is scheduled — read it as "reserved, not promised."

## What it would take to implement (user-facing summary)

If it becomes real, Spring Cloud consumers would likely expect:

1. A `src/main/java` configuration layer activated by Spring Cloud (`org.springframework.cloud:*` dependencies, version-supplied by the inherited `microsphere-spring-cloud-dependencies:0.2.26`).
2. `spring.factories` / `AutoConfiguration.imports` wiring so registries and decorators are configured from Spring Cloud properties.
3. A dependency on `microsphere-resilience4j-spring` (and transitively `-commons`).
4. Registration of the artifact in the `microsphere-resilience4j-dependencies` BOM so its version can be omitted.

Until an actual release ships this, rely on `microsphere-resilience4j-spring`. The [developer guide page](../developer-guide/microsphere-resilience4j-spring-cloud.md) documents the reactor position and the implementation checklist in detail.

## How to tell if a future release is real

This module already publishes an (empty) jar on the `0.2.x` line, so "it exists on Maven Central" does not mean it works. Before depending on it:

1. **Check the BOM.** Appearance in `microsphere-resilience4j-dependencies` marks the move from placeholder to supported coordinate.
2. **Check the jar contents.** A real Spring Cloud layer ships classes and configuration under `io/microsphere/resilience4j/spring/cloud/...`; the `0.2.4` jar contains none.
3. **Check the docs.** README's module table and `release-notes.md` do not mention this module today.

## FAQ

- **"Does it read Resilience4j config from Spring Cloud Config?"** No — the jar is empty; wire registries yourself via `microsphere-resilience4j-spring`.
- **"Why keep an empty module in the build?"** It reserves the coordinate and stays in the reactor `<modules>`, so it is built and released automatically with every version.
- **"Will it need its own Spring Cloud version profile?"** No — the `spring-cloud-2022…2025` profiles are inherited from `microsphere-spring-cloud-parent:0.2.26`, already on the classpath of the parent.

## Version facts on this line

| Fact | Value |
|------|-------|
| Version line | `0.2.x` (`release` branch) |
| `${revision}` in the reactor POM | `0.2.5-SNAPSHOT` |
| Latest release | `0.2.4` — the placeholder jar exists on this line but is empty |
| Spring Cloud range (via parent) | `2022.0.x` – `2025.0.x` (`microsphere-spring-cloud-parent` `0.2.26`) |
| JDK | `17+` |
