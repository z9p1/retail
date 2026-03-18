# 大模型架构指南：Dify + RAG + Milvus（面试背诵版）

本文档格式与「面试背诵」一致：站在回答者角度，每个内容用一段话回答，不换行。

---

## 一、名词与概念

RAG（检索增强生成）： 在我们架构里，RAG 是指先根据用户问题从知识库检索相关片段，再把片段和问题一起交给大模型生成回答；检索和生成都在 Dify 里完成，Java 只发用户原话，不参与检索。分块（Chunk）就是把长文档或整库数据切成小段便于做向量和检索；Ingest Pipeline 是建库流水线，即拉数据、清洗、分块、嵌入、写向量库；嵌入或向量是把文字变成一串数字用来算两段话有多像，在 AI 知识库语境下向量即文本含义在数学空间中的坐标；向量库如 Milvus 存向量并能按相似度查最像的 K 条。Dify 是开源大模型应用平台，提供对话、工作流、知识库，对外 HTTP API；Milvus 是开源向量数据库，适合 RAG 大规模检索。query 在调 Dify 时就是本轮用户输入，Java 只传用户原话作为 query，多轮历史由 Dify 按 conversation_id 存储与带入。召回是第一轮用用户问题向量在向量库做相似度查找拉出一批候选 chunk；Rerank 是对已召回的 chunk 再打一次分重新排序后取 Top K 给大模型；Score 阈值只保留相关度分数不低于阈值的 chunk。REST 用 URL 和 HTTP 方法做接口；GraphQL 让前端一次请求按需拿多种数据；RabbitMQ 把 RAG 全量建库等耗时任务从 Java 解耦由 Python 消费；Redis 做限流计数和可选结果缓存，会话和上下文由 Dify 存储不存 conversation_id。MCP 是让 IDE 或助手连你工具服务的协议，主链路用户到 Java 到 Dify 不经过 MCP。Orchestrator 编排器在我们架构里就是 Java 后端，负责鉴权、限流、把用户原话当 query 调 Dify、解析返回，RAG 和 LLM 在 Dify，Java 只做编排。预训练是用海量文本让模型学通用语言能力，得到基座模型；微调是在基座上用你的业务数据再训一茬，分全量微调和增量微调如 LoRA。

---

## 二、整体架构与各组件职责

架构总览与组件职责： 我们整体是前端通过 REST 或 GraphQL 到 Java 后端，Java 做鉴权、限流、调 Dify、发 MQ，会话在 Dify；Java 连 Redis 做限流、连 Dify 做 LLM 和 RAG、连 RabbitMQ 发 RAG 建库任务；Dify 可选调 Python RAG 服务的 retrieve 查 Milvus，或由 Python 消费者从 MQ 执行 Ingest 写 Milvus。一句话就是架子是 Dify 负责对话、知识库 RAG 和会话存储，Java 只做鉴权、限流、转发用户原话、解析返回，不存会话和上下文，RAG 在 Dify 内完成。Java 的职责是业务入口、鉴权限流、把用户原话当 query 调 Dify、可选发 MQ 触发建库、返回回复，不负责 RAG 和 LLM 也不存会话；Dify 负责对话、工作流、知识库 RAG，会话与多轮上下文按 conversation_id 存储，对外 REST 如 POST /v1/chat-messages；Python RAG 仅当不用 Dify 知识库改用自建 Milvus 时，由 Dify 工作流 HTTP 调该服务做检索或 Dify 直连 Milvus，建库可由 Java 发 MQ、Python 消费写 Milvus；Milvus 存 RAG 的向量和元数据，只被 Python 或 Dify 读写不对 Java 暴露；Redis 做限流 key 和可选结果缓存，会话与 conversation_id 不存 Redis 由 Dify 管理；RabbitMQ 用于 Java 发 RAG 同步任务、Python 消费执行 Ingest 避免 HTTP 长时间阻塞。

---

## 三、一条自然语言的旅程（主流程）

