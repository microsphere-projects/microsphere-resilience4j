# microsphere-resilience4j-spring-boot — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · **status: empty placeholder (no code shipped)** · JDK **17+** · [← guide index](README.md)

## TL;DR — this module ships no functionality yet

`microsphere-resilience4j-spring-boot` is a **placeholder**. Its directory contains **only** a `pom.xml`; there is no `src/` tree (verified by directory listing of `microsphere-resilience4j-spring-boot/`). A `mvn package` on this module produces a jar with no classes and no resources.

> **Do not add this dependency to a project expecting behavior.** Today it contributes nothing. For Spring integration use [`microsphere-resilience4j-spring`](microsphere-resilience4j-spring.md) instead.

## Coordinates

| Field | Value |
|-------|-------|
| groupId | `io.github.microsphere-projects` |
| artifactId | `microsphere-resilience4j-spring-boot` |
| version | `${revision}` |
| packaging | `jar` |
| name | `Microsphere :: Resilience4j :: Spring Boot` |
| parent | `microsphere-resilience4j-parent` |
| `<properties>` | (empty block — declares nothing) |
| `<dependencies>` | **none declared in this POM** |

## How the module is (and is not) wired in

The module is a real member of the build reactor and is therefore built and released with the rest of the project, but it is intentionally isolated:

| Where it appears | Reference |
|------------------|-----------|
| Root reactor `<modules>` | yes — `microsphere-resilience4j-spring-boot` is listed |
| Its own `pom.xml` | yes |
| `microsphere-resilience4j-dependencies` (BOM) | **no** — not version-managed by the BOM |
| Any other module's `<dependencies>` | **no** — nothing depends on it |
| `README.md` module table | **no** — not listed |
| `release-notes.md` | **no** — no feature entry mentions it |

A repository-wide search for the artifactId returns hits only in the root `pom.xml` `<modules>` entry and in the module's own `pom.xml`, confirming it is referenced nowhere else.

## Why the placeholder exists

Because the coordinate is defined, inherits the build parent (Resilience4j profiles + framework BOMs), and is packaged as `jar`, its presence **reserves the Maven coordinate `io.github.microsphere-projects:microsphere-resilience4j-spring-boot`** for a future Spring Boot auto-configuration module — the conventional companion to `microsphere-resilience4j-spring` that would provide `@EnableAutoConfiguration`-style setup.

> Accuracy note: **no source file, README entry, or release note documents this rationale.** The statement above is an inference from the module's packaging/coordinates and naming convention. Nothing in the `release` snapshot proves an auto-configuration is scheduled, so treat it as "reserved, not promised."

## What it would take to implement (user-facing summary)

If and when this module becomes real, Spring Boot consumers would expect roughly:

1. `src/main/java` auto-configuration classes (e.g. `Resilience4jAutoConfiguration`) plus a `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registration file.
2. A dependency on `org.springframework.boot:spring-boot-autoconfigure` (version supplied by the `microsphere-spring-cloud-dependencies` / Spring Boot BOM line the parent inherits).
3. Compile-scope wiring to `microsphere-resilience4j-spring` / `-commons` so that `@EnableResilience4j` behavior is offered through Boot configuration properties.
4. Registration of the artifact in the `microsphere-resilience4j-dependencies` BOM so consumers can omit its version.

Until a release actually ships these, keep using `microsphere-resilience4j-spring` and the `@Enable*` annotations directly. See the [developer guide page](../developer-guide/microsphere-resilience4j-spring-boot.md) for the reactor position and the full implementation checklist.

## How to tell if a future release is real

Because this module already publishes an (empty) jar on the `0.2.x` line, "the artifact exists on Maven Central" is **not** proof it does anything. Before depending on it, verify the release actually added content:

1. **Check the BOM.** Once `microsphere-resilience4j-spring-boot` appears in `microsphere-resilience4j-dependencies`, it has been promoted from a placeholder to a supported coordinate.
2. **Check the jar.** An implemented module ships classes under `io/microsphere/resilience4j/spring/boot/...` and a `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` entry. The current `0.2.4` jar contains none of these.
3. **Check the release notes / README.** The `release` snapshot's README module table and `release-notes.md` do not mention this module — a real implementation would normally add an entry.

## FAQ

- **"I added the dependency and nothing happens."** Expected — the jar is empty. Use `microsphere-resilience4j-spring` and `@EnableResilience4j` today.
- **"Is it going away?"** It is kept in the reactor, so it is built and released; there is no documented plan to remove it.
- **"Why is it built at all if it does nothing?"** It is a reactor `<module>`, so it is packaged with every build; the empty jar is the harmless by-product.

## Version facts on this line

| Fact | Value |
|------|-------|
| Version line | `0.2.x` (`release` branch) |
| `${revision}` in the reactor POM | `0.2.5-SNAPSHOT` |
| Latest release | `0.2.4` — the placeholder jar exists on this line but is empty |
| JDK | `17+` (inherited from the reactor POM / CI) |
