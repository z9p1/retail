# 大模型智能对话与 RAG 架构系统 — 简历/项目介绍

> 可直接复制到简历「项目经历」或「个人项目」中，按需删减或扩展。

---

## 一句话介绍

基于 **Dify + RAG + Milvus** 搭建的大模型应用架构：**Java 后端**负责鉴权、限流与编排，**Dify** 提供对话与工作流并接入大模型 API（如 OpenAI），**自建 Python RAG 服务 + Milvus** 实现大规模知识检索；配合 **Redis** 会话与限流、**RabbitMQ** 异步建库，支持**持久化多轮上下文**与长会话摘要，形成从用户自然语言到模型回复的完整链路。

---

## 项目描述（简历用，可压缩为一段）

- **定位**：面向业务侧的大模型应用架构方案与落地指南，实现「用户自然语言 → Java → Dify/ RAG → 大模型 API → 回复」的端到端流程。
- **技术栈**：  
  **后端**：Java（Spring Boot）— 鉴权（JWT）、限流（Redis）、会话与对话编排；  
  **大模型层**：Dify（Python）— 对话/工作流/Agent，对接 OpenAI 等大模型 API；  
  **RAG**：Python RAG 服务（如 FastAPI）+ Milvus 向量库，提供知识检索 REST 接口（retrieve/ingest）；  
  **中间件**：Redis（会话、conversation_id 缓存、限流）、RabbitMQ（RAG 全量/增量建库等异步任务）。
- **核心职责**：  
  - 设计并实现 Java 与 Dify、Python RAG 的**接口编排**与**数据流**（含两种模式：RAG 在 Dify 内 vs 自建 RAG + Milvus）；  
  - **持久化上下文**：会话与消息落库（agent_conversation / agent_message），Dify conversation_id 持久化，支持多轮与历史拉取；  
  - **长会话控制**：通过限制历史轮数、自建上下文上限、会话分段或**历史摘要**（LLM 对较早轮次做摘要并写入 context_summary），控制单次传给 Dify 的 Token 长度；  
  - RESTful API 与 GraphQL 的职责划分（业务/聚合由 Java 暴露，Dify 与 RAG 以 REST 对外）。
- **成果**：形成可复用的架构文档与流程图，涵盖整体架构、自然语言请求链路、Embedding 在 RAG 中的角色、持久化上下文及长会话处理方案，便于在 Java 项目中接入大模型与 RAG 能力。

---

## 关键词（便于简历筛选）

大模型应用架构 | Dify | RAG | Milvus | 向量检索 | Java 后端编排 | 多轮对话 | 持久化上下文 | Redis 限流 | RabbitMQ 异步 | RESTful API | GraphQL | 长会话摘要

---

## 简短版（适合简历一行或两行）

**大模型智能对话与 RAG 架构**：基于 Dify + Milvus + Java 的端到端方案，Java 负责鉴权/限流/编排，Dify 对接大模型 API，自建 Python RAG 服务做向量检索；支持持久化多轮上下文与长会话摘要，配套 Redis/RabbitMQ 做限流与异步建库。
