# Microsphere Resilience4j 文档

本目录包含 **microsphere-resilience4j** 项目的完整文档，按版本线、文档类型和语言组织。

## 文档结构

```
docs/
├── README.md                    # 你在这里——文档索引
├── current/                     # `release` 分支的文档（v0.2.x，Resilience4j 2.x）
│   ├── user-guide/              # 英文（默认语言）
│   │   ├── README.md
│   │   ├── zh/                  # 中文，文件名完全一致
│   └── developer-guide/
│       ├── README.md
│       ├── zh/
└── 1.x/                         # `release-1.x` 分支的文档（v0.1.x，Resilience4j 1.x）
    ├── user-guide/
    │   ├── README.md
    │   ├── zh/
    └── developer-guide/
        ├── README.md
        └── zh/
```

## 发布线

| 目录 | 源分支 | 版本线 | Resilience4j | Spring Cloud |
|-----------|---------------|--------------|--------------|--------------|
| [`current/`](../current/user-guide/zh/README.md) | `release` | `0.2.x` | 2.0.0 – 2.4.0 | 2022.0.x – 2025.0.x |
| [`1.x/`](../1.x/user-guide/zh/README.md) | `release-1.x` | `0.1.x` | 1.3.0 – 1.7.0 | Hoxton – 2021.0.x |

## 指南

### 用户指南

读者对象：在应用中**使用**该库的开发者。

- [当前 (0.2.x) 用户指南](../current/user-guide/zh/README.md) · [English](../current/user-guide/README.md)
- [1.x (0.1.x) 用户指南](../1.x/user-guide/zh/README.md) · [English](../1.x/user-guide/README.md)

内容：安装、逐模块 API 参考、可运行代码示例（源自项目的测试用例）、配置，以及版本与兼容性信息。

### 开发者指南

读者对象：**为该库贡献代码**或对其进行扩展的开发者。

- [当前 (0.2.x) 开发者指南](../current/developer-guide/zh/README.md) · [English](../current/developer-guide/README.md)
- [1.x (0.1.x) 开发者指南](../1.x/developer-guide/zh/README.md) · [English](../1.x/developer-guide/README.md)

内容：架构设计、逐模块源码分析、所用设计模式，以及复杂控制流与事件流的 mermaid 流程图。

## 语言策略

英文是默认语言，位于每个指南目录的主路径上。中文译文存放于与之平行的 `zh/` 子目录中，**文件名完全一致**，因此每一对页面始终是 `<dir>/<name>.md`（英文）和 `<dir>/zh/<name>.md`（中文）。

## 已记录的模块

以下模块在两个指南中都有逐模块文档：

- `microsphere-resilience4j-commons` — 核心模板与门面（不依赖 Spring）
- `microsphere-resilience4j-spring` — Spring 注解驱动集成与事件桥
- `microsphere-resilience4j-alibaba-druid` — Druid 连接池插件
- `microsphere-resilience4j-mybatis` — MyBatis 执行器插件
- `microsphere-resilience4j-openfeign` — OpenFeign capability/client 插件
- `microsphere-resilience4j-spring-web` — Spring Web 处理器拦截器插件
- `microsphere-resilience4j-hibernate-core` — Hibernate 实体回调插件（两条版本线均有；不受 BOM 管理）
- `microsphere-resilience4j-compatible` — Resilience4j 1.x 事件兼容垫片（仅 `1.x`）
- `microsphere-resilience4j-parent` / `microsphere-resilience4j-dependencies` — 构建父工程与 BOM
