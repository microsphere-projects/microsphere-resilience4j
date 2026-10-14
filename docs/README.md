# Microsphere Resilience4j Documentation

This directory contains the complete documentation for the **microsphere-resilience4j** project, organized by release line, document type, and language.

## Documentation Structure

```
docs/
├── README.md                    # You are here — documentation index
├── current/                     # Docs for the `release` branch (v0.2.x, Resilience4j 2.x)
│   ├── user-guide/              # English (default language)
│   │   ├── README.md
│   │   ├── zh/                  # Chinese, identical file names
│   └── developer-guide/
│       ├── README.md
│       ├── zh/
└── 1.x/                         # Docs for the `release-1.x` branch (v0.1.x, Resilience4j 1.x)
    ├── user-guide/
    │   ├── README.md
    │   ├── zh/
    └── developer-guide/
        ├── README.md
        └── zh/
```

## Release Lines

| Directory | Source Branch | Version Line | Resilience4j | Spring Cloud |
|-----------|---------------|--------------|--------------|--------------|
| [`current/`](current/user-guide/README.md) | `release` | `0.2.x` | 2.0.0 – 2.4.0 | 2022.0.x – 2025.0.x |
| [`1.x/`](1.x/user-guide/README.md) | `release-1.x` | `0.1.x` | 1.3.0 – 1.7.0 | Hoxton – 2021.0.x |

## Guides

### User Guide

Audience: developers **using** the library in an application.

- [Current (0.2.x) User Guide](current/user-guide/README.md) · [中文](current/user-guide/zh/README.md)
- [1.x (0.1.x) User Guide](1.x/user-guide/README.md) · [中文](1.x/user-guide/zh/README.md)

Contents: installation, per-module API reference, runnable code examples (derived from the project test-cases), configuration, version and compatibility information.

### Developer Guide

Audience: developers **contributing to** or extending the library.

- [Current (0.2.x) Developer Guide](current/developer-guide/README.md) · [中文](current/developer-guide/zh/README.md)
- [1.x (0.1.x) Developer Guide](1.x/developer-guide/README.md) · [中文](1.x/developer-guide/zh/README.md)

Contents: architecture design, module-by-module source analysis, design patterns used, and mermaid flow diagrams for complex control and event flows.

## Language Policy

English is the default language and lives on the main path of each guide directory. Chinese translations are stored in a parallel `zh/` subdirectory with **identical file names**, so a page pair is always `<dir>/<name>.md` (English) and `<dir>/zh/<name>.md` (Chinese).

## Modules Documented

Per-module documents exist in both guides for:

- `microsphere-resilience4j-commons` — core templates and facade (no Spring dependency)
- `microsphere-resilience4j-spring` — Spring annotation-driven integration and event bridge
- `microsphere-resilience4j-alibaba-druid` — Druid connection-pool plugin
- `microsphere-resilience4j-mybatis` — MyBatis executor plugin
- `microsphere-resilience4j-openfeign` — OpenFeign capability/client plugin
- `microsphere-resilience4j-spring-web` — Spring Web handler-interceptor plugin
- `microsphere-resilience4j-hibernate-core` — Hibernate entity-callback plugin (both lines; not managed by the BOM)
- `microsphere-resilience4j-compatible` — Resilience4j 1.x event compatibility shim (`1.x` only)
- `microsphere-resilience4j-parent` / `microsphere-resilience4j-dependencies` — build parent and BOM
