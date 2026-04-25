package com.hyltest.rag_practice.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件类型实体类
 * 对应数据库表 file_type
 * 用于区分不同类型的文件（如 PDF、Word 等）
 *
 * 注意：type 是 MySQL 保留字，SQL 中需要用反引号转义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileType {

    /**
     * 主键 ID
     * 使用数据库自增策略
     */
    private Integer id;

    /**
     * 文件类型标识
     * 如 "pdf"
     * 注意：type 是 MySQL 保留字，建表和查询时需要反引号转义
     */
    private String type;
}
