package com.kuaishou.rag.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文档服务 - 处理文档上传、解析、分块、向量化全流程
 * 
 * 核心功能：
 * 1. 文档上传与格式验证
 * 2. 内容提取（PDF/DOCX/TXT等）
 * 3. 智能分块
 * 4. 调用 Embedding Service 生成向量
 * 5. 存储到 Milvus
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DocumentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
