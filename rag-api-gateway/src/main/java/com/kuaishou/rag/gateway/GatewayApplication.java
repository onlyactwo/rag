package com.kuaishou.rag.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API网关启动类
 * 
 * TODO: 配置说明
 * 1. application.yml 中配置路由规则
 * 2. 配置Consul/Eureka服务发现
 * 3. 配置限流策略（Redis/本地）
 * 4. 配置认证过滤器
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
