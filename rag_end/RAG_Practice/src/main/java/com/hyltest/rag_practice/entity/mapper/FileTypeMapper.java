package com.hyltest.rag_practice.entity.mapper;

import com.hyltest.rag_practice.entity.po.FileType;
import org.apache.ibatis.annotations.*;

/**
 * 文件类型 Mapper 接口
 * 使用纯 MyBatis 注解方式定义 SQL，替代 MyBatis-Plus 的 BaseMapper
 */
@Mapper
public interface FileTypeMapper {

    /**
     * 根据 ID 查询文件类型
     *
     * @param id 主键 ID
     * @return 文件类型实体
     */
    @Select("SELECT id, `type` FROM file_type WHERE id = #{id}")
    FileType selectById(@Param("id") Integer id);

    /**
     * 根据类型标识查询文件类型记录
     *
     * @param type 类型标识（如 "pdf"）
     * @return 文件类型实体
     */
    @Select("SELECT id, `type` FROM file_type WHERE `type` = #{type}")
    FileType selectByType(@Param("type") String type);

    /**
     * 插入新的文件类型记录
     * 使用 @Options 获取数据库自增主键并回填到实体
     *
     * @param fileType 文件类型实体
     * @return 影响行数
     */
    @Insert("INSERT INTO file_type(`type`) VALUES(#{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileType fileType);

    /**
     * 根据 ID 删除文件类型记录
     *
     * @param id 主键 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM file_type WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
