package com.kuaishou.rag.vector.service;

import com.kuaishou.rag.vector.dto.SearchRequest;
import com.kuaishou.rag.vector.dto.SearchResponse;
import com.kuaishou.rag.vector.dto.InsertRequest;
import com.kuaishou.rag.vector.dto.InsertResponse;
import io.milvus.client.MilvusClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.*;
import io.milvus.param.index.*;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储服务
 *
 * 核心功能：
 * 1. Collection 管理（创建/删除/加载）
 * 2. 向量插入（批量）
 * 3. ANN 搜索（近似最近邻）
 * 4. 混合搜索（向量 + 标量过滤）
 */
@Slf4j
@Service
public class VectorStoreService {

    @Autowired
    private MilvusClient milvusClient;

    // ==================== Collection 管理 ====================

    /**
     * 创建 Collection（如果不存在）
     *
     * @param collectionName Collection 名称
     * @param dimension      向量维度（如 1536 for OpenAI）
     * @param desc           Collection 描述
     * @return true if 创建成功
     */
    public boolean createCollection(String collectionName, int dimension, String desc) {
        try {
            // 检查是否已存在
            R<Boolean> hasResponse = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            );
            if (hasResponse.getData()) {
                log.info("Collection {} already exists", collectionName);
                return true;
            }

            // 定义字段
            List<FieldType> fields = Arrays.asList(
                // 主键字段
                FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(64)
                    .withPrimaryKey(true)
                    .withAutoID(false)  // 手动指定 ID
                    .build(),
                
                // 向量字段
                FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build(),
                
                // 内容文本（可选，用于返回原始内容）
                FieldType.newBuilder()
                    .withName("content")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build(),
                
                // 文档 ID（标量过滤用）
                FieldType.newBuilder()
                    .withName("doc_id")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(64)
                    .build(),
                
                // 业务元数据（JSON 字符串）
                FieldType.newBuilder()
                    .withName("metadata")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(4096)
                    .build()
            );

            // 创建 Collection
            R<RpcStatus> response = milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription(desc)
                    .withFieldTypes(fields)
                    .withShardsNum(2)  // 分片数，根据数据量调整
                    .build()
            );

            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("Failed to create collection: {}", response.getException().getMessage());
                return false;
            }

            // 创建索引（HNSW - 高精度）
            createIndex(collectionName, "embedding", IndexType.HNSW, MetricType.COSINE);

            // 加载 Collection
            loadCollection(collectionName);

            log.info("Collection {} created successfully with HNSW index", collectionName);
            return true;

        } catch (Exception e) {
            log.error("Error creating collection: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建索引
     */
    public boolean createIndex(String collectionName, String fieldName, IndexType indexType, MetricType metricType) {
        try {
            Map<String, String> extraParams = new HashMap<>();
            
            if (indexType == IndexType.HNSW) {
                // HNSW 参数
                extraParams.put("M", "16");                    // 每层最大连接数
                extraParams.put("efConstruction", "200");        // 构建时搜索深度
            } else if (indexType == IndexType.IVF_FLAT) {
                // IVF 参数
                extraParams.put("nlist", "4096");               // 聚类中心数
            }

            R<RpcStatus> response = milvusClient.createIndex(
                CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(fieldName)
                    .withIndexType(indexType)
                    .withMetricType(metricType)
                    .withExtraParam(extraParams)
                    .withSyncMode(Boolean.TRUE)  // 同步等待索引完成
                    .build()
            );

            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Error creating index: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 加载 Collection（必须加载后才能查询）
     */
    public boolean loadCollection(String collectionName) {
        try {
            R<RpcStatus> response = milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            );
            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Error loading collection: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 向量操作 ====================

    /**
     * 批量插入向量
     *
     * @param collectionName Collection 名称
     * @param request        插入请求
     * @return 插入结果
     */
    public InsertResponse batchInsert(String collectionName, InsertRequest request) {
        try {
            List<String> ids = request.getIds();
            List<List<Float>> vectors = request.getVectors();
            List<String> contents = request.getContents();
            List<String> docIds = request.getDocIds();
            List<String> metadataList = request.getMetadataList();

            // 构建 insert data
            List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("embedding", vectors),
                new InsertParam.Field("content", contents != null ? contents : Collections.nCopies(ids.size(), "")),
                new InsertParam.Field("doc_id", docIds != null ? docIds : Collections.nCopies(ids.size(), "")),
                new InsertParam.Field("metadata", metadataList != null ? metadataList : Collections.nCopies(ids.size(), "{}"))
            );

            R<MutationResult> response = milvusClient.insert(
                InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build()
            );

            if (response.getStatus() != R.Status.Success.getCode()) {
                return InsertResponse.error(response.getException().getMessage());
            }

            // 刷新数据（确保立即可查询）
            milvusClient.flush(FlushParam.newBuilder()
                .withCollectionNames(Collections.singletonList(collectionName))
                .build()
            );

            return InsertResponse.success(ids.size(), response.getData().getInsertIds());

        } catch (Exception e) {
            log.error("Error inserting vectors: {}", e.getMessage(), e);
            return InsertResponse.error(e.getMessage());
        }
    }

    /**
     * ANN 向量搜索（核心接口）
     *
     * @param collectionName Collection 名称
     * @param request        搜索请求
     * @return 搜索结果
     */
    public SearchResponse search(String collectionName, SearchRequest request) {
        try {
            // 构建 ANN 参数
            List<String> outputFields = Arrays.asList("id", "content", "doc_id", "metadata");

            // 标量过滤表达式（可选）
            String expr = request.getFilterExpr();  // 如: doc_id == 'xxx'

            SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(request.getTopK())
                .withVectors(Collections.singletonList(request.getVector()))
                .withVectorFieldName("embedding")
                .withOutFields(outputFields);

            // 添加过滤条件
            if (expr != null && !expr.isEmpty()) {
                searchBuilder.withExpr(expr);
            }

            // HNSW 搜索参数
            searchBuilder.withParams("{\"ef\": " + Math.max(request.getTopK() * 10, 64) + "}");

            R<SearchResults> response = milvusClient.search(searchBuilder.build());

            if (response.getStatus() != R.Status.Success.getCode()) {
                return SearchResponse.error(response.getException().getMessage());
            }

            // 解析结果
            SearchResultsWrapper resultsWrapper = new SearchResultsWrapper(response.getData().getResults());
            return parseSearchResults(resultsWrapper, request.getTopK());

        } catch (Exception e) {
            log.error("Error searching vectors: {}", e.getMessage(), e);
            return SearchResponse.error(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除向量
     */
    public boolean deleteByIds(String collectionName, List<String> ids) {
        try {
            String expr = "id in [" + ids.stream().map(id -> "'" + id + "'").collect(Collectors.joining(",")) + "]";
            
            R<MutationResult> response = milvusClient.delete(
                DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr)
                    .build()
            );

            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Error deleting vectors: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据 doc_id 删除（删除整个文档的所有片段）
     */
    public boolean deleteByDocId(String collectionName, String docId) {
        try {
            String expr = "doc_id == '" + docId + "'";
            
            R<MutationResult> response = milvusClient.delete(
                DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr)
                    .build()
            );

            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Error deleting by doc_id: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private SearchResponse parseSearchResults(SearchResultsWrapper resultsWrapper, int topK) {
        List<SearchResponse.SearchResult> results = new ArrayList<>();

        for (int i = 0; i < resultsWrapper.getRowCount(); i++) {
            SearchResultsWrapper.IDScore idScore = resultsWrapper.getIDScore(0).get(i);
            
            SearchResponse.SearchResult result = new SearchResponse.SearchResult();
            result.setId(idScore.getLongID() > 0 ? String.valueOf(idScore.getLongID()) : idScore.getStrID());
            result.setScore(idScore.getScore());  // 相似度分数（Cosine: 范围 [-1, 1]）
            
            // 提取字段
            result.setContent((String) idScore.get("content"));
            result.setDocId((String) idScore.get("doc_id"));
            result.setMetadata((String) idScore.get("metadata"));
            
            results.add(result);
        }

        return SearchResponse.success(results);
    }
}
