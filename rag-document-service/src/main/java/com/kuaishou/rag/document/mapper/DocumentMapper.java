package com.kuaishou.rag.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuaishou.rag.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档数据访问层 - MyBatis Plus
 * 
 * TODO: 数据库配置说明
 * 1. application.yml 中配置 MySQL/PostgreSQL 连接
 * 2. 如需分库分表，集成 ShardingSphere
 * 3. 大文本字段 content 建议存储到对象存储，表中只存 URL
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 根据知识库ID查询文档列表
     */
    @Select("SELECT * FROM document WHERE kb_id = #{kbId} AND deleted = 0 ORDER BY create_time DESC")
    List<Document> selectByKbId(String kbId);

    /**
     * 根据状态查询待处理的文档
     */
    @Select("SELECT * FROM document WHERE status = #{status} AND deleted = 0 LIMIT #{limit}")
    List<Document> selectByStatusWithLimit(String status, Integer limit);
}
