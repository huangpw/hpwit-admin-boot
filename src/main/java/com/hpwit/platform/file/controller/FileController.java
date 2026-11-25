package com.hpwit.platform.file.controller;

import com.hpwit.common.result.Result;
import com.hpwit.platform.file.model.FileInfo;
import com.hpwit.platform.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件控制层
 * @Author huangpw
 * @Date 2026/2/4 9:28
 */
@Tag(name = "07.文件接口")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    @Operation(summary = "文件上传")
    public Result<FileInfo> uploadFile(
            @Parameter(name = "file", description = "表单文件对象", required = true, in = ParameterIn.DEFAULT, schema = @Schema(name = "file", format = "binary"))
            @RequestPart(value = "file") MultipartFile file
    ) {
        FileInfo fileInfo = fileService.uploadFile(file);
        return Result.success(fileInfo);
    }

    @PostMapping("/multiple")
    @Operation(summary = "多文件上传")
    public Result<List<FileInfo>> uploadFiles(@RequestPart(value = "files") MultipartFile[] files) {
        List<FileInfo> list = fileService.uploadFiles(files);
        return Result.success(list);
    }

    @DeleteMapping
    @Operation(summary = "文件删除")
    @SneakyThrows
    public Result<?> deleteFile(@Parameter(description = "文件路径") @RequestParam String filePath) {
        boolean result = fileService.deleteFile(filePath);
        return Result.judge(result);
    }
}
