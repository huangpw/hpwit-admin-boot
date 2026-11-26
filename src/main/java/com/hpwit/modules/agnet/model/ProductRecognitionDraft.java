package com.hpwit.modules.agnet.model;

import java.util.List;

/**
 * AI 识别后供前端表单回填的商品草稿。无法确认的可选文本字段为 {@code null}，
 * 未识别到标签或配件时返回空列表。
 *
 * @param productName 识别出的主体商品名称
 * @param model 从商品或包装上识别出的型号
 * @param description 商品外观及可见特征
 * @param categorySuggestion 供用户核对的分类名称建议
 * @param tags 从图片归纳的商品标签
 * @param accessories 图片中可见的随附配件
 * @param serialNumber 图片中清晰可辨的设备序列号
 */
public record ProductRecognitionDraft(
        String productName,
        String model,
        String description,
        String categorySuggestion,
        List<String> tags,
        List<String> accessories,
        String serialNumber
) {
}
