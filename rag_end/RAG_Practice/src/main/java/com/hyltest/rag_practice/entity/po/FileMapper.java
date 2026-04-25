package com.hyltest.rag_practice.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件映射实体类
 * 对应数据库表 file_mapper
 * 用于记录文件与会话的映射关系
 *
 * 注意：url 和 type 是 MySQL 保留字，SQL 中需要用反引号转义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMapper {

    /**
     * 主键 ID
     * 使用数据库自增策略
     */
    private Integer id;

    /**
     * 文件类型 ID
     * 外键关联 file_type 表的 id 字段
     * 注意：type 是 MySQL 保留字，建表和查询时需要反引号转义
     */
    private Integer type;

    /**
     * 会话 ID
     * 用于标识一次会话对话
     */
    private String chatId;

    /**
     * 会话标题
     * 通常使用文件名作为标题
     */
    private String title;

    /**
     * 文件名
     * 原始上传的文件名
     */
    private String fileName;

    /**
     * 文件访问路径（OSS URL）
     * 存储在阿里云 OSS 中的文件访问地址
     * 注意：url 是 MySQL 保留字，建表和查询时需要反引号转义
     */
    private String url;
}