主流程（RAG 在 Dify 知识库）： 用户说一句话后前端请求 Java，Java 鉴权、限流、把用户原话当 query 调 Dify，conversation_id 由前端传或省略；Dify 内部做知识库检索即 Embedding、召回、可选 Rerank、Top K，拼 Prompt 调 LLM 并自行存会话，返回 answer 和 conversation_id；Java 解析后返回前端，不存会话，前端若需多轮则保存 conversation_id 下次带上。具体来说前端 POST /api/agent/chat 带 message 和可选 conversation_id，Java 鉴权、限流、query 等于用户原话、POST Dify chat-messages 带 query、user、conversation_id、response_mode blocking，Dify 做知识库检索、拼 Prompt、调 LLM、在 Dify 侧存会话与历史并返回，Java 解析 answer 和 conversation_id 返回前端，路径是前端到 Java 到 Dify 再回 Java 到前端。

自建 Milvus 与异步建库： 若采用 Dify 工作流调自建 Milvus，Java 仍然只发用户原话给 Dify，Dify 工作流里用 HTTP 节点调 Python RAG 的 POST /retrieve 或 Dify 直连 Milvus，拿到 chunk 再拼 Prompt、调 LLM，RAG 还是在 Dify 侧完成。异步建库不跟用户请求同路，管理员或定时任务在 Java 侧触发同步知识库，Java 往 RabbitMQ 发消息如 queue 为 rag.ingest、body 含 source_type、full_refresh，Python 消费者拉取数据、清洗、分块、嵌入、写 Milvus，用户之后问问题时检索就能命中新数据。

业务数据如何进回答： 像「我的累计消费」这类在你自己 DB 里的数据不用 RAG，有两种做法：一是 Dify 工作流里加 HTTP 请求节点调 Java 的接口如 GET /api/user/consumption/summary 拿 JSON 再拼进 Prompt 给 LLM；二是 Java 先根据 JWT 查 DB，把结果拼成一段文字放进 query 再调 Dify，例如 query 里带「当前用户累计消费：12345 元。用户问：我的累计消费多少？请用一句话回复」，Dify 只做生成。Embedding 在建库时把每条 chunk 转成向量存进向量库，检索时把用户问题也转成向量在库里做相似度查找，建库和检索必须用同一套嵌入模型和同一维度否则向量不可比。

---

## 四、REST、GraphQL、MCP

REST / GraphQL / MCP 怎么用： 智能助手对话用 REST 如 POST /api/agent/chat；前端要工作台加对话加用户一页多数据时可在 Java 层提供 GraphQL 一次请求按需拿；Java 调 Dify 一定是 REST 因为 Dify 只提供 REST。MCP 主链路不经过，MCP 是给 Cursor 等 IDE 连你的 MCP Server 用自然语言查工作台、消费等，Server 内部 HTTP 调你 Java 接口，和 Dify 及用户请求并行。

---

## 五、Redis 与 RabbitMQ

Redis 与 RabbitMQ： 本架构下上下文由 Dify 存储，Redis 不存 conversation_id。Redis 由 Java 读写，用途包括限流如 key 为 agent:ratelimit:{userId} 过期 30 秒，以及可选结果缓存如 agent:cache:{hash(query)} 过期 5 分钟。RabbitMQ 用于 RAG 全量或增量同步，队列 rag.ingest 由 Java 生产、Python 消费执行 Ingest 写 Milvus，消息体示例为 source_type、full_refresh 等。

---

## 六、Dify + RAG 架子

Dify 搭建与 Python RAG： 我们部署 Dify 用 Docker Compose 或云，配置 .env 如 OPENAI_API_KEY，在 Dify 里创建对话型或工作流应用，若用内置知识库就上传文档并启用知识库检索节点，发布后在 API 访问拿到 Base URL 和 API Key，Java 用 RestTemplate 或 HttpClient 调 POST chat-messages 即可。若自建 Milvus，Python RAG 提供 POST /retrieve 入参 query、top_k，内部对 query 嵌入、查 Milvus、返回 content 和 source_id 等；POST /ingest 或由 MQ 消费者直接写 Milvus；Java 不调 RAG，若用自建 Milvus 由 Dify 工作流里的 HTTP 节点调 /retrieve。

