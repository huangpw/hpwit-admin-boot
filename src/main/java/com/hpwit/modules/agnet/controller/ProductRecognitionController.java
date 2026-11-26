package com.hpwit.modules.agnet.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.hpwit.common.result.Result;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.ProductRecognitionDraft;
import com.hpwit.modules.agnet.service.ProductRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 接收 PC 端上传的商品图片，返回供表单回填的识别草稿。
 */
@Tag(name = "AI 识图录入")
@RestController
@RequestMapping("/v1/agnet/product")
@RequiredArgsConstructor
public class ProductRecognitionController {

    private final ProductRecognitionService recognitionService;

    /**
     * 从登录会话获取用户 ID，用于按用户限流。业务错误通过统一响应返回给前端。
     *
     * @param file multipart 表单中的商品图片。缺少文件时由服务层返回统一的校验提示
     * @return 识别草稿或业务错误
     */
    @Operation(summary = "从商品图片识别表单草稿")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ProductRecognitionDraft> recognize(@RequestPart(value = "file", required = false) MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        try {
            return Result.success(recognitionService.recognize(userId, file));
        } catch (BusinessException e) {
            return Result.failed(e.getResultCode(), e.getMessage());
        }
    }

    /** 从同一商品的多张照片生成一份草稿。表单中的每张照片使用 files 字段。 */
    @Operation(summary = "联合识别同一商品的多张图片")
    @PostMapping(value = "/recognize-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ProductRecognitionDraft> recognizeMultiple(
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        long userId = StpUtil.getLoginIdAsLong();
        try {
            return Result.success(recognitionService.recognizeMultiple(userId, files));
        } catch (BusinessException e) {
            return Result.failed(e.getResultCode(), e.getMessage());
        }
    }
}
