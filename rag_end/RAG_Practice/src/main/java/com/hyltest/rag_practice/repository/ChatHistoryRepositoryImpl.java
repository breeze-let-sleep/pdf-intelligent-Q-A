package com.hyltest.rag_practice.repository;

import com.hyltest.rag_practice.entity.mapper.FileMapperMapper;
import com.hyltest.rag_practice.entity.mapper.FileTypeMapper;
import com.hyltest.rag_practice.entity.po.FileMapper;
import com.hyltest.rag_practice.entity.po.FileType;
import com.hyltest.rag_practice.entity.vo.ChatHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天历史仓储的 MySQL 实现
 * 负责管理会话 ID 列表的持久化，数据存储在 file_mapper 表中
 * 使用纯 MyBatis Mapper 进行数据库操作，不依赖 MyBatis-Plus
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatHistoryRepositoryImpl implements ChatHistoryRepository {

    /**
     * 文件映射 Mapper
     */
    private final FileMapperMapper fileMapperMapper;

    /**
     * 文件类型 Mapper
     */
    private final FileTypeMapper fileTypeMapper;

    @Override
    public void save(String type, String chatId, String fileName) {
        // 不带 URL 的保存，URL 字段留空
        save(type, chatId, fileName, null);
    }

    @Override
    public void save(String type, String chatId, String fileName, String url) {
        // 查询是否已存在该 chatId 的记录
        FileMapper existing = fileMapperMapper.selectByChatId(chatId);

        // 获取文件类型 ID（确保指定类型存在）
        Integer typeId = getOrCreateFileType(type);

        if (existing != null) {
            // 更新已有记录
            existing.setType(typeId);
            existing.setTitle(fileName);
            existing.setFileName(fileName);
            if (url != null && !url.isEmpty()) {
                existing.setUrl(url);
            }
            fileMapperMapper.updateById(existing);
            log.debug("更新会话历史: type={}, chatId={}, fileName={}, url={}", type, chatId, fileName, url);
        } else {
            // 插入新记录
            FileMapper newMapper = new FileMapper();
            newMapper.setType(typeId);
            newMapper.setChatId(chatId);
            newMapper.setTitle(fileName != null ? fileName : chatId);
            newMapper.setFileName(fileName);
            newMapper.setUrl(url);
            fileMapperMapper.insert(newMapper);
            log.debug("新增会话历史: type={}, chatId={}, fileName={}, url={}", type, chatId, fileName, url);
        }
    }

    @Override
    public List<ChatHistoryVO> getChatIds(String type) {
        // 1. 根据类型查找对应的 fileType 记录
        FileType fileType = fileTypeMapper.selectByType(type);

        if (fileType == null) {
            log.debug("未找到类型为 {} 的文件类型记录", type);
            return List.of();
        }

        // 2. 根据 fileType.id 查询所有对应的文件映射记录（按主键倒序，最新的在前）
        List<FileMapper> mappers = fileMapperMapper.selectByType(fileType.getId());

        // 3. 转换为 VO 对象
        return mappers.stream()
                .map(mapper -> new ChatHistoryVO(
                        mapper.getChatId(),
                        mapper.getTitle() != null ? mapper.getTitle() : mapper.getFileName(),
                        mapper.getFileName()
                ))
                .toList();
    }

    /**
     * 获取或创建文件类型
     * 确保指定类型的记录存在于 file_type 表中
     *
     * @param type 文件类型标识
     * @return 文件类型 ID
     */
    private Integer getOrCreateFileType(String type) {
        FileType fileType = fileTypeMapper.selectByType(type);

        if (fileType != null) {
            return fileType.getId();
        }

        // 类型不存在，创建新记录
        FileType newType = new FileType();
        newType.setType(type);
        fileTypeMapper.insert(newType);
        log.info("新建文件类型: type={}", type);

        // insert 时 @Options 已将自增主键回填到 newType.id，直接返回即可
        return newType.getId();
    }
}
