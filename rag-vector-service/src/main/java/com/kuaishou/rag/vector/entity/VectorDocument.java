package com.kuaishou.rag.vector.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 向量文档实体类
 * 对应 Milvus Collection 中的一条记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（UUID）
     */
    private String id;

    /**
     * 向量数据（浮点数组）
     * 维度必须与 Collection 定义一致，默认 1536（OpenAI embedding）
     */
    private List<Float> vector;

    /**
     * 原始文本内容
     */
    private String content;

    /**
     * 所属文档 ID
     */
    private String docId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 扩展元数据（JSON 格式存储）
     * 可用于过滤条件，如：{"category": "tech", "author": "zhangsan"}
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 更新时间戳
     */
    private Long updateTime;

    /**
     * 相似度分数（查询时填充）
     */
    private Float score;
}
