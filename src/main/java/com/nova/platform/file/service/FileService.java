package com.nova.platform.file.service;

import com.nova.platform.file.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 对象存储服务接口层
 * @Author huangpw
 * @Date 2026/2/4 9:29
 */
public interface FileService {

    /**
     * 上传文件
     * @param file 表单文件对象
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file);

    /**
     * 删除文件
     *
     * @param filePath 文件完整URL
     * @return 删除结果
     */
    boolean deleteFile(String filePath);

    /**
     * 上传多文件
     * @return 文件信息
     */
    List<FileInfo> uploadFiles(MultipartFile[] files);
}
