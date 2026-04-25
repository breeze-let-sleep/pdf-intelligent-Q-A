package com.hyltest.rag_practice.utils;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss") //将配置信息注入到该类中
public class AliyunOSSProperties {
    private String endpoint;
    private String bucketName;
    private String region;
}