向量与 Dify 知识库的存储架构（索引与实体分离）： 在 AI 知识库的语境下，向量（Vector）是文本含义在数学空间中的坐标，用于相似度计算。在 Dify 的默认实现中，Milvus 主要存储向量和 ID，而原始文本（Chunk）存储在关系数据库（PostgreSQL）中，例如当你看到知识库中的“143 分段”时，Dify 会将这些文本分段保存到 db_postgres 容器中。在企业级 RAG 实践中，Dify 采用的是索引与实体分离的存储架构：当外置 Milvus 完成向量检索后，它仅返回相似度最高的 Primary Key（分段 ID），而不直接返回长文本；Dify 随后会根据这些 ID，从持久化能力更强、成本更低的 PostgreSQL 关系型数据库中精准抓取原始 Chunk 文本和元数据。这种设计既保证了向量检索的极速响应，又利用关系型数据库确保了分段内容的高可用性与多租户安全隔离，主要为了兼顾存储成本、数据一致性以及 Rerank 的灵活性。

本项目知识库与向量的具体实现： 我们项目当前没有使用外置 Milvus，而是用 MySQL 单表 rag_chunk 存储 RAG 数据，表结构包括 id、content、embedding_json（TEXT 存向量 JSON 数组）、source_type、source_id、create_time，即向量与原始文本在同一张表中，没有做索引与实体分离。建库由 Java 的 RagService 完成，从商品表同步时按商品生成 chunk（名称加描述）、调用 EmbeddingService 获取 embedding 向量、写入 rag_chunk，先按 source_type 为 product 删除再插入实现全量覆盖；检索时在应用层查全表、解析 embedding_json、用余弦相似度算分、排序取 topK（配置项 rag.retrieve-top-k 默认 5）、将选中 chunk 的 content 拼接成参考文本返回，供智能助手增强回答。这种实现适合数据量不大、无需独立向量库的场景；若后续知识库规模扩大或需要与 Dify 内置知识库一致的高性能检索与 Rerank 灵活性，可改为 Milvus 存向量与 ID、MySQL 或 PostgreSQL 存原文的索引与实体分离架构，与 Dify 默认方案对齐。

Milvus 向量表与 RAG 要点： 用 Milvus 存 RAG 时用 Collection，常用字段包括 id 主键、embedding 向量维度与嵌入模型一致、content 文本块原文、source_type 和 source_id 用于多源区分与增量更新、create_time 可选、tenant_id 可选做多租户隔离；建 Collection 时向量维度必须和嵌入模型输出一致，对 embedding 建向量索引如 IVF_FLAT 或 HNSW，度量选余弦或内积，检索时可用 source_type、tenant_id 做 filter。RAG 要点上我们同一 Collection 用 source_type 和 source_id 区分多源，按源发 MQ、消费者按 source_type 分支写入，检索可全库或按 source_types 过滤；Ingest Pipeline 是拉取、数据清洗、分块、嵌入、写库；知识库更新可全量按 source_type 先删后插或增量按 source_id 增删改，触发方式为定时、事件 MQ 或手动；Embedding 选型上建库与检索同一模型同一维度，中文优先考虑 BGE、M3E；可选多路召回如向量加关键词 BM25 再加图谱，合并去重后 Rerank 取 Top K。

---

## 七、Java 侧接入

Java 侧接入： 我们依赖 RestTemplate 或 WebClient 调 Dify、Spring Data Redis 做限流、Spring AMQP 发 rag.ingest；配置 dify.base-url、dify.api-key 或从 DB 的 agent_config 读，Redis 和 RabbitMQ 用现有即可。核心流程是鉴权、限流用 Redis、query 等于用户原话、conversation_id 由前端传入多轮时带上、POST Dify chat-messages、解析 answer 和 conversation_id、返回前端且不存会话，会话在 Dify。发 RAG 任务用 rabbitTemplate.convertAndSend 发 rag.ingest 队列，body 含 source_type、full_refresh，Python 消费执行。若需要 GraphQL 则在 Java 用 Spring GraphQL 定义 Schema，Resolver 调现有 REST 或 Service，与 Dify 无直接关系。

---

## 八、持久化与安全

