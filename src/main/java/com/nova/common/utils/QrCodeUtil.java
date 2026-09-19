package com.nova.common.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码工具类
 * @Author huangpw
 * @Date 2026/2/25 18:25
 */
public class QrCodeUtil {

    /**
     * 生成二维码并保存到本地
     *
     * @param content   二维码内容（编号 / URL / JSON 等）
     * @param width     宽
     * @param height    高
     * @param savePath  本地保存路径（包含文件名）
     * @return          返回保存路径
     */
    public static String generateQrCode(String content, int width, int height, String savePath) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            // 设置字符编码为UTF-8，支持中文内容
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            // 设置二维码边距，值为1表示留白区域较小
            hints.put(EncodeHintType.MARGIN, 1);

            // 使用MultiFormatWriter将内容编码为二维码矩阵
            // 参数说明：内容、二维码类型、宽度、高度、编码参数
            BitMatrix bitMatrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 将字符串路径转换为Path对象
            Path path = FileSystems.getDefault().getPath(savePath);
            // 将二维码矩阵写入文件，格式为PNG
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            // 返回保存路径
            return savePath;
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}
