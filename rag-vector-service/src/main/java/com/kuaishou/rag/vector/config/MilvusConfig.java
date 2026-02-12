package com.kuaishou.rag.vector.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 *
 * TODO: 配置说明
 * 1. 生产环境使用 Milvus Cluster 模式，需要配置多个 server 地址
 * 2. 认证信息从 KMS/配置中心获取，不要硬编码
 * 3. 连接池参数根据实际 QPS 调整
 */
@Slf4j
@Configuration
public class MilvusConfig {

    /**
     * Milvus 服务器地址
     * TODO: 替换为实际地址，格式：host:port
     * 示例：milvus-cluster.internal:19530
     */
    @Value("${milvus.host:milvus-server}")
    private String milvusHost;

    /**
     * Milvus 端口
     * TODO: 默认 19530，如果是 TLS 则使用 443
     */
    @Value("${milvus.port:19530}")
    private Integer milvusPort;

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${milvus.connect-timeout:10000}")
    private Integer connectTimeout;

    /**
     * 客户端保持连接时间（毫秒）
     */
    @Value("${milvus.keepalive-time:55000}")
    private Long keepAliveTime;

    /**
     * 认证 Token（如有）
     * TODO: 从环境变量或 KMS 获取，不要提交到代码仓库
     * 示例：${MILVUS_TOKEN:}
     */
    @Value("${milvus.token:}")
    private String token;

    @Bean
    public MilvusServiceClient milvusClient() {
        log.info("Initializing Milvus client, host: {}, port: {}", milvusHost, milvusPort);

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withConnectTimeout(connectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .withKeepAliveTime(keepAliveTime, java.util.concurrent.TimeUnit.MILLISECONDS);

        // 如果配置了 Token，启用认证
        if (token != null && !token.isEmpty()) {
            builder.withToken(token);
            log.info("Milvus authentication enabled");
        }

        return new MilvusServiceClient(builder.build());
    }
}
