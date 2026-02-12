# RAG Java 微服务脚手架

> 快手 RAG 知识库系统 - Java 微服务实现

## 📋 项目结构

```
rag-java/
├── rag-parent/                 # 父POM，依赖版本管理
├── rag-common/                 # 公共模块
├── rag-api-gateway/            # API网关 (Spring Cloud Gateway)
├── rag-document-service/       # 文档服务
├── rag-ingestion-service/      # 数据摄入服务
├── rag-vector-service/         # 向量存储服务 (Milvus)
├── rag-query-service/          # 查询服务
├── rag-llm-service/            # LLM服务 (Spring AI)
├── rag-admin-service/          # 管理服务
├── deploy/                     # 部署配置
│   ├── docker/                 # Docker Compose配置
│   ├── k8s/                    # Kubernetes配置
│   └── mysql/                  # 数据库脚本
└── docs/                       # 文档
```

## 🚀 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- Milvus (向量数据库)
- MySQL 8+
- Redis 6+

### 2. 编译项目

```bash
cd rag-java
mvn clean install -DskipTests
```

### 3. Docker Compose 启动

```bash
cd deploy/docker
docker-compose up -d
```

### 4. 访问服务

| 服务 | 地址 |
|------|------|
| API Gateway | http://localhost:8080 |
| Document Service | http://localhost:8081 |
| Vector Service | http://localhost:8082 |
| Query Service | http://localhost:8083 |
| LLM Service | http://localhost:8084 |

## 📖 文档

- [部署指南](deploy/docker/README.md)
- [API 接口文档](../api-specification.md)
- [数据库设计](../database-design.md)
- [项目路线图](../roadmap.md)

## 🔧 中间件配置

所有中间件连接配置都在各服务的 `application.yml` 中，需要修改的地方都有注释说明：

- **Milvus**: 向量数据库连接
- **MySQL**: 业务数据存储
- **Redis**: 缓存和消息队列
- **Consul**: 服务注册发现

## 📝 开发说明

TODO: 后续补充详细开发指南
