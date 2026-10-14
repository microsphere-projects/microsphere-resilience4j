# Microsphere Resilience4j

> Microsphere Projects for [Resilience4j](https://github.com/resilience4j/microsphere-resilience4j)

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/microsphere-projects/microsphere-resilience4j)
[![Maven Build](https://github.com/microsphere-projects/microsphere-resilience4j/actions/workflows/maven-build.yml/badge.svg)](https://github.com/microsphere-projects/microsphere-resilience4j/actions/workflows/maven-build.yml)
[![Codecov](https://codecov.io/gh/microsphere-projects/microsphere-resilience4j/branch/main/graph/badge.svg)](https://app.codecov.io/gh/microsphere-projects/microsphere-resilience4j)
![Maven](https://img.shields.io/maven-central/v/io.github.microsphere-projects/microsphere-resilience4j.svg)
![License](https://img.shields.io/github/license/microsphere-projects/microsphere-resilience4j.svg)

Microsphere Resilience4j is an extension framework that enhances the capabilities of the Resilience4j library by
providing seamless integration with popular Java frameworks and libraries. It offers a unified approach to implementing
fault tolerance patterns across different parts of an application.

The project aims to simplify the application of resilience patterns such as Circuit Breaker, Bulkhead, Rate Limiter,
Retry, and Time Limiter in various contexts including:

- Spring Web MVC applications
- API clients (Feign)
- Database access (MyBatis, JDBC with Druid and P6Spy)

## Modules

| **Module**                                | **Purpose**                                                            |
|-------------------------------------------|------------------------------------------------------------------------|
| **microsphere-resilience4j-parent**       | Defines the parent POM with dependency management and version profiles |
| **microsphere-resilience4j-dependencies** | Centralizes dependency management for all project modules              |
| **microsphere-resilience4j-commons**      | Common featurues of Resilience4j extension                             |
| **microsphere-resilience4j-plugins**      | The plugins of Resilience4j                                            |
| **microsphere-resilience4j-spring**       | Integration for Resilience4j Spring                                    |

## Getting Started

The easiest way to get started is by adding the Microsphere Resilience4j BOM (Bill of Materials) to your project's
pom.xml:

```xml

<dependencyManagement>
    <dependencies>
        ...
        <!-- Microsphere Resilience4j Dependencies -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>${microsphere-resilience4j.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        ...
    </dependencies>
</dependencyManagement>
```

`${microsphere-resilience4j.version}` has two branches:

| **Branches** | **Purpose**                                      | **Latest Version** |
|--------------|--------------------------------------------------|--------------------|
| **main**     | Compatible with Spring Cloud 2022.0.x - 2025.0.x |   `0.2.4`          |
| **1.x**      | Compatible with Spring Cloud Hoxton - 2021.0.x   |   `0.1.4`          |

## Building from Source

You don't need to build from source unless you want to try out the latest code or contribute to the project.

To build the project, follow these steps:

1. Clone the repository:

```bash
git clone https://github.com/microsphere-projects/microsphere-resilience4j.git
```

2. Build the source:

- Linux/MacOS:

```bash
./mvnw package
```

- Windows:

```powershell
mvnw.cmd package
```

## Contributing

We welcome your contributions! Please read [Code of Conduct](./CODE_OF_CONDUCT.md) before submitting a pull request.

## Reporting Issues

* Before you log a bug, please search
  the [issues](https://github.com/microsphere-projects/microsphere-resilience4j/issues)
  to see if someone has already reported the problem.
* If the issue doesn't already
  exist, [create a new issue](https://github.com/microsphere-projects/microsphere-resilience4j/issues/new).
* Please provide as much information as possible with the issue report.

## Documentation

The full bilingual documentation set lives in the [`docs/`](docs/README.md) directory
(中文索引见 [docs/zh/README.md](docs/zh/README.md)), covering the `release` branch (as [`current`](docs/current/user-guide/README.md))
and the `release-1.x` branch (as [`1.x`](docs/1.x/user-guide/README.md)).

### User Guide

- Current line (v0.2.x, Resilience4j 2.0.0–2.4.0): [English](docs/current/user-guide/README.md) · [中文](docs/current/user-guide/zh/README.md)
- 1.x line (v0.1.x, Resilience4j 1.3.0–1.7.0): [English](docs/1.x/user-guide/README.md) · [中文](docs/1.x/user-guide/zh/README.md)
- Online: [DeepWiki Host](https://deepwiki.com/microsphere-projects/microsphere-resilience4j)

### Developer Guide

- Current line (v0.2.x): [English](docs/current/developer-guide/README.md) · [中文](docs/current/developer-guide/zh/README.md)
- 1.x line (v0.1.x): [English](docs/1.x/developer-guide/README.md) · [中文](docs/1.x/developer-guide/zh/README.md)

### Wiki

[Github Host](https://github.com/microsphere-projects/microsphere-resilience4j/wiki)

### JavaDoc

- [microsphere-resilience4j-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-commons)
- [microsphere-resilience4j-spring](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-spring)
- [microsphere-resilience4j-alibaba-druid](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-alibaba-druid)
- [microsphere-resilience4j-mybatis](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-mybatis)
- [microsphere-resilience4j-openfeign](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-openfeign)
- [microsphere-resilience4j-spring-web](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-spring-web)
- [microsphere-resilience4j-spring](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-resilience4j-spring)

## License

The Microsphere Spring is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
