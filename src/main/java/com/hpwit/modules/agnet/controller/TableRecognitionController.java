package com.hpwit.modules.agnet.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.hpwit.common.result.Result;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.TableRecognitionDraft;
import com.hpwit.modules.agnet.service.TableRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 接收表格截图，返回前端显示所需的列定义、单元格和父行关系。
 */
@RestController
@RequestMapping("/v1/agnet/table")
@RequiredArgsConstructor
public class TableRecognitionController {

    private final TableRecognitionService recognitionService;

    @Operation(summary = "从截图识别表格")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TableRecognitionDraft> recognize(@RequestPart(value = "file", required = false) MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        try {
            return Result.success(recognitionService.recognize(userId, file));
        } catch (BusinessException e) {
            return Result.failed(e.getResultCode(), e.getMessage());
        }
    }
}
