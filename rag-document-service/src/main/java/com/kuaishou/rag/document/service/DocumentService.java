package com.kuaishou.rag.document.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuaishou.rag.common.result.Result;
import com.kuaishou.rag.document.entity.Document;
import com.kuaishou.rag.document.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {

    // TODO: 注入 MinIO 客户端，用于文件存储
    // private final MinioClient minioClient;
    
    // TODO: 注入 Kafka/RabbitMQ 生产者，用于发送文档解析任务
    // private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 上传文档
     *
     * @param file 文件
     * @param kbId 知识库ID
     * @return 文档信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Document> uploadDocument(MultipartFile file, String kbId) {
        try {
            // 1. 生成文档ID
            String docId = UUID.randomUUID().toString().replace("-", "");
            
            // 2. 保存文件到对象存储
            // TODO: 配置 MinIO 连接信息后启用
            // String objectName = "docs/" + kbId + "/" + docId + "/" + file.getOriginalFilename();
            // minioClient.putObject(PutObjectArgs.builder()
            //     .bucket("rag-documents")
            //     .object(objectName)
            //     .stream(file.getInputStream(), file.getSize(), -1)
            //     .contentType(file.getContentType())
            //     .build());
            
            // 3. 保存文档元数据到数据库
            Document document = new Document();
            document.setId(docId);
            document.setKbId(kbId);
            document.setName(file.getOriginalFilename());
            document.setType(getFileExtension(file.getOriginalFilename()));
            document.setSize(file.getSize());
            document.setStatus(0); // 待处理
            document.setStoragePath("minio://rag-documents/" + docId); // TODO: 实际路径
            document.setCreateTime(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
            
            save(document);
            
            // 4. 发送异步处理消息到队列
            // TODO: 配置 Kafka 后启用
            // kafkaTemplate.send("document-process-topic", docId);
            
            log.info("文档上传成功: docId={}, name={}", docId, file.getOriginalFilename());
            return Result.success(document);
            
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库下的文档列表
     */
    public Result<List<Document>> listByKbId(String kbId) {
        List<Document> documents = lambdaQuery()
            .eq(Document::getKbId, kbId)
            .orderByDesc(Document::getCreateTime)
            .list();
        return Result.success(documents);
    }

    /**
     * 删除文档
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteDocument(String docId) {
        Document document = getById(docId);
        if (document == null) {
            return Result.error("文档不存在");
        }
        
        // TODO: 删除 MinIO 中的文件
        // minioClient.removeObject(RemoveObjectArgs.builder()
        //     .bucket("rag-documents")
        //     .object(document.getStoragePath())
        //     .build());
        
        // TODO: 调用 Vector Service 删除向量
        // vectorService.deleteByDocId(docId);
        
        removeById(docId);
        log.info("文档删除成功: docId={}", docId);
        return Result.success();
    }

    /**
     * 更新文档状态
     */
    public Result<Void> updateStatus(String docId, Integer status) {
        lambdaUpdate()
            .set(Document::getStatus, status)
            .set(Document::getUpdateTime, LocalDateTime.now())
            .eq(Document::getId, docId)
            .update();
        return Result.success();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
