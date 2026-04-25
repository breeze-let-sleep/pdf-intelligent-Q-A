package com.hyltest.rag_practice.service;

import com.hyltest.rag_practice.entity.po.FileMapper;
import org.springframework.core.io.Resource;

public interface IFileService {
    /**
     * 保存文件,还要记录chatId与文件的映射关系
     * @param chatId 会话id
     * @param resource 文件
     * @return 上传成功，返回true； 否则返回false
     */
    boolean save(String chatId, Resource resource);

    /**
     * 根据chatId获取文件
     * @param chatId 会话id
     * @return 找到的文件
     */
    Resource getFile(String chatId);

    /**
     * 根据 chatId 获取文件URL
     * @param chatId 会话id
     * @return 文件URL（OSS路径）
     */
    String getFileUrl(String chatId);

    /**
     * 根据 chatId 获取会话标题
     * @param chatId 会话id
     * @return 会话标题
     */
    String getTitle(String chatId);

    /**
     * 根据 chatId 获取文件名
     * @param chatId 会话id
     * @return 文件名
     */
    String getFileName(String chatId);

    /**
     * 根据 chatId 更新文件信息
     * @param chatId 会话id
     * @param title 新标题
     * @param fileName 新文件名
     * @param url 新文件URL
     */
    void updateFileInfo(String chatId, String title, String fileName, String url);

    /**
     * 获取文件映射信息
     * @param chatId 会话id
     * @return 文件映射实体
     */
    FileMapper getFileMapper(String chatId);
}