上下文存在哪与按角色处理： 本架构下对话上下文即会话和多轮历史由 Dify 存储，Java 不建会话表、不存 conversation_id。Dify 按 conversation_id 维护同一会话的对话记录，生成时自动带入历史，conversation_id 由 Dify 在首次对话时返回，之后请求带上即可续聊；前端若要做多轮需保存 Dify 返回的 conversation_id，下次请求在 Body 里带上，Java 只做透传不存；若需会话列表或某会话历史展示可调 Dify 提供的会话或消息 API 或由前端本地保存会话 id 列表，不在 Java 建 agent_conversation 或 agent_message 表。同一账号可能有店家或用户等不同角色，限流与 Dify 的 user 按角色区分，调 Dify 时 user 带角色如 store-123、user-456，限流 key 带角色如 agent:ratelimit:{userId}:{userRole} 避免换角色后共用限流。长会话与摘要由 Dify 侧配置，在应用或工作流中限制带入历史的轮数或上下文长度，Java 不维护长会话与摘要。

多租户与鉴权： 多租户上 Redis 限流 key 可带 tenant_id，Dify 的 user 可带租户如 tenant-1-store-123，RAG 若按租户隔离则向量库 filter tenant_id；鉴权上对外 API 统一在 Java 做 JWT 校验，Dify 回调 Java 时用内部 API Key 不校验 JWT；角色上接口与数据按 user_id、user_role 及 tenant_id 隔离，会话内容在 Dify，Java 只做鉴权与透传。

---

## 九、搭建顺序与检查清单

搭建顺序与检查： 我们按以下顺序搭建：先启好 Redis 和 RabbitMQ 确保 Java 能连；部署 Dify，创建对话应用，拿到 Base URL 和 API Key，用 Postman 调通 POST /v1/chat-messages；Java 实现 POST /api/agent/chat 做鉴权、限流、用户原话发 Dify，conversation_id 由前端传入、Java 透传，返回 answer 与 conversation_id；上下文由 Dify 存储无需在 Java 建会话或消息表，前端多轮时保存 conversation_id 并在下次请求带上；可选在 Dify 知识库上传文档、工作流启用知识库检索；可选 Milvus 加 Python RAG，Dify 工作流 HTTP 调 /retrieve，Java 发 MQ、Python 消费写 Milvus；可选再做 GraphQL、MCP。检查时确认用户发一句经 Java 到 Dify 能拿到 answer 并返回、限流生效、多轮时前端带 conversation_id 且 Dify 返回连续对话、若用知识库则回答能体现检索内容。

---

## 十、本项目如何使用 Dify

本项目如何使用 Dify： 我们安装 Dify 是克隆 langgenius/dify 仓库、进 docker 目录、copy .env.example .env、docker compose up -d，浏览器打开 http://localhost/install 初始化管理员再访问 http://localhost 登录。在 Dify 中创建对话型应用、配置 LLM，可选在工作流中加 HTTP 请求节点调本后端如 GET 工作台接口，发布后在 API 访问记录 API Key 和 Base URL 如 http://localhost/v1。在本项目中启用 Dify 时在 application.yml 设 agent.provider 为 dify、agent.api-url 与 Dify Base URL 一致，数据库表 agent_config 中 config_key 为 agent_api_key 的 config_value 设为 Dify 应用的 API Key，重启后端后在店家端对话页发一句验证。请求对应关系上 agent.provider 为 dify 表示使用 Dify 对话 API，agent.api-url 对应 Dify API 访问的 Base URL，agent_config 的 agent_api_key 对应 Dify API 访问的 API Key，POST chat-messages 的 Body 里 query 为用户原话、user、conversation_id、response_mode 为 blocking，RAG 在 Dify 内 Java 只传 query；Dify 需回调本后端时后端可增加内部 Key 校验。

---

## 十一、自然语言完整流程（Java 与 Dify 分工）

端到端时序与职责对照： 与一条自然语言的旅程一致，端到端是用户输入后前端 POST /api/store/agent/chat 带 message 和可选 conversation_id 及 JWT，Java 鉴权、参数校验、限流用 Redis、读 API Key 和 api-url、query 为用户原话、conversation_id 由前端传入无则省略、POST Dify chat-messages，Dify 校验 API Key、跑工作流如知识库检索、可选 HTTP 调 Java、LLM 生成、在 Dify 侧存会话、返回 answer 和 conversation_id，Java 解析 answer 和 conversation_id、返回前端 reply 和 conversation_id 且不存会话，前端展示且多轮时保存 conversation_id 下次带上。职责上编排和入口在 Java 做鉴权、限流、转发用户原话、解析返回，RAG 在 Dify 做知识库检索即 Embedding、召回、Rerank、拼 Prompt，业务数据由 Java 提供 REST、可选被 Dify HTTP 调，Dify 工作流内 HTTP 节点调 Java，LLM 生成在 Dify；会话和多轮由 Java 不存、只透传前端传来的 conversation_id 并返回 Dify 的 conversation_id 给前端，由 Dify 存储、按 conversation_id 维护历史、生成时带入。按上述流程自然语言从进入到出口的路径与分工一一对应，便于排查与扩展。

