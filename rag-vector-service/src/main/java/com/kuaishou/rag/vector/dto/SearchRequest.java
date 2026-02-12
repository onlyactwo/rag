package com.kuaishou.rag.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索请求 DTO
 */
@Data
public class SearchRequest {

    /**
     * 查询向量（必须）
     * 由 Embedding Service 生成
     */
    @NotNull(message = "查询向量不能为空")
    private List<Float> vector;

    /**
     * 返回结果数量（TopK）
     * 默认 10，最大 100
     */
    @Min(value = 1, message = "topK 至少为 1")
    @Max(value = 100, message = "topK 最大为 100")
    private Integer topK = 10;

    /**
     * 候选集倍数
     * 内部检索 candidate = topK * candidateMultiplier
     * 越大召回率越高，但性能下降
     */
    @Min(value = 1)
    @Max(value = 100)
    private Integer candidateMultiplier = 10;

    /**
     * 过滤条件（可选）
     * 支持字段：doc_id, doc_name, metadata.xxx
     * 示例：{"doc_id": "doc123", "metadata.category": "tech"}
     */
    private Map<String, Object> filters;

    /**
     * 指定搜索的 Collection 名称
     * 默认使用配置文件中的 collection
     */
    private String collectionName;

    /**
     * 是否输出向量数据
     * 默认 false，减少网络传输
     */
    private Boolean outputVector = false;
}
