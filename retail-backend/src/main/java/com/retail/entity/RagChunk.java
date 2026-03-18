package com.retail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 知识库文本块，embedding 以 JSON 数组形式存储
 */
@Data
@TableName("rag_chunk")
public class RagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private String embeddingJson;
    private String sourceType;
    private String sourceId;
    private LocalDateTime createTime;
}
