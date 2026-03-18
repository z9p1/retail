-- RAG 知识库：文本块与向量（embedding 存 JSON 数组，检索时在应用层算相似度）
CREATE TABLE IF NOT EXISTS rag_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content TEXT NOT NULL COMMENT '文本块内容',
  embedding_json TEXT NOT NULL COMMENT '向量 JSON，如 [0.1,-0.2,...]',
  source_type VARCHAR(32) NOT NULL DEFAULT 'product' COMMENT '来源：product, faq, doc',
  source_id VARCHAR(64) COMMENT '来源 ID，如商品 id',
  create_time DATETIME
);

CREATE INDEX idx_rag_chunk_source ON rag_chunk(source_type, source_id);
