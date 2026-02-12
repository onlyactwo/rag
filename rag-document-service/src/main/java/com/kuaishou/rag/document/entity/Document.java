package com.kuaishou.rag.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
 * 
 * TODO: 数据库配置
 * 1. 创建表 SQL 见 database-design.md
 * 2. 配置 MyBatis-Plus 数据源
 * 3. 考虑分库分表策略（按 user_id 或 tenant_id）
 */
@Data
@TableName("documents")
public class Document {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 文档唯一标识
     */
    private String docId;
    
    /**
     * 文档名称
     */
    private String docName;
    
    /**
     * 文档类型：pdf/doc/docx/txt/md
     */
    private String docType;
    
    /**
     * 存储路径
     * TODO: 对接对象存储（S3/MinIO/KS3）
     * 格式：s3://bucket/path/to/file.pdf
     */
    private String storagePath;
    
    /**
     * 文档大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文档状态
     * pending/processing/completed/failed
     */
    private String status;
    
    /**
     * 所属用户/租户
     * TODO: 多租户隔离
     */
    private Long userId;
    private String tenantId;
    
    /**
     * 处理错误信息
     */
    private String errorMsg;
    
    /**
     * 元数据 JSON
     * TODO: 使用 JSON 类型字段
     */
    private String metadata;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