---

## 十二、实践要点：AtoA、省 Token、防幻觉

AtoA 与省 Token： AtoA 即 Agent 或 App 串联，我们做法是 Java 当总编排决定这轮调哪个 Dify 应用，再把上一个结果当输入喂给下一个；可在 Dify 里建多个应用如知识问答、营销话术、SQL 解析，Java 里为每个配置 app_key 和 base_url，编排上先调一个路由 Agent 做意图判断再根据返回调对应 Agent，需要串行时 Agent_A 输出后 Java 执行或查 DB、结果再当输入调 Agent_B，Dify 内部也可用 HTTP 请求节点调另一个 Dify 应用的 chat-messages 实现 App 串联，但总编排、安全、审计仍建议放在 Java。节约 Token 上我们在 Dify 侧限制带入历史的轮数或长度，若 Dify 支持摘要可在 Dify 内配置；RAG 少而精即 Top K 设小如 3 到 5、设 score 阈值低分 chunk 不塞给模型、建库时去重去噪 chunk 别太长；Prompt 瘦身即系统提示写要点、工具或 HTTP 返回只给必要字段；输出结构化要求模型输出 JSON 或字段减少废话间接省 token。

防幻觉： 在输入层系统 Prompt 写明只根据检索到的知识或接口返回数据回答、查不到就说不知道不要编造、金额数量以数据为准；检索层设 score 阈值，若没有任何 chunk 达到阈值则走兜底逻辑如直接让模型回答当前知识库无相关信息，不喂低质量上下文；生成层重要回答用结构化输出如 JSON，要求回答里标明依据如根据知识库或根据接口今日订单 20 单；出口层 Java 对数字做范围或合理性校验，不信任 LLM 直接给出的金额做下单或改价，只用于展示或解释。

---

## 十三、Dify 工作流在 Linux/生产上怎么「画线」

Dify 工作流在生产上的管理： 图形化画线只能在有浏览器的界面里操作，在 Linux 服务器或生产环境没有桌面时不是在生产上画线，而是用 DSL 导出导入或 Console API 来管理。画线只在开发或本机的 Dify 网页里做，打开工作流编排页拖节点、连线、保存；生产或 Linux 上不装浏览器、不画线，把在开发环境导出的 DSL 文件拿到生产 Dify 里导入就会生成同样节点和连线的应用，也就是说线即工作流是数据，用 DSL 文件表示，在 A 环境画好导出、在 B 环境导入即可无需在 B 再画一遍。导出 DSL 时在能画线的那台或开发环境打开 Dify 工作室、进入该应用的工作流编排页，在页面左上角工作流图标下方菜单选导出 DSL 或部分版本在右下角或编排栏选导出 DSL，会下载一个 YAML 文件包含节点、连线、变量、知识库引用等。在生产或 Linux 上的 Dify 里导入时用浏览器访问生产 Dify 的地址，在工作室首页或创建应用里选通过 DSL 导入或导入 DSL，上传导出的 YAML 文件，导入后检查知识库或模型等若缺插件或配置按提示补全、发布应用后即可用 API，这样生产上的工作流等于导入的 DSL 不需要在生产服务器上画线。无浏览器时可用 Dify 的 Console API 批量导出所有应用的 DSL，社区有 dify-apps-dsl-exporter 等用 Python 调 Console API 批量拉取 DSL；流程建议是在开发或测试 Dify 画好工作流并导出 DSL、把 YAML 放进代码库或配置仓库，部署生产时用同一 YAML 在生产 Dify 里导入，变更工作流时在开发环境改、导出新 DSL、替换仓库中的文件、在生产 Dify 重新导入或覆盖。小结是开发或本机有浏览器时画线并在 Dify 编排页拖节点连线、保存后导出 DSL，生产或 Linux 无桌面时不画线、用导出的 DSL 文件在 Dify 里导入得到相同工作流或通过 Console API 批量导出备份，画线等于编辑工作流数据，在 Linux 或生产上只要能把这份数据 DSL 灌进 Dify 即可不需要在服务器上打开图形界面。

