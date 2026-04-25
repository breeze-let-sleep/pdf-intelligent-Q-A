package com.hyltest.rag_practice.entity.mapper;

import com.hyltest.rag_practice.entity.po.FileMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文件映射 Mapper 接口
 * 使用纯 MyBatis 注解方式定义 SQL，替代 MyBatis-Plus 的 BaseMapper
 */
@Mapper
public interface FileMapperMapper {

    /**
     * 根据 ID 查询文件映射记录
     *
     * @param id 主键 ID
     * @return 文件映射实体
     */
    @Select("SELECT id, `type`, chat_id, title, file_name, `url` FROM file_mapper WHERE id = #{id}")
    FileMapper selectById(@Param("id") Integer id);

    /**
     * 根据会话 ID 查询文件映射记录
     *
     * @param chatId 会话 ID
     * @return 文件映射实体
     */
    @Select("SELECT id, `type`, chat_id, title, file_name, `url` FROM file_mapper WHERE chat_id = #{chatId}")
    FileMapper selectByChatId(@Param("chatId") String chatId);

    /**
     * 根据文件类型 ID 查询所有文件映射记录，按主键倒序排列
     *
     * @param type 文件类型 ID
     * @return 文件映射列表
     */
    @Select("SELECT id, `type`, chat_id, title, file_name, `url` FROM file_mapper WHERE `type` = #{type} ORDER BY id DESC")
    List<FileMapper> selectByType(@Param("type") Integer type);

    /**
     * 插入新的文件映射记录
     * 使用 @Options 获取数据库自增主键并回填到实体
     *
     * @param fileMapper 文件映射实体
     * @return 影响行数
     */
    @Insert("INSERT INTO file_mapper(`type`, chat_id, title, file_name, `url`) " +
            "VALUES(#{type}, #{chatId}, #{title}, #{fileName}, #{url})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileMapper fileMapper);

    /**
     * 根据 ID 更新文件映射记录
     *
     * @param fileMapper 文件映射实体（需包含 id）
     * @return 影响行数
     */
    @Update("UPDATE file_mapper SET `type` = #{type}, chat_id = #{chatId}, " +
            "title = #{title}, file_name = #{fileName}, `url` = #{url} WHERE id = #{id}")
    int updateById(FileMapper fileMapper);

    /**
     * 根据 ID 删除文件映射记录
     *
     * @param id 主键 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM file_mapper WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