---

## 十四、预训练与微调（大白话）

预训练与微调： Dify 里用的大模型如 OpenAI、通义、本地 LLaMA 都是先预训练再视情况微调出来的。预训练是用海量文本如网页书籍百科训练模型，让它学会人话即语法、常识、简单推理、多语言等，不针对具体业务属于通用能力，得到基座模型如 LLaMA、Qwen、ChatGLM，你接 Dify 时选的模型多数就是这类基座或厂商已微调过的版本，能力强泛化好但对你的话术产品名流程不熟，常配合 RAG 或微调用。微调是在已经预训练好的基座上再用你自己的数据如问答对、客服话术、报表解读示例训一轮，让模型更贴合你的领域、风格或任务格式；全量微调是更新所有参数、显存算力要求高、数据量要大，增量微调如 LoRA 只加一小部分可训练参数、原模型大部分冻结、省显存省时间、数据量可以小一些，得到再在你数据上学了一茬的模型，在 Dify 里把模型地址指到微调后的模型即可。本架构里 Dify 用的模型常见做法是直接用现成 API 如 OpenAI、通义、千问，这些已经是预训练加厂商可能微调过的，不需要自己预训练也不一定要微调；若自托管则拿开源基座模型在 Dify 里配置模型地址，若要更贴业务再在本地或云上用你的数据做 LoRA 等微调，把微调后的模型部署好、Dify 指向该部署地址；Java 侧不参与预训练和微调只调 Dify，模型是谁、是否微调过对 Java 透明。什么时候考虑微调上，若回答总偏通用、不贴你行业话术可做领域或话术微调用 LoRA 加你整理的问答或话术数据；输出格式要固定如 JSON 或表格时微调时多给格式正确的样本或先用 Prompt 加结构化输出解决、不够再微调；知识更新频繁、业务文档多时优先用 RAG 把知识塞进检索，微调主要改怎么说而不是说什么；数据少、只想快速上线时先用 RAG 加好 Prompt，微调可后续再加。预训练给模型通用能力一般不自己做、用现成基座或 API，微调让模型更像你业务的人说话、更守格式、适合话术风格领域术语，RAG 把最新细碎的知识通过检索塞进上下文、知识更新不用重新训模型，三者可并存即基座加可选微调加 RAG，本架构里 Dify 负责调用的就是基座或微调后的模型加 RAG。

---

## 十五、面试背诵（每问一段话，不换行）

模型上下文（上下文持久化）： 在我们项目中，模型上下文持久化是由 Java 侧统一管理，而不是完全依赖第三方平台自动维护，核心目标是保证多轮对话可持续、跨设备可恢复、并且上下文长度可控。整体设计上我们采用 conversation 表和 message 表分层存储，会话级信息存放在 conversation 表中，包括 conversation_id、user_id、tenant_id、会话状态、摘要 summary、创建与更新时间等，用于做会话归属隔离和生命周期管理；每一轮具体对话内容存放在 message 表中，包括 role（user/assistant/system）、content、token 预估值、时间戳等，用于构建上下文和后续审计统计。在实现流程上，当用户发起请求时，Java 会先校验 conversation_id 与当前 user_id 是否匹配，防止串会话，然后从 message 表中按时间倒序查询最近 N 条历史消息，同时读取 conversation 表中的摘要字段；接着对即将发送给模型的上下文进行 token 预估，如果长度超过设定阈值，则采用“历史摘要 + 最近若干轮对话”的方式进行裁剪，而不是无限拼接全部历史，从而解决长对话 token 膨胀问题；整理完成后，将 system 指令、摘要内容、最近消息以及当前问题组装成 messages JSON 结构，通过 HTTP 调用 Dify 接口进行推理；模型返回结果后，再将 assistant 回复落库，并根据对话长度决定是否更新摘要字段。在方案选择上我们评估过三种模式：第一种完全依赖 Dify 的 conversation_id 机制由平台维护历史，开发简单但可控性不足；第二种完全由 Java 侧自主管理上下文并显式传递 messages，灵活性最高，适合多租户和企业级场景；第三种是在第二种基础上增加摘要压缩机制控制上下文长度。最终我们选择“Java 自主管理 + 摘要压缩”的方案，因为这样既能保证数据主权和权限隔离，又能精确控制 token 成本、支持跨设备续聊，并为后续接入 RAG 或做会话分析预留扩展空间。整体设计原则是“会话状态由业务系统掌控，模型只负责推理”，从而实现稳定、可控、可扩展的多轮对话能力。

RAG 整体流程： 我在项目中落地过完整的 RAG 流程，整体分为离线建库和在线检索生成两个阶段，离线阶段包括数据拉取、清洗去重、合理分块并设置 overlap、使用统一 embedding 模型生成向量并写入向量库，在线阶段则是对用户问题做向量化检索召回 Top K 片段，再根据需要做 Rerank 精排和阈值过滤，最后将相关片段与用户问题一起拼接进 Prompt 交给大模型生成回答；在实现方案上可以选择纯向量召回、向量+关键词混合召回或多路召回融合排序三种模式，我实际采用混合召回提升召回率，从而降低模型幻觉并支持知识动态更新而无需重训模型。

RAG 各环节优化： 在优化层面我从建库、检索和生成三阶段分别改进，建库阶段重点优化分块长度与重叠比例、统一 embedding 模型版本以及增加 source_type 和 tenant_id 字段实现多租户隔离，同时支持增量更新避免全量重建；检索阶段通过调整 Top K、设置 score 阈值过滤低相关内容并引入 Rerank 提升精度，同时采用向量+BM25 混合召回提高覆盖率；生成阶段则在 Prompt 中强约束“仅基于检索内容回答”并限制输出格式，整体优化目标是在准确率、响应延迟和 token 成本之间取得平衡。

微调概念和场景： 我理解微调主要解决的是模型表达能力和风格稳定性问题，而不是知识更新问题，在方案选择上可以分为全量微调、LoRA 等参数高效微调以及指令微调三种方式，全量微调效果上限高但成本大，LoRA 成本低更适合业务落地，指令微调适用于统一输出风格和行业术语；在实际决策中我会优先采用 RAG 解决“知识来源”问题，当出现固定格式输出不稳定或行业表达不统一时再考虑 LoRA 微调，通过对比 RAG+Prompt 与微调后的效果决定是否上线，避免过拟合和资源浪费。

大模型评估： 在模型评估方面我会从效果、性能和安全三个维度设计指标，效果上使用准确率、召回率、F1 以及业务自定义指标（如 JSON 格式正确率）并结合人工抽检，性能上关注平均延迟、TP99、吞吐量和单请求 token 消耗，安全上评估幻觉率、敏感信息泄露和越权风险；评估方法上采用离线标注集评测、线上灰度 A/B 对比以及用户反馈闭环三种方式，通过数据驱动持续优化 Prompt、RAG 参数或模型版本。

大模型微调落地流程： 在实际微调落地时我会按照数据准备、数据清洗去重、训练验证集划分、选择微调方式、训练参数调优以及效果评估六个步骤推进，优先选择 LoRA 以降低显存和训练成本，训练后与基线 RAG+Prompt 方案做对比评估准确率和幻觉率，同时关注是否出现过拟合或通用能力下降的问题，确保收益大于成本后再上线，因此我认为微调是能力增强手段而不是默认方案，需要结合业务目标和资源投入做权衡。

低并发/简单逻辑， 选 synchronized；高并发/性能敏感 ， 选 volatile + AtomicInteger；高并发QPS统计服务，另CAS（比较并交换-所以轻量），volatile是可见性，修饰boolean，也可选对应的AtomicI，count++不是原子操作，所以使用AtomicInteger对应的 xxxx增加？GET？（忘记了[笑哭]）,查API看看吧。需细粒度锁控制 ， 选 ReentrantLock。适用于复杂业务，多状态协作（如生产者-消费者）时，ReentrantLock + Condition 是最优解，高并发系统（如支付系统、订单引擎）的核心技术栈。
